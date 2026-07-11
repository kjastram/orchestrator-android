package com.orchestrator.app.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.orchestrator.app.data.db.TelemetryDao
import com.orchestrator.app.data.db.TelemetrySample
import com.orchestrator.app.data.device.BatteryProvider
import com.orchestrator.app.data.device.StepProvider
import com.orchestrator.app.data.model.DeviceTelemetryData
import com.orchestrator.app.data.repository.BatchResult
import com.orchestrator.app.data.repository.TelemetryRepository
import com.orchestrator.app.data.store.TelemetryPrefs
import com.orchestrator.app.data.store.TokenStore
import com.orchestrator.app.util.hasBackgroundLocationPermission
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Flush worker (15-min): reads battery deep-stats + step count into one buffer row, then
 * batch-uploads the whole Room buffer and deletes uploaded rows by id. The GPS trail is owned
 * by [com.orchestrator.app.service.LocationTrackingService] — no GPS fix is taken here.
 */
@HiltWorker
class TelemetryWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val dao: TelemetryDao,
    private val batteryProvider: BatteryProvider,
    private val stepProvider: StepProvider,
    private val telemetryRepository: TelemetryRepository,
    private val tokenStore: TokenStore,
    private val telemetryPrefs: TelemetryPrefs
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Not logged in — nothing to upload, don't retry.
        if (tokenStore.getToken() == null) {
            return Result.success()
        }

        // Self-cancel if the user disabled telemetry or the background-location grant went away.
        if (!telemetryPrefs.isEnabled() || !hasBackgroundLocationPermission(applicationContext)) {
            TelemetryScheduler.cancel(applicationContext)
            return Result.success()
        }

        // Battery/steps are sampled at flush time (they survive reboot via WorkManager even
        // while GPS is paused). Insert one battery/steps row into the buffer.
        val battery = batteryProvider.read()
        val steps = stepProvider.readCumulative()
        dao.insert(
            TelemetrySample(
                batteryPercent = battery.percent,
                isCharging = battery.isCharging,
                batteryTempC = battery.tempC,
                batteryHealth = battery.health,
                stepCount = steps,
                batteryCurrentUa = battery.currentUa,
                batteryVoltageMv = battery.voltageMv,
                chargeSource = battery.chargeSource
            )
        )

        val rows = dao.getAll()
        if (rows.isEmpty()) {
            return Result.success()
        }

        val ids = rows.map { it.id }
        val batch = rows.map { it.toDto() }

        return when (val result = telemetryRepository.sendBatch(batch)) {
            is BatchResult.Sent -> {
                dao.deleteByIds(ids)
                Result.success()
            }
            is BatchResult.Retryable -> Result.retry()
            is BatchResult.Unauthorized -> Result.success()
            is BatchResult.Poison -> {
                Log.w(TAG, "Dropping poison batch (${rows.size} rows), HTTP ${result.code}")
                dao.deleteByIds(ids)
                Result.success()
            }
        }
    }

    private fun TelemetrySample.toDto() = DeviceTelemetryData(
        batteryPercent = batteryPercent,
        isCharging = isCharging,
        batteryTempC = batteryTempC,
        batteryHealth = batteryHealth,
        latitude = latitude,
        longitude = longitude,
        accuracyM = accuracyM,
        fixTimestamp = fixTimestamp,
        stepCount = stepCount,
        batteryCurrentUa = batteryCurrentUa,
        batteryVoltageMv = batteryVoltageMv,
        chargeSource = chargeSource
    )

    companion object {
        private const val TAG = "TelemetryWorker"
    }
}
