package com.orchestrator.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.orchestrator.app.data.repository.AuthRepository
import com.orchestrator.app.data.store.TelemetryPrefs
import com.orchestrator.app.nav.AppNavGraph
import com.orchestrator.app.service.LocationTrackingService
import com.orchestrator.app.ui.theme.OrchestratorTheme
import com.orchestrator.app.util.hasBackgroundLocationPermission
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    @Inject
    lateinit var telemetryPrefs: TelemetryPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OrchestratorTheme {
                AppNavGraph(authRepository = authRepository)
            }
        }
    }

    /**
     * Sole reboot-recovery path for the location FGS. A `location` foreground service cannot
     * start from BOOT_COMPLETED on API 31+, so it's started here from a visible context when
     * opted-in + perms present. Starting an already-running FGS is a safe no-op.
     */
    override fun onStart() {
        super.onStart()
        if (telemetryPrefs.isEnabled() && hasBackgroundLocationPermission(this)) {
            ContextCompat.startForegroundService(
                this,
                Intent(this, LocationTrackingService::class.java)
            )
        }
    }
}
