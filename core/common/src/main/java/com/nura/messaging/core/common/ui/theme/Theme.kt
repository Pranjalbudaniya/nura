package com.nura.messaging.core.common.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext

object NuraTheme {
    val colors: NuraColorTokens
        @Composable
        @ReadOnlyComposable
        get() = LocalNuraColorTokens.current

    val spacing: NuraSpacing
        @Composable
        @ReadOnlyComposable
        get() = LocalSpacing.current
}

@Composable
fun NuraTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    ThemePreferencesManager.init(context)
    val themeConfig by ThemePreferencesManager.themeConfig.collectAsState()

    val darkTheme = when (themeConfig.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val selectedAccentColor = if (darkTheme) themeConfig.accent.darkColor else themeConfig.accent.lightColor

    val baseColorScheme = if (darkTheme) NuraDarkColorScheme else NuraLightColorScheme
    val colorScheme = baseColorScheme.copy(
        primary = selectedAccentColor,
        primaryContainer = selectedAccentColor
    )

    val baseNuraColors = if (darkTheme) DarkNuraColorTokens else LightNuraColorTokens
    val nuraColors = baseNuraColors.copy(
        terracottaAccent = selectedAccentColor,
        inputBorderFocus = selectedAccentColor
    )
    val spacing = NuraSpacing()

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalNuraColorTokens provides nuraColors,
        LocalSpacing provides spacing
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = NuraTypography,
            shapes = NuraShapes,
            content = content
        )
    }
}
