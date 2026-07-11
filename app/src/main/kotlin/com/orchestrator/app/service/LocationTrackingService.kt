package com.orchestrator.app.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.orchestrator.app.MainActivity
import com.orchestrator.app.R
import com.orchestrator.app.data.db.TelemetryDao
import com.orchestrator.app.data.db.TelemetrySample
import com.orchestrator.app.data.store.TelemetryPrefs
import com.orchestrator.app.util.hasBackgroundLocationPermission
import com.orchestrator.app.util.isoFromEpochMillis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that is the sole GPS source: samples via
 * [LocationManager.requestLocationUpdates] on GPS_PROVIDER every ~5 min and buffers each fix
 * as a location-only [TelemetrySample] into Room. The flush worker uploads the buffer.
 *
 * The "Location tracking" notification channel is created in `OrchestratorApp.onCreate`, so it
 * exists before [startForeground]. START_STICKY covers process-death-while-alive; reboot
 * recovery is driven by `MainActivity.onStart` (a location FGS cannot start from BOOT_COMPLETED).
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var dao: TelemetryDao

    @Inject
    lateinit var telemetryPrefs: TelemetryPrefs

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val locationManager: LocationManager? by lazy {
        getSystemService(LOCATION_SERVICE) as? LocationManager
    }

    private val listener = LocationListener { location -> onFix(location) }

    private var updatesRequested = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()

        // Stop if opt-in was turned off or the background-location grant went away.
        if (!telemetryPrefs.isEnabled() || !hasBackgroundLocationPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        requestUpdates()
        return START_STICKY
    }

    private fun requestUpdates() {
        if (updatesRequested) return
        val lm = locationManager ?: return
        try {
            lm.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                UPDATE_INTERVAL_MS,
                0f,
                listener
            )
            updatesRequested = true
        } catch (_: SecurityException) {
            stopSelf()
        } catch (_: IllegalArgumentException) {
            // GPS provider absent — nothing to sample.
            stopSelf()
        }
    }

    private fun onFix(location: Location) {
        val sample = TelemetrySample(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyM = if (location.hasAccuracy()) location.accuracy.toDouble() else null,
            fixTimestamp = isoFromEpochMillis(location.time)
        )
        scope.launch { dao.insert(sample) }
    }

    private fun buildNotification(): Notification {
        val tapIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            this,
            0,
            tapIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Location tracking")
            .setContentText("Recording your location trail.")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        if (updatesRequested) {
            try {
                locationManager?.removeUpdates(listener)
            } catch (_: SecurityException) {
            }
            updatesRequested = false
        }
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val CHANNEL_ID = "location_tracking"
        const val CHANNEL_NAME = "Location tracking"
        private const val NOTIFICATION_ID = 42
        private const val UPDATE_INTERVAL_MS = 5 * 60_000L
    }
}
