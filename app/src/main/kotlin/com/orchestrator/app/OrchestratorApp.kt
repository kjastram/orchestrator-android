package com.orchestrator.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.orchestrator.app.data.store.TelemetryPrefs
import com.orchestrator.app.notification.TaskAlarmReceiver
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

        // Re-enqueue on start only if opted in AND perms still present. The worker
        // self-cancels otherwise, but this keeps a stale flag from silently no-op'ing.
        if (telemetryPrefs.isEnabled() && hasBackgroundLocationPermission()) {
            TelemetryScheduler.enqueue(this)
        }
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        val foreground = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return foreground && background
    }
}
