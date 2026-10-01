package com.nura.messaging.core.common.ui.theme

import android.content.Context
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode {
    SYSTEM,
    DARK,
    LIGHT
}

enum class CustomAccent(
    val displayName: String,
    val lightColor: Color,
    val darkColor: Color
) {
    TERRACOTTA(
        displayName = "Radiant Terracotta",
        lightColor = RadiantTerracottaLight,
        darkColor = RadiantTerracottaDark
    ),
    EMERALD(
        displayName = "Alpine Emerald",
        lightColor = AlpineEmeraldLight,
        darkColor = AlpineEmeraldDark
    ),
    COBALT(
        displayName = "Nordic Cobalt",
        lightColor = NordicCobaltLight,
        darkColor = NordicCobaltDark
    ),
    AMETHYST(
        displayName = "Royal Amethyst",
        lightColor = RoyalAmethystLight,
        darkColor = RoyalAmethystDark
    )
}

data class ThemeConfig(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: CustomAccent = CustomAccent.TERRACOTTA,
    val isHighContrast: Boolean = false
)

object ThemePreferencesManager {
    private const val PREFS_NAME = "nura_theme_preferences"
    private const val KEY_MODE = "theme_mode"
    private const val KEY_ACCENT = "custom_accent"
    private const val KEY_HIGH_CONTRAST = "high_contrast"

    private val _themeConfig = MutableStateFlow(ThemeConfig())
    val themeConfig: StateFlow<ThemeConfig> = _themeConfig.asStateFlow()
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val modeStr = prefs.getString(KEY_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val accentStr = prefs.getString(KEY_ACCENT, CustomAccent.TERRACOTTA.name) ?: CustomAccent.TERRACOTTA.name
        val highContrast = prefs.getBoolean(KEY_HIGH_CONTRAST, false)

        val mode = try { ThemeMode.valueOf(modeStr) } catch (_: Exception) { ThemeMode.SYSTEM }
        val accent = try { CustomAccent.valueOf(accentStr) } catch (_: Exception) { CustomAccent.TERRACOTTA }

        _themeConfig.value = ThemeConfig(mode, accent, highContrast)
        isInitialized = true
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        _themeConfig.value = _themeConfig.value.copy(themeMode = mode)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODE, mode.name)
            .apply()
    }

    fun setCustomAccent(context: Context, accent: CustomAccent) {
        _themeConfig.value = _themeConfig.value.copy(accent = accent)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_ACCENT, accent.name)
            .apply()
    }

    fun setHighContrast(context: Context, enabled: Boolean) {
        _themeConfig.value = _themeConfig.value.copy(isHighContrast = enabled)
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_HIGH_CONTRAST, enabled)
            .apply()
    }
}
