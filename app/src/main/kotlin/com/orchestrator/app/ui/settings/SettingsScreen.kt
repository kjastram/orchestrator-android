package com.orchestrator.app.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-check grants whenever we resume (returning from the Settings deep-link, or
    // after a revoke elsewhere). Enqueues/cancels the worker as appropriate.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshOnResume()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Activity recognition (steps) — requested opportunistically on first enable. Denial is
    // NON-FATAL: steps just go null and the trail continues, so the result is ignored.
    val activityRecognitionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* non-fatal — ignored */ }

    // Step 1: request FINE + COARSE together. FINE alone on API 31+ can be downgraded
    // to approximate, so both are requested to give the user the fine/coarse choice.
    val foregroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (viewModel.hasForegroundLocationPermission() &&
            !viewModel.hasActivityRecognitionPermission()
        ) {
            activityRecognitionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        viewModel.onForegroundResult()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Enable device telemetry",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Records a GPS trail in the background and uploads it, " +
                            "with battery and step stats, every 15 minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = uiState.telemetryEnabled,
                    onCheckedChange = { checked ->
                        if (checked) {
                            viewModel.onEnableRequested()
                            if (viewModel.hasForegroundLocationPermission()) {
                                if (!viewModel.hasActivityRecognitionPermission()) {
                                    activityRecognitionLauncher.launch(
                                        Manifest.permission.ACTIVITY_RECOGNITION
                                    )
                                }
                                viewModel.onForegroundResult()
                            } else {
                                foregroundLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        } else {
                            viewModel.onDisable()
                        }
                    }
                )
            }

            // Step 2: background ("Allow all the time") — cannot be obtained via a
            // permission launcher on API 30+, so deep-link to app Settings behind a
            // rationale. ON_RESUME re-check picks up the grant.
            if (uiState.awaitingBackgroundGrant && !uiState.telemetryActive) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Allow location \"all the time\"",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Background reporting needs \"Allow all the time\" location access. " +
                        "Open the system settings, tap Permissions → Location, and choose " +
                        "\"Allow all the time\".",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = {
                    val intent = Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                }) {
                    Text("Open app settings")
                }
            }

            if (uiState.telemetryActive) {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Trail is active. Location samples every ~5 minutes and uploads " +
                        "every 15 minutes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tip: to keep the trail from being interrupted, exclude this app " +
                        "from battery optimization in system settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
