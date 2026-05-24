package com.example.pulsar.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CaramelBrown,
    onPrimary = WarmWhite,
    primaryContainer = WarmSandGold,
    onPrimaryContainer = DeepCharcoal,
    secondary = SageGreen,
    onSecondary = WarmWhite,
    background = WarmLinen,
    onBackground = DeepCharcoal,
    surface = WarmWhite,
    onSurface = DeepCharcoal,
    onSurfaceVariant = WarmTaupe,
    outline = SoftGrey
)

private val DarkColorScheme = darkColorScheme(
    primary = CaramelLight,
    onPrimary = DeepBrown,
    primaryContainer = CaramelBrown,
    onPrimaryContainer = WarmWhite,
    secondary = SageLight,
    onSecondary = DeepBrown,
    background = DeepBrown,
    onBackground = WarmLinen,
    surface = DarkSurface,
    onSurface = WarmLinen,
    onSurfaceVariant = LightTaupe,
    outline = WarmTaupe,
    surfaceContainer = DarkSurface
)

/**
 * The main application theme applying colors, typography, and shapes based on the current configuration.
 */
@Composable
fun PulsarTheme(
    appTheme: String = "Auto",
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val isDark = when (appTheme) {
        "Light" -> false
        "Dark" -> true
        else -> darkTheme
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDark
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !isDark
            window.navigationBarColor = colorScheme.background.toArgb()
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulsarTypography,
        shapes = PulsarShapes,
        content = content
    )
}