package com.nura.messaging.core.common.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Semantic Light Palette from Stitch Project
private val LightPrimary = Color(0xFFA43716)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFFC54F2C)
private val LightOnPrimaryContainer = Color(0xFFFFFBFF)

private val LightSecondary = Color(0xFF56615D)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFD7E2DD)
private val LightOnSecondaryContainer = Color(0xFF5A6561)

private val LightSurface = Color(0xFFFCF8FB)
private val LightOnSurface = Color(0xFF1B1B1D)
private val LightSurfaceVariant = Color(0xFFE4E2E4)
private val LightOnSurfaceVariant = Color(0xFF58423C)

private val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
private val LightSurfaceContainerLow = Color(0xFFF6F3F5)
private val LightSurfaceContainer = Color(0xFFF0EDEF)
private val LightSurfaceContainerHigh = Color(0xFFEAE7EA)
private val LightSurfaceContainerHighest = Color(0xFFE4E2E4)

private val LightOutline = Color(0xFF8B716A)
private val LightOutlineVariant = Color(0xFFDFC0B7)

private val LightError = Color(0xFFBA1A1A)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFFFDAD6)
private val LightOnErrorContainer = Color(0xFF93000A)

// Semantic Dark Palette from Stitch Project
private val DarkPrimary = Color(0xFFFFB5A0)
private val DarkOnPrimary = Color(0xFF5F1500)
private val DarkPrimaryContainer = Color(0xFFE86E4A)
private val DarkOnPrimaryContainer = Color(0xFF561200)

private val DarkSecondary = Color(0xFFA6A6AC)
private val DarkOnSecondary = Color(0xFF1B1B1D)
private val DarkSecondaryContainer = Color(0xFF2A2A2E)
private val DarkOnSecondaryContainer = Color(0xFFFFA085)

private val DarkSurface = Color(0xFF131315)
private val DarkOnSurface = Color(0xFFFCF8FB)
private val DarkSurfaceVariant = Color(0xFF353437)
private val DarkOnSurfaceVariant = Color(0xFFDEC0B8)

private val DarkSurfaceContainerLowest = Color(0xFF0E0E10)
private val DarkSurfaceContainerLow = Color(0xFF1B1B1D)
private val DarkSurfaceContainer = Color(0xFF1F1F21)
private val DarkSurfaceContainerHigh = Color(0xFF2A2A2C)
private val DarkSurfaceContainerHighest = Color(0xFF353437)

private val DarkOutline = Color(0xFFA68B83)
private val DarkOutlineVariant = Color(0xFF57423C)

private val DarkError = Color(0xFFFFB4AB)
private val DarkOnError = Color(0xFF690005)
private val DarkErrorContainer = Color(0xFF93000A)
private val DarkOnErrorContainer = Color(0xFFFFDAD6)

// Predefined Theme Accent Palettes
val RadiantTerracottaLight = Color(0xFFA43716)
val RadiantTerracottaDark = Color(0xFFE86E4A)
val AlpineEmeraldLight = Color(0xFF1E6B47)
val AlpineEmeraldDark = Color(0xFF34A870)
val NordicCobaltLight = Color(0xFF2B5B9E)
val NordicCobaltDark = Color(0xFF4D88DB)
val RoyalAmethystLight = Color(0xFF6B43A6)
val RoyalAmethystDark = Color(0xFF9B6FE3)

// Stitch Custom Brand & Input Semantic Tokens
@Immutable
data class NuraColorTokens(
    val terracottaAccent: Color,
    val brandLogoText: Color,
    val inputBackground: Color,
    val inputBorder: Color,
    val inputBorderFocus: Color,
    val inputPlaceholder: Color,
    val googleButtonBackground: Color,
    val googleButtonBorder: Color,
    val googleButtonText: Color,
    val secondaryButtonBackground: Color,
    val secondaryButtonBorder: Color,
    val strengthWeak: Color,
    val strengthMedium: Color,
    val strengthStrong: Color,
    val strengthBarInactive: Color,
    val dividerColor: Color,
    val cardDarkBubble: Color,
    val cardDarkBorder: Color,
    val cardLightBubble: Color,
    val cardLightOnBubble: Color,
    val cardLightDuration: Color,
    val waveformDark: Color,
    val waveformLight: Color,
    val badgeBackground: Color,
    val badgeBorder: Color,
    val badgeText: Color,
    val legalText: Color,
    val subtitleText: Color
)

val LightNuraColorTokens = NuraColorTokens(
    terracottaAccent = Color(0xFFA43716),
    brandLogoText = Color(0xFF19191B),
    inputBackground = Color(0xFFF6F3F5),
    inputBorder = Color(0xFFE4E2E4),
    inputBorderFocus = Color(0xFFA43716),
    inputPlaceholder = Color(0xFF8B716A),
    googleButtonBackground = Color(0xFFF0EDEF),
    googleButtonBorder = Color(0xFFE4E2E4),
    googleButtonText = Color(0xFF1B1B1D),
    secondaryButtonBackground = Color(0xFFF0EDEF),
    secondaryButtonBorder = Color(0xFFE4E2E4),
    strengthWeak = Color(0xFFE53935),
    strengthMedium = Color(0xFFFBC02D),
    strengthStrong = Color(0xFF2E7D32),
    strengthBarInactive = Color(0xFFD7E2DD),
    dividerColor = Color(0xFFE4E2E4),
    cardDarkBubble = Color(0xFFF0EDEF),
    cardDarkBorder = Color(0xFFE4E2E4),
    cardLightBubble = Color(0xFFFFFFFF),
    cardLightOnBubble = Color(0xFF1B1B1D),
    cardLightDuration = Color(0xFF5A6561),
    waveformDark = Color(0xFF1B1B1D),
    waveformLight = Color(0xFFD7E2DD),
    badgeBackground = Color(0xFFF0EDEF),
    badgeBorder = Color(0xFFE4E2E4),
    badgeText = Color(0xFF56615D),
    legalText = Color(0xFF8B716A),
    subtitleText = Color(0xFF56615D)
)

val DarkNuraColorTokens = NuraColorTokens(
    terracottaAccent = Color(0xFFE86E4A),
    brandLogoText = Color(0xFFFCF8FB),
    inputBackground = Color(0xFF1C1C20),
    inputBorder = Color(0xFF2D2D33),
    inputBorderFocus = Color(0xFFE86E4A),
    inputPlaceholder = Color(0xFF6E6E76),
    googleButtonBackground = Color(0xFF222226),
    googleButtonBorder = Color(0xFF34343A),
    googleButtonText = Color(0xFFFCF8FB),
    secondaryButtonBackground = Color(0xFF222226),
    secondaryButtonBorder = Color(0xFF36363C),
    strengthWeak = Color(0xFFE53935),
    strengthMedium = Color(0xFFFBC02D),
    strengthStrong = Color(0xFF43A047),
    strengthBarInactive = Color(0xFF2E2E35),
    dividerColor = Color(0xFF2D2D33),
    cardDarkBubble = Color(0xFF1F1F25),
    cardDarkBorder = Color(0xFF2E2E36),
    cardLightBubble = Color(0xFFFCF8FB),
    cardLightOnBubble = Color(0xFF131315),
    cardLightDuration = Color(0xFF555259),
    waveformDark = Color(0xFF202024),
    waveformLight = Color(0xFFD4CFD2),
    badgeBackground = Color(0xFF1F1F23),
    badgeBorder = Color(0xFF2D2D35),
    badgeText = Color(0xFFA6A6AC),
    legalText = Color(0xFF767680),
    subtitleText = Color(0xFFA6A6AC)
)

val LocalNuraColorTokens = staticCompositionLocalOf { LightNuraColorTokens }

val NuraLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer
)

val NuraDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer
)
