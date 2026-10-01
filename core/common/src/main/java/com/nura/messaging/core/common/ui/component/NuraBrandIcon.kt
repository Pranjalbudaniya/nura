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

            // Scale factor relative to 108dp base
            val scaleX = w / 108f
            val scaleY = h / 108f

            // 1. Draw the 'n' arch
            val path = Path().apply {
                moveTo(34f * scaleX, 72f * scaleY)
                lineTo(34f * scaleX, 53f * scaleY)
                // Outer arch
                cubicTo(
                    34f * scaleX, 41.5f * scaleY,
                    41.2f * scaleX, 35.5f * scaleY,
                    50f * scaleX, 35.5f * scaleY
                )
                cubicTo(
                    58.8f * scaleX, 35.5f * scaleY,
                    66f * scaleX, 41.5f * scaleY,
                    66f * scaleX, 53f * scaleY
                )
                lineTo(66f * scaleX, 72f * scaleY)
                lineTo(55f * scaleX, 72f * scaleY)
                lineTo(55f * scaleX, 53f * scaleY)
                // Inner arch
                cubicTo(
                    55f * scaleX, 47.5f * scaleY,
                    52.8f * scaleX, 44f * scaleY,
                    50f * scaleX, 44f * scaleY
                )
                cubicTo(
                    47.2f * scaleX, 44f * scaleY,
                    45f * scaleX, 47.5f * scaleY,
                    45f * scaleX, 53f * scaleY
                )
                lineTo(45f * scaleX, 72f * scaleY)
                close()
            }
            drawPath(path = path, color = archColor)

            // 2. Draw the radiant terracotta dot
            val dotCenterX = 69f * scaleX
            val dotCenterY = 38f * scaleY
            val dotRadius = 7.5f * minOf(scaleX, scaleY)
            drawCircle(
                color = dotColor,
                radius = dotRadius,
                center = Offset(dotCenterX, dotCenterY)
            )
        }
    }
}
