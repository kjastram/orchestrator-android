package com.orchestrator.app.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Single source of truth for the background-location grant check. Previously duplicated in
 * three places (`OrchestratorApp`, `TelemetryWorker`, `SettingsViewModel`) — all point here
 * now so they can't drift. Requires FINE or COARSE foreground AND (API 29+) background.
 */
fun hasBackgroundLocationPermission(context: Context): Boolean {
    val foreground = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    val background = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }

    return foreground && background
}
