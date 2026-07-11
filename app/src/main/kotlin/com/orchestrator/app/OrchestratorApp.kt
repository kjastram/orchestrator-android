package com.orchestrator.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.orchestrator.app.data.store.TelemetryPrefs
import com.orchestrator.app.notification.TaskAlarmReceiver
import com.orchestrator.app.service.LocationTrackingService
import com.orchestrator.app.util.hasBackgroundLocationPermission
import com.orchestrator.app.worker.TelemetryScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class OrchestratorApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var telemetryPrefs: TelemetryPrefs

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        TaskAlarmReceiver.createNotificationChannel(this)
        createLocationChannel()

        // Re-enqueue on start only if opted in AND perms still present. The worker
        // self-cancels otherwise, but this keeps a stale flag from silently no-op'ing.
        // NOTE: the location FGS is NOT started here — a location FGS needs a visible
        // context, so it's started from MainActivity.onStart (reboot recovery).
        if (telemetryPrefs.isEnabled() && hasBackgroundLocationPermission(this)) {
            TelemetryScheduler.enqueue(this)
        }
    }

    /** Low-importance channel for the location foreground service. Created before any
     *  startForeground so the channel always exists first. */
    private fun createLocationChannel() {
        val channel = NotificationChannel(
            LocationTrackingService.CHANNEL_ID,
            LocationTrackingService.CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Ongoing location trail recording"
        }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }
}
