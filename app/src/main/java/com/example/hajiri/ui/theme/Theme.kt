package com.example.hajiri.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.hajiri.model.AppThemeStyle

val LocalHajiriPalette = staticCompositionLocalOf { LiquidLightPalette }

@Composable
fun HajiriTheme(
    themeStyle: AppThemeStyle = AppThemeStyle.LIQUID_LIGHT,
    content: @Composable () -> Unit
) {
    val palette = when (themeStyle) {
        AppThemeStyle.LIQUID_LIGHT -> LiquidLightPalette
        AppThemeStyle.OCEAN_GLASS -> OceanGlassPalette
        AppThemeStyle.ROSE_GOLD -> RoseGoldPalette
        AppThemeStyle.LIQUID_DARK -> LiquidDarkPalette
    }

    val colorScheme = if (palette.isDark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = TextLight,
            secondary = palette.accentSecondary,
            onSecondary = TextLight,
            tertiary = OtOrange,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.glassCard,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.glassCardHigh,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.glassBorderMid
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = TextLight,
            secondary = palette.accentSecondary,
            onSecondary = TextLight,
            tertiary = OtOrange,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.glassCard,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.glassCardHigh,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.glassBorderMid
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !palette.isDark
                insetsController.isAppearanceLightNavigationBars = !palette.isDark
            }
        }
    }

    CompositionLocalProvider(LocalHajiriPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content
        )
    }
}
