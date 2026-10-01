package com.nura.messaging.features.auth.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.unit.dp
import com.nura.messaging.core.common.ui.theme.ButtonShape
import com.nura.messaging.core.common.ui.theme.NuraTheme

@Composable
fun NuraGoogleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String = "Continue with Google",
    isLoading: Boolean = false,
    enabled: Boolean = true
) {
    val colors = NuraTheme.colors
    val spacing = NuraTheme.spacing
    val typography = MaterialTheme.typography

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(spacing.oAuthButtonHeight),
        enabled = enabled && !isLoading,
        shape = ButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = colors.googleButtonBackground,
            contentColor = colors.googleButtonText,
            disabledContainerColor = colors.googleButtonBackground.copy(alpha = 0.6f),
            disabledContentColor = colors.googleButtonText.copy(alpha = 0.5f)
        ),
        border = BorderStroke(1.dp, colors.googleButtonBorder),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = colors.googleButtonText,
                strokeWidth = 2.dp
            )
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                GoogleIcon(modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(spacing.small))
                Text(
                    text = text,
                    style = typography.labelLarge,
                    color = colors.googleButtonText
                )
            }
        }
    }
}

@Composable
private fun GoogleIcon(modifier: Modifier = Modifier) {
    val cutoutColor = NuraTheme.colors.googleButtonBackground
    Canvas(modifier = modifier) {
        val sizePx = size.minDimension
        val center = Offset(sizePx / 2f, sizePx / 2f)
        val radius = sizePx / 2f

        // 4 Google brand arc colors
        val red = Color(0xFFEA4335)
        val blue = Color(0xFF4285F4)
        val yellow = Color(0xFFFBBC05)
        val green = Color(0xFF34A853)

        // Draw Google G geometry
        val path = Path().apply {
            moveTo(center.x, center.y)
            lineTo(sizePx, center.y)
        }

        // Draw colored arcs around the perimeter
        drawArc(
            color = red,
            startAngle = 200f,
            sweepAngle = 110f,
            useCenter = true,
            size = size
        )
        drawArc(
            color = yellow,
            startAngle = 140f,
            sweepAngle = 60f,
            useCenter = true,
            size = size
        )
        drawArc(
            color = green,
            startAngle = 40f,
            sweepAngle = 100f,
            useCenter = true,
            size = size
        )
        drawArc(
            color = blue,
            startAngle = 310f,
            sweepAngle = 90f,
            useCenter = true,
            size = size
        )

        // Inner cutout for the G donut
        drawCircle(
            color = cutoutColor,
            radius = radius * 0.55f,
            center = center,
            style = Fill
        )

        // Blue crossbar
        drawRect(
            color = blue,
            topLeft = Offset(center.x, center.y - radius * 0.2f),
            size = androidx.compose.ui.geometry.Size(radius * 0.95f, radius * 0.4f)
        )
    }
}
