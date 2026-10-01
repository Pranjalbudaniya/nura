package com.nura.messaging.core.common.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
data class NuraSpacing(
    val extraSmall: Dp = 4.dp,
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
    val extraLarge: Dp = 40.dp,
    val screenPadding: Dp = 20.dp,
    val buttonHeight: Dp = 54.dp,
    val inputHeight: Dp = 54.dp,
    val oAuthButtonHeight: Dp = 50.dp,
    val iconButtonSize: Dp = 44.dp
)

val LocalSpacing = staticCompositionLocalOf { NuraSpacing() }
