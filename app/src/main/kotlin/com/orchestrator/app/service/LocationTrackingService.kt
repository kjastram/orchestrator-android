package com.orchestrator.app.service

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.orchestrator.app.MainActivity
import com.orchestrator.app.R
import com.orchestrator.app.data.db.TelemetryDao
import com.orchestrator.app.data.db.TelemetrySample
import com.orchestrator.app.data.device.LocationProvider
import com.orchestrator.app.data.store.TelemetryPrefs
import com.orchestrator.app.util.hasBackgroundLocationPermission
import com.orchestrator.app.util.isoFromEpochMillis
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Foreground service that is the sole GPS source: every ~5 min it asks [LocationProvider] for a
 * fix (GPS -> NETWORK -> last-known fallback chain) and buffers it as a location-only
 * [TelemetrySample] into Room. The flush worker uploads the buffer.
 *
 * We poll [LocationProvider.getFix] rather than register continuous GPS updates because a
 * GPS_PROVIDER-only continuous listener yields nothing when the phone is indoors/stationary —
 * the fallback chain guarantees a point (at worst a repeated last-known) each cycle.
 *
 * The "Location tracking" notification channel is created in `OrchestratorApp.onCreate`, so it
 * exists before [startForeground]. START_STICKY covers process-death-while-alive; reboot
 * recovery is driven by `MainActivity.onStart` (a location FGS cannot start from BOOT_COMPLETED).
 */
@AndroidEntryPoint
class LocationTrackingService : Service() {

    @Inject
    lateinit var locationProvider: LocationProvider

    @Inject
    lateinit var dao: TelemetryDao

    @Inject
    lateinit var telemetryPrefs: TelemetryPrefs

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var sampling = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundCompat()

        // Stop if opt-in was turned off or the background-location grant went away.
        if (!telemetryPrefs.isEnabled() || !hasBackgroundLocationPermission(this)) {
            stopSelf()
            return START_NOT_STICKY
        }

        startSampling()
        return START_STICKY
    }

    private fun startSampling() {
        if (sampling) return
        sampling = true
        scope.launch {
            while (isActive) {
                val fix = locationProvider.getFix()
                if (fix != null) {
                    dao.insert(
                        TelemetrySample(
                            latitude = fix.latitude,
                            longitude = fix.longitude,
                            accuracyM = if (fix.hasAccuracy()) fix.accuracy.toDouble() else null,
                            fixTimestamp = isoFromEpochMillis(fix.time)
                        )
                    )
                }
                delay(UPDATE_INTERVAL_MS)
            }
        }
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
        sampling = false
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
