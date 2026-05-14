package com.orchestrator.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val PurplePrimary = Color(0xFF6650A4)
private val PurpleOnPrimary = Color(0xFFFFFFFF)
private val PurpleContainer = Color(0xFFEADDFF)
private val PurpleOnContainer = Color(0xFF21005E)

private val DarkPurplePrimary = Color(0xFFCFBCFF)
private val DarkPurpleOnPrimary = Color(0xFF381E72)
private val DarkPurpleContainer = Color(0xFF4F378A)
private val DarkPurpleOnContainer = Color(0xFFEADDFF)

private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,
    onPrimary = PurpleOnPrimary,
    primaryContainer = PurpleContainer,
    onPrimaryContainer = PurpleOnContainer
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkPurplePrimary,
    onPrimary = DarkPurpleOnPrimary,
    primaryContainer = DarkPurpleContainer,
    onPrimaryContainer = DarkPurpleOnContainer
)

@Composable
fun OrchestratorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
