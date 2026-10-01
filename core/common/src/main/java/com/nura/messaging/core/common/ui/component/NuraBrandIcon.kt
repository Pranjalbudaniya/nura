package com.nura.messaging.core.common.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nura.messaging.core.common.ui.theme.NuraTheme

/**
 * Renders the official Nura brand icon mark (architectural 'n' arch with radiant terracotta accent dot).
 * Automatically adapts between dark mode (pure white arch) and light mode (dark charcoal arch).
 */
@Composable
fun NuraBrandIcon(
    modifier: Modifier = Modifier,
    size: Dp = 108.dp,
    isDark: Boolean = isSystemInDarkTheme(),
    archColor: Color = if (isDark) Color.White else NuraTheme.colors.brandLogoText,
    dotColor: Color = NuraTheme.colors.terracottaAccent
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val w = this.size.width
            val h = this.size.height

            val s = minOf(w, h) / 1024f
            val ox = (w - 1024f * s) / 2f
            val oy = (h - 1024f * s) / 2f

            // 1. Draw the architectural 'n' arch
            val path = Path().apply {
                moveTo(ox + 333.5f * s, oy + 653f * s)
                lineTo(ox + 333.5f * s, oy + 514f * s)
                // Outer arc
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = ox + (470f - 136.5f) * s,
                        top = oy + (514f - 136.5f) * s,
                        right = ox + (470f + 136.5f) * s,
                        bottom = oy + (514f + 136.5f) * s
                    ),
                    startAngleDegrees = 180f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = false
                )
                lineTo(ox + 606.5f * s, oy + 656f * s)
                lineTo(ox + 561f * s, oy + 656f * s)
                // Inner bottom-right fillet
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = ox + (561f - 15.5f) * s,
                        top = oy + (640.5f - 15.5f) * s,
                        right = ox + (561f + 15.5f) * s,
                        bottom = oy + (640.5f + 15.5f) * s
                    ),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(ox + 545.5f * s, oy + 514f * s)
                // Inner arc
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = ox + (470f - 75.5f) * s,
                        top = oy + (514f - 75.5f) * s,
                        right = ox + (470f + 75.5f) * s,
                        bottom = oy + (514f + 75.5f) * s
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = -180f,
                    forceMoveTo = false
                )
                lineTo(ox + 394.5f * s, oy + 653f * s)
                close()
            }
            drawPath(path = path, color = archColor)

            // 2. Draw the radiant terracotta accent dot
            drawCircle(
                color = dotColor,
                radius = 52f * s,
                center = Offset(ox + 644.2f * s, oy + 399f * s)
            )
        }
    }
}
