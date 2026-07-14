package com.orchestrator.app.data.device

/**
 * Cumulative-since-boot step reader. Seam over [StepProvider] so [DailyStepTracker] can be
 * unit-tested with a scripted fake instead of Android's SensorManager.
 */
interface StepReader {
    suspend fun readCumulative(): Int?
}
