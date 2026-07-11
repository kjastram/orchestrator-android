package com.orchestrator.app.data.store

import android.app.Application
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/** Plain (non-encrypted) prefs for the telemetry opt-in flag. */
@Singleton
class TelemetryPrefs @Inject constructor(context: Application) {

    private val prefs = context.getSharedPreferences("orchestrator_telemetry_prefs", Context.MODE_PRIVATE)

    fun isEnabled(): Boolean = prefs.getBoolean(KEY_ENABLED, false)

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    companion object {
        private const val KEY_ENABLED = "telemetry_enabled"
    }
}
