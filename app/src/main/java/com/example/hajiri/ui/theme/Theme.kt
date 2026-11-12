package com.example.hajiri.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = AccentIndigo,
    onPrimary = TextLight,
    secondary = AccentPurple,
    onSecondary = TextLight,
    tertiary = OtOrange,
    background = LightBackground,
    onBackground = TextPrimary,
    surface = LiquidGlassCard,
    onSurface = TextPrimary,
    surfaceVariant = LiquidGlassCardSubtle,
    onSurfaceVariant = TextSecondary,
    outline = LiquidGlassBorder
)

@Composable
fun HajiriTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                // Light theme: dark status & navigation bar icons
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
