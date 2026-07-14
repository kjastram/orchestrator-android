package com.orchestrator.app.data.device

import android.util.Log
import com.orchestrator.app.data.model.StepsData
import com.orchestrator.app.data.repository.StepUploader
import com.orchestrator.app.data.store.StepPrefs
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns the reboot-relative, cumulative-since-boot step counter into a device-local daily total.
 *
 * The hardware [StepProvider.readCumulative] returns steps-since-boot, which resets to ~0 on
 * reboot. We persist the last raw reading and accumulate deltas across ticks; a drop (sensor <
 * last) is read as a reboot and the new reading is taken as the delta from zero. State rolls over
 * at the device-local calendar day, flushing the finished day to the backend before resetting.
 */
@Singleton
class DailyStepTracker @Inject constructor(
    private val stepReader: StepReader,
    private val prefs: StepPrefs,
    private val uploader: StepUploader
) {

    /**
     * One accumulator tick. Returns the current day's accumulated total, or null if the sensor
     * couldn't be read (a no-op tick that mutates nothing). [today] is the device-local date and
     * is injectable so the accumulator logic is testable without a wall clock.
     */
    suspend fun update(
        today: String = LocalDate.now(ZoneId.systemDefault()).toString()
    ): Int? {
        val sensor = stepReader.readCumulative() ?: return prefs.stepAccumulated

        if (today != prefs.stepDay) {
            // Flush the finished day before resetting — best-effort, never blocks the reset.
            if (prefs.stepDay.isNotEmpty() && prefs.stepAccumulated > 0) {
                try {
                    uploader.sendSteps(
                        StepsData(localDate = prefs.stepDay, stepsToday = prefs.stepAccumulated)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Rollover flush failed for ${prefs.stepDay}", e)
                }
            }
            prefs.stepAccumulated = 0
            prefs.stepDay = today
            prefs.stepLastSensor = sensor
            return 0
        }

        if (prefs.stepLastSensor < 0) {
            // First reading of the day — establish a baseline, add nothing.
            prefs.stepLastSensor = sensor
        } else {
            val last = prefs.stepLastSensor
            val delta = if (sensor >= last) sensor - last else sensor
            prefs.stepAccumulated += delta
            prefs.stepLastSensor = sensor
        }
        return prefs.stepAccumulated
    }

    companion object {
        private const val TAG = "DailyStepTracker"
    }
}
