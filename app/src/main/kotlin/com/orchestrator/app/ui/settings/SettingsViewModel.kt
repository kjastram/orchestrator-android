package com.orchestrator.app.ui.settings

import android.Manifest
import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.orchestrator.app.data.store.TelemetryPrefs
import com.orchestrator.app.service.LocationTrackingService
import com.orchestrator.app.util.hasBackgroundLocationPermission
import com.orchestrator.app.worker.TelemetryScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SettingsUiState(
    val telemetryEnabled: Boolean = false,
    // True once the two-step location grant flow is fully satisfied and the trail is running.
    val telemetryActive: Boolean = false,
    // Set when foreground granted but background ("Allow all the time") still needed.
    val awaitingBackgroundGrant: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val application: Application,
    private val telemetryPrefs: TelemetryPrefs
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            telemetryEnabled = telemetryPrefs.isEnabled(),
            telemetryActive = telemetryPrefs.isEnabled() && hasBackgroundLocationPermission(application)
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /** User flipped the switch on: persist intent and let the screen drive the permission steps. */
    fun onEnableRequested() {
        telemetryPrefs.setEnabled(true)
        _uiState.update { it.copy(telemetryEnabled = true) }
    }

    fun onDisable() {
        telemetryPrefs.setEnabled(false)
        stopTracking()
        _uiState.update {
            it.copy(telemetryEnabled = false, telemetryActive = false, awaitingBackgroundGrant = false)
        }
    }

    /** Called after the foreground [FINE, COARSE] request resolves. */
    fun onForegroundResult() {
        if (!hasForegroundLocationPermission()) {
            // Foreground denied — abandon the flow, revert the toggle.
            onDisable()
            return
        }
        if (hasBackgroundLocationPermission(application)) {
            activate()
        } else {
            _uiState.update { it.copy(awaitingBackgroundGrant = true) }
        }
    }

    /**
     * Re-check on ON_RESUME (returning from the system Settings deep-link, or after a
     * revoke elsewhere). Starts the trail when fully granted; stops it when the grant is gone.
     */
    fun refreshOnResume() {
        if (!telemetryPrefs.isEnabled()) {
            _uiState.update {
                it.copy(telemetryEnabled = false, telemetryActive = false, awaitingBackgroundGrant = false)
            }
            return
        }
        if (hasBackgroundLocationPermission(application)) {
            activate()
        } else if (_uiState.value.telemetryActive) {
            // Grant was revoked while active — stop the trail.
            stopTracking()
            _uiState.update { it.copy(telemetryActive = false) }
        }
    }

    private fun activate() {
        if (!_uiState.value.telemetryActive) {
            ContextCompat.startForegroundService(
                application,
                Intent(application, LocationTrackingService::class.java)
            )
            TelemetryScheduler.enqueue(application)
        }
        _uiState.update {
            it.copy(telemetryEnabled = true, telemetryActive = true, awaitingBackgroundGrant = false)
        }
    }

    private fun stopTracking() {
        application.stopService(Intent(application, LocationTrackingService::class.java))
        TelemetryScheduler.cancel(application)
    }

    fun hasForegroundLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            application, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                application, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasActivityRecognitionPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            application, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
    }
}
