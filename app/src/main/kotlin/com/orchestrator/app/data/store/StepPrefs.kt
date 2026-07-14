package com.orchestrator.app.data.store

/**
 * Persisted daily-step accumulator state. Extracted from [TelemetryPrefs] so [com.orchestrator
 * .app.data.device.DailyStepTracker] can be unit-tested on the JVM with a map-backed fake.
 */
interface StepPrefs {
    var stepDay: String
    var stepAccumulated: Int
    var stepLastSensor: Int
    var stepLastUploadHour: String
}
