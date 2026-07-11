package com.orchestrator.app.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orchestrator.app.data.device.BatteryProvider
import com.orchestrator.app.data.device.LocationProvider
import com.orchestrator.app.data.model.DeviceTelemetryData
import com.orchestrator.app.data.repository.TelemetryRepository
import com.orchestrator.app.data.store.TelemetryPrefs
import com.orchestrator.app.data.store.TokenStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@HiltWorker
class TelemetryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val locationProvider: LocationProvider,
    private val batteryProvider: BatteryProvider,
    private val telemetryRepository: TelemetryRepository,
    private val tokenStore: TokenStore,
    private val telemetryPrefs: TelemetryPrefs
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Not logged in — nothing to upload, don't retry.
        if (tokenStore.getToken() == null) {
            return Result.success()
        }

        // Self-cancel if the user disabled telemetry or the background-location
        // grant went away (revocation can restart the process).
        if (!telemetryPrefs.isEnabled() || !hasBackgroundLocationPermission()) {
            TelemetryScheduler.cancel(applicationContext)
            return Result.success()
        }

        val battery = batteryProvider.read()
        val location = locationProvider.getFix() // nullable — partial battery-only row is fine

        val payload = DeviceTelemetryData(
            batteryPercent = battery.percent,
            isCharging = battery.isCharging,
            batteryTempC = battery.tempC,
            batteryHealth = battery.health,
            latitude = location?.latitude,
            longitude = location?.longitude,
            accuracyM = location?.let { if (it.hasAccuracy()) it.accuracy.toDouble() else null },
            fixTimestamp = location?.let { isoFromEpochMillis(it.time) }
        )

        return telemetryRepository.send(payload).fold(
            onSuccess = { Result.success() },
            onFailure = { Result.retry() } // network/HTTP failure — retry; missing fix already handled above
        )
    }

    private fun hasBackgroundLocationPermission(): Boolean {
        val foreground = ContextCompat.checkSelfPermission(
            applicationContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        return foreground && background
    }

    private fun isoFromEpochMillis(millis: Long): String =
        DateTimeFormatter.ISO_OFFSET_DATE_TIME
            .format(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC))
}
