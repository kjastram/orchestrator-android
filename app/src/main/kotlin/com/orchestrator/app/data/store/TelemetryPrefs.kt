package com.orchestrator.app.data.store

import android.app.Application
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/** Plain (non-encrypted) prefs for the telemetry opt-in flag. */
@Singleton
class TelemetryPrefs @Inject constructor(context: Application) : StepPrefs {

    private val prefs = context.getSharedPreferences("orchestrator_telemetry_prefs", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    override var stepDay: String
        get() = prefs.getString(KEY_STEP_DAY, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_STEP_DAY, value).apply()
        }

    override var stepAccumulated: Int
        get() = prefs.getInt(KEY_STEP_ACCUMULATED, 0)
        set(value) {
            prefs.edit().putInt(KEY_STEP_ACCUMULATED, value).apply()
        }

    override var stepLastSensor: Int
        get() = prefs.getInt(KEY_STEP_LAST_SENSOR, -1)
        set(value) {
            prefs.edit().putInt(KEY_STEP_LAST_SENSOR, value).apply()
        }

    override var stepLastUploadHour: String
        get() = prefs.getString(KEY_STEP_LAST_UPLOAD_HOUR, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_STEP_LAST_UPLOAD_HOUR, value).apply()
        }

    companion object {
        private const val KEY_ENABLED = "telemetry_enabled"
        private const val KEY_STEP_DAY = "step_day"
        private const val KEY_STEP_ACCUMULATED = "step_accumulated"
        private const val KEY_STEP_LAST_SENSOR = "step_last_sensor"
        private const val KEY_STEP_LAST_UPLOAD_HOUR = "step_last_upload_hour"
    }
}
