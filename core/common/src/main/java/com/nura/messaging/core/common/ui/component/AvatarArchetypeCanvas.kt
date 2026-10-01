package com.nura.messaging.core.common.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.nura.messaging.core.common.ui.theme.NuraTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Total number of curated procedural default avatars.
 */
const val TOTAL_AVATAR_PRESETS = 36

@Composable
fun AvatarArchetypeCanvas(
    presetIndex: Int,
    modifier: Modifier = Modifier
) {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    val terracotta = colors.terracottaAccent
    val sage = colors.strengthStrong
    val logoText = colors.brandLogoText
    val subtitle = colors.subtitleText

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val minDim = minOf(w, h)

        when (presetIndex % TOTAL_AVATAR_PRESETS) {
            0 -> {
                // Dual Monolith Pillars
                val barW = minDim * 0.18f
                val barH = minDim * 0.65f
                drawRoundRect(
                    color = terracotta,
                    topLeft = Offset(cx - barW * 1.3f, cy - barH / 2f),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(barW / 2f)
                )
                drawRoundRect(
                    color = terracotta.copy(alpha = 0.4f),
                    topLeft = Offset(cx + barW * 0.3f, cy - barH * 0.35f),
                    size = Size(barW, barH * 0.7f),
                    cornerRadius = CornerRadius(barW / 2f)
                )
            }
            1 -> {
                // Terracotta Concentric Rings
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(terracotta.copy(alpha = 0.9f), terracotta.copy(alpha = 0.3f)),
                        center = Offset(cx, cy),
                        radius = minDim * 0.42f
                    ),
                    radius = minDim * 0.40f
                )
                drawCircle(color = colorScheme.surface, radius = minDim * 0.18f)
            }
            2 -> {
                // Angular Crossing Beams
                val strokeW = minDim * 0.05f
                drawLine(
                    color = subtitle,
                    start = Offset(cx - minDim * 0.32f, cy + minDim * 0.35f),
                    end = Offset(cx + minDim * 0.35f, cy - minDim * 0.32f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = terracotta,
                    start = Offset(cx - minDim * 0.25f, cy + minDim * 0.30f),
                    end = Offset(cx + minDim * 0.15f, cy - minDim * 0.10f),
                    strokeWidth = strokeW * 0.8f,
                    cap = StrokeCap.Round
                )
            }
            3 -> {
                // Zen Solid Dot with Radiant Glow
                drawCircle(
                    color = terracotta.copy(alpha = 0.2f),
                    radius = minDim * 0.42f
                )
                drawCircle(
                    color = terracotta,
                    radius = minDim * 0.22f
                )
            }
            4 -> {
                // Moon & Orbital Planet
                drawCircle(color = subtitle.copy(alpha = 0.3f), radius = minDim * 0.32f)
                drawCircle(color = terracotta, radius = minDim * 0.12f, center = Offset(cx + minDim * 0.22f, cy - minDim * 0.18f))
            }
            5 -> {
                // Radiant Sunburst
                for (i in 0 until 8) {
                    val angle = (i * 45) * (Math.PI / 180.0)
                    val r1 = minDim * 0.22f
                    val r2 = minDim * 0.40f
                    drawLine(
                        color = terracotta,
                        start = Offset(cx + (r1 * cos(angle)).toFloat(), cy + (r1 * sin(angle)).toFloat()),
                        end = Offset(cx + (r2 * cos(angle)).toFloat(), cy + (r2 * sin(angle)).toFloat()),
                        strokeWidth = minDim * 0.06f,
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(color = terracotta, radius = minDim * 0.12f)
            }
            6 -> {
                // Prism Triangle
                val path = Path().apply {
                    moveTo(cx, cy - minDim * 0.35f)
                    lineTo(cx + minDim * 0.35f, cy + minDim * 0.28f)
                    lineTo(cx - minDim * 0.35f, cy + minDim * 0.28f)
                    close()
                }
                drawPath(path, color = sage, style = Stroke(width = minDim * 0.08f, cap = StrokeCap.Round))
                drawCircle(color = terracotta, radius = minDim * 0.09f, center = Offset(cx, cy + minDim * 0.05f))
            }
            7 -> {
                // Diamond Core
                val path = Path().apply {
                    moveTo(cx, cy - minDim * 0.38f)
                    lineTo(cx + minDim * 0.32f, cy)
                    lineTo(cx, cy + minDim * 0.38f)
                    lineTo(cx - minDim * 0.32f, cy)
                    close()
                }
                drawPath(path, color = terracotta.copy(alpha = 0.85f))
                drawCircle(color = colorScheme.surface, radius = minDim * 0.10f)
            }
            8 -> {
                // Hexagonal Matrix
                val path = Path().apply {
                    for (i in 0 until 6) {
                        val angle = (i * 60 - 30) * (Math.PI / 180.0)
                        val x = cx + (minDim * 0.36f * cos(angle)).toFloat()
                        val y = cy + (minDim * 0.36f * sin(angle)).toFloat()
                        if (i == 0) moveTo(x, y) else lineTo(x, y)
                    }
                    close()
                }
                drawPath(path, color = logoText, style = Stroke(width = minDim * 0.07f))
                drawCircle(color = terracotta, radius = minDim * 0.12f)
            }
            9 -> {
                // Ocean Wave Crest
                val wavePath = Path().apply {
                    moveTo(cx - minDim * 0.38f, cy)
                    cubicTo(
                        cx - minDim * 0.18f, cy - minDim * 0.35f,
                        cx - minDim * 0.05f, cy + minDim * 0.35f,
                        cx + minDim * 0.15f, cy
                    )
                    cubicTo(
                        cx + minDim * 0.25f, cy - minDim * 0.20f,
                        cx + minDim * 0.32f, cy - minDim * 0.10f,
                        cx + minDim * 0.38f, cy
                    )
                }
                drawPath(wavePath, color = sage, style = Stroke(width = minDim * 0.08f, cap = StrokeCap.Round))
                drawCircle(color = terracotta, radius = minDim * 0.08f, center = Offset(cx - minDim * 0.12f, cy - minDim * 0.22f))
            }
            10 -> {
                // Concentric Triple Ripples
                drawCircle(color = terracotta.copy(alpha = 0.25f), radius = minDim * 0.40f, style = Stroke(width = minDim * 0.05f))
                drawCircle(color = terracotta.copy(alpha = 0.55f), radius = minDim * 0.26f, style = Stroke(width = minDim * 0.06f))
                drawCircle(color = terracotta, radius = minDim * 0.12f)
            }
            11 -> {
                // Nautilus Spiral Segments
                drawArc(
                    color = terracotta,
                    startAngle = 0f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(cx - minDim * 0.32f, cy - minDim * 0.32f),
                    size = Size(minDim * 0.64f, minDim * 0.64f),
                    style = Stroke(width = minDim * 0.08f, cap = StrokeCap.Round)
                )
                drawCircle(color = sage, radius = minDim * 0.09f, center = Offset(cx + minDim * 0.05f, cy + minDim * 0.05f))
            }
            12 -> {
                // Starlight 4-Point Compass
                val path = Path().apply {
                    moveTo(cx, cy - minDim * 0.42f)
                    lineTo(cx + minDim * 0.10f, cy - minDim * 0.10f)
                    lineTo(cx + minDim * 0.42f, cy)
                    lineTo(cx + minDim * 0.10f, cy + minDim * 0.10f)
                    lineTo(cx, cy + minDim * 0.42f)
                    lineTo(cx - minDim * 0.10f, cy + minDim * 0.10f)
                    lineTo(cx - minDim * 0.42f, cy)
                    lineTo(cx - minDim * 0.10f, cy - minDim * 0.10f)
                    close()
                }
                drawPath(path, color = terracotta)
            }
            13 -> {
                // Horizon Beacon Light
                drawRoundRect(
                    color = subtitle.copy(alpha = 0.3f),
                    topLeft = Offset(cx - minDim * 0.38f, cy + minDim * 0.15f),
                    size = Size(minDim * 0.76f, minDim * 0.08f),
                    cornerRadius = CornerRadius(minDim * 0.04f)
                )
                drawCircle(
                    color = terracotta,
                    radius = minDim * 0.22f,
                    center = Offset(cx, cy - minDim * 0.05f)
                )
            }
            14 -> {
                // Crescent Moon Arc
                drawCircle(color = terracotta, radius = minDim * 0.35f)
                drawCircle(color = colorScheme.surface, radius = minDim * 0.28f, center = Offset(cx + minDim * 0.16f, cy - minDim * 0.10f))
            }
            15 -> {
                // Camera Iris Aperture
                val stroke = minDim * 0.06f
                for (i in 0 until 6) {
                    val angle = (i * 60) * (Math.PI / 180.0)
                    val p1 = Offset(cx + (minDim * 0.20f * cos(angle)).toFloat(), cy + (minDim * 0.20f * sin(angle)).toFloat())
                    val p2 = Offset(cx + (minDim * 0.38f * cos(angle + 0.8)).toFloat(), cy + (minDim * 0.38f * sin(angle + 0.8)).toFloat())
                    drawLine(color = if (i % 2 == 0) terracotta else sage, start = p1, end = p2, strokeWidth = stroke, cap = StrokeCap.Round)
                }
            }
            16 -> {
                // Isometric Cube
                val pathTop = Path().apply {
                    moveTo(cx, cy - minDim * 0.32f)
                    lineTo(cx + minDim * 0.28f, cy - minDim * 0.16f)
                    lineTo(cx, cy)
                    lineTo(cx - minDim * 0.28f, cy - minDim * 0.16f)
                    close()
                }
                val pathLeft = Path().apply {
                    moveTo(cx - minDim * 0.28f, cy - minDim * 0.16f)
                    lineTo(cx, cy)
                    lineTo(cx, cy + minDim * 0.32f)
                    lineTo(cx - minDim * 0.28f, cy + minDim * 0.16f)
                    close()
                }
                val pathRight = Path().apply {
                    moveTo(cx, cy)
                    lineTo(cx + minDim * 0.28f, cy - minDim * 0.16f)
                    lineTo(cx + minDim * 0.28f, cy + minDim * 0.16f)
                    lineTo(cx, cy + minDim * 0.32f)
                    close()
                }
                drawPath(pathTop, color = terracotta)
                drawPath(pathLeft, color = terracotta.copy(alpha = 0.65f))
                drawPath(pathRight, color = terracotta.copy(alpha = 0.40f))
            }
            17 -> {
                // Infinity Mobius
                val strokeW = minDim * 0.08f
                drawCircle(color = terracotta, radius = minDim * 0.18f, center = Offset(cx - minDim * 0.16f, cy), style = Stroke(strokeW))
                drawCircle(color = sage, radius = minDim * 0.18f, center = Offset(cx + minDim * 0.16f, cy), style = Stroke(strokeW))
            }
            18 -> {
                // Compass Needle
                val pathNorth = Path().apply {
                    moveTo(cx, cy - minDim * 0.40f)
                    lineTo(cx + minDim * 0.14f, cy)
                    lineTo(cx, cy)
                    close()
                }
                val pathSouth = Path().apply {
                    moveTo(cx, cy + minDim * 0.40f)
                    lineTo(cx - minDim * 0.14f, cy)
                    lineTo(cx, cy)
                    close()
                }
                drawPath(pathNorth, color = terracotta)
                drawPath(pathSouth, color = subtitle)
                drawCircle(color = colorScheme.surface, radius = minDim * 0.06f)
            }
            19 -> {
                // Double Chevrons
                val strokeW = minDim * 0.08f
                val path1 = Path().apply {
                    moveTo(cx - minDim * 0.28f, cy - minDim * 0.18f)
                    lineTo(cx, cy - minDim * 0.34f)
                    lineTo(cx + minDim * 0.28f, cy - minDim * 0.18f)
                }
                val path2 = Path().apply {
                    moveTo(cx - minDim * 0.28f, cy + minDim * 0.08f)
                    lineTo(cx, cy - minDim * 0.08f)
                    lineTo(cx + minDim * 0.28f, cy + minDim * 0.08f)
                }
                drawPath(path1, color = terracotta, style = Stroke(strokeW, cap = StrokeCap.Round))
                drawPath(path2, color = sage, style = Stroke(strokeW, cap = StrokeCap.Round))
            }
            20 -> {
                // Shield Emblem
                val path = Path().apply {
                    moveTo(cx, cy - minDim * 0.35f)
                    lineTo(cx + minDim * 0.30f, cy - minDim * 0.25f)
                    lineTo(cx + minDim * 0.24f, cy + minDim * 0.12f)
                    lineTo(cx, cy + minDim * 0.35f)
                    lineTo(cx - minDim * 0.24f, cy + minDim * 0.12f)
                    lineTo(cx - minDim * 0.30f, cy - minDim * 0.25f)
                    close()
                }
                drawPath(path, color = terracotta.copy(alpha = 0.85f))
                drawCircle(color = colorScheme.surface, radius = minDim * 0.09f)
            }
            21 -> {
                // Yin-Yang Harmony
                drawArc(color = terracotta, startAngle = 90f, sweepAngle = 180f, useCenter = true, topLeft = Offset(cx - minDim * 0.35f, cy - minDim * 0.35f), size = Size(minDim * 0.70f, minDim * 0.70f))
                drawArc(color = logoText, startAngle = 270f, sweepAngle = 180f, useCenter = true, topLeft = Offset(cx - minDim * 0.35f, cy - minDim * 0.35f), size = Size(minDim * 0.70f, minDim * 0.70f))
                drawCircle(color = logoText, radius = minDim * 0.175f, center = Offset(cx, cy - minDim * 0.175f))
                drawCircle(color = terracotta, radius = minDim * 0.175f, center = Offset(cx, cy + minDim * 0.175f))
                drawCircle(color = colorScheme.surface, radius = minDim * 0.06f, center = Offset(cx, cy - minDim * 0.175f))
                drawCircle(color = colorScheme.surface, radius = minDim * 0.06f, center = Offset(cx, cy + minDim * 0.175f))
            }
            22 -> {
                // Interlocking Circles (Venn)
                drawCircle(color = terracotta.copy(alpha = 0.6f), radius = minDim * 0.25f, center = Offset(cx - minDim * 0.14f, cy))
                drawCircle(color = sage.copy(alpha = 0.6f), radius = minDim * 0.25f, center = Offset(cx + minDim * 0.14f, cy))
            }
            23 -> {
                // Audio Frequency Spectrum
                val barW = minDim * 0.07f
                val heights = listOf(0.35f, 0.65f, 0.85f, 0.50f, 0.30f)
                val spacing = minDim * 0.13f
                val startX = cx - (spacing * 2f)
                heights.forEachIndexed { i, hRatio ->
                    val barH = minDim * hRatio
                    drawRoundRect(
                        color = if (i % 2 == 0) terracotta else sage,
                        topLeft = Offset(startX + (i * spacing) - barW / 2f, cy - barH / 2f),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW / 2f)
                    )
                }
            }
            24 -> {
                // Constellation Nodes
                val p1 = Offset(cx - minDim * 0.28f, cy - minDim * 0.20f)
                val p2 = Offset(cx + minDim * 0.10f, cy - minDim * 0.30f)
                val p3 = Offset(cx + minDim * 0.28f, cy + minDim * 0.10f)
                val p4 = Offset(cx - minDim * 0.08f, cy + minDim * 0.28f)
                drawLine(color = subtitle.copy(alpha = 0.5f), start = p1, end = p2, strokeWidth = 2.dp.toPx())
                drawLine(color = subtitle.copy(alpha = 0.5f), start = p2, end = p3, strokeWidth = 2.dp.toPx())
                drawLine(color = subtitle.copy(alpha = 0.5f), start = p3, end = p4, strokeWidth = 2.dp.toPx())
                drawLine(color = subtitle.copy(alpha = 0.5f), start = p4, end = p1, strokeWidth = 2.dp.toPx())
                listOf(p1, p2, p3, p4).forEachIndexed { i, p ->
                    drawCircle(color = if (i == 1) terracotta else sage, radius = minDim * 0.08f, center = p)
                }
            }
            25 -> {
                // Botanical Sprout Leaf
                val path = Path().apply {
                    moveTo(cx, cy + minDim * 0.35f)
                    cubicTo(cx - minDim * 0.35f, cy + minDim * 0.10f, cx - minDim * 0.25f, cy - minDim * 0.28f, cx, cy - minDim * 0.35f)
                    cubicTo(cx + minDim * 0.25f, cy - minDim * 0.28f, cx + minDim * 0.35f, cy + minDim * 0.10f, cx, cy + minDim * 0.35f)
                }
                drawPath(path, color = sage.copy(alpha = 0.85f))
                drawLine(color = terracotta, start = Offset(cx, cy + minDim * 0.35f), end = Offset(cx, cy - minDim * 0.15f), strokeWidth = minDim * 0.04f, cap = StrokeCap.Round)
            }
            26 -> {
                // Atomic Nucleus Orbit
                drawCircle(color = terracotta, radius = minDim * 0.12f)
                drawArc(
                    color = subtitle.copy(alpha = 0.6f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(cx - minDim * 0.35f, cy - minDim * 0.16f),
                    size = Size(minDim * 0.70f, minDim * 0.32f),
                    style = Stroke(width = minDim * 0.04f)
                )
            }
            27 -> {
                // Four-Dots Harmony Matrix
                val r = minDim * 0.10f
                val dist = minDim * 0.18f
                drawCircle(color = terracotta, radius = r, center = Offset(cx - dist, cy - dist))
                drawCircle(color = sage, radius = r, center = Offset(cx + dist, cy - dist))
                drawCircle(color = logoText, radius = r, center = Offset(cx - dist, cy + dist))
                drawCircle(color = terracotta.copy(alpha = 0.4f), radius = r, center = Offset(cx + dist, cy + dist))
            }
            28 -> {
                // Fibonacci Arc
                val strokeW = minDim * 0.07f
                drawArc(color = terracotta, startAngle = 180f, sweepAngle = 90f, useCenter = false, topLeft = Offset(cx - minDim * 0.35f, cy - minDim * 0.35f), size = Size(minDim * 0.70f, minDim * 0.70f), style = Stroke(strokeW, cap = StrokeCap.Round))
                drawArc(color = sage, startAngle = 270f, sweepAngle = 90f, useCenter = false, topLeft = Offset(cx - minDim * 0.20f, cy - minDim * 0.35f), size = Size(minDim * 0.40f, minDim * 0.40f), style = Stroke(strokeW, cap = StrokeCap.Round))
                drawCircle(color = terracotta, radius = minDim * 0.08f, center = Offset(cx + minDim * 0.10f, cy - minDim * 0.05f))
            }
            29 -> {
                // Flame Spark
                val path = Path().apply {
                    moveTo(cx, cy - minDim * 0.40f)
                    cubicTo(cx + minDim * 0.30f, cy - minDim * 0.10f, cx + minDim * 0.25f, cy + minDim * 0.30f, cx, cy + minDim * 0.38f)
                    cubicTo(cx - minDim * 0.25f, cy + minDim * 0.30f, cx - minDim * 0.30f, cy - minDim * 0.10f, cx, cy - minDim * 0.40f)
                }
                drawPath(path, color = terracotta)
                drawCircle(color = colorScheme.surface, radius = minDim * 0.09f, center = Offset(cx, cy + minDim * 0.15f))
            }
            30 -> {
                // Mountain Peak & Horizon
                val path = Path().apply {
                    moveTo(cx - minDim * 0.35f, cy + minDim * 0.25f)
                    lineTo(cx - minDim * 0.08f, cy - minDim * 0.22f)
                    lineTo(cx + minDim * 0.08f, cy - minDim * 0.05f)
                    lineTo(cx + minDim * 0.22f, cy - minDim * 0.28f)
                    lineTo(cx + minDim * 0.38f, cy + minDim * 0.25f)
                    close()
                }
                drawPath(path, color = terracotta.copy(alpha = 0.85f))
                drawCircle(color = sage, radius = minDim * 0.08f, center = Offset(cx - minDim * 0.20f, cy - minDim * 0.25f))
            }
            31 -> {
                // Eye of Serenity
                val path = Path().apply {
                    moveTo(cx - minDim * 0.38f, cy)
                    cubicTo(cx - minDim * 0.18f, cy - minDim * 0.26f, cx + minDim * 0.18f, cy - minDim * 0.26f, cx + minDim * 0.38f, cy)
                    cubicTo(cx + minDim * 0.18f, cy + minDim * 0.26f, cx - minDim * 0.18f, cy + minDim * 0.26f, cx - minDim * 0.38f, cy)
                }
                drawPath(path, color = logoText, style = Stroke(minDim * 0.06f))
                drawCircle(color = terracotta, radius = minDim * 0.12f)
            }
            32 -> {
                // Hourglass
                val path = Path().apply {
                    moveTo(cx - minDim * 0.26f, cy - minDim * 0.32f)
                    lineTo(cx + minDim * 0.26f, cy - minDim * 0.32f)
                    lineTo(cx - minDim * 0.26f, cy + minDim * 0.32f)
                    lineTo(cx + minDim * 0.26f, cy + minDim * 0.32f)
                    close()
                }
                drawPath(path, color = terracotta.copy(alpha = 0.75f))
                drawCircle(color = colorScheme.surface, radius = minDim * 0.05f)
            }
            33 -> {
                // Crosshair Target
                drawCircle(color = terracotta.copy(alpha = 0.7f), radius = minDim * 0.32f, style = Stroke(minDim * 0.06f))
                drawLine(color = terracotta, start = Offset(cx - minDim * 0.40f, cy), end = Offset(cx + minDim * 0.40f, cy), strokeWidth = minDim * 0.05f)
                drawLine(color = terracotta, start = Offset(cx, cy - minDim * 0.40f), end = Offset(cx, cy + minDim * 0.40f), strokeWidth = minDim * 0.05f)
                drawCircle(color = colorScheme.surface, radius = minDim * 0.12f)
            }
            34 -> {
                // Radial Dial
                for (i in 0 until 12) {
                    val angle = (i * 30) * (Math.PI / 180.0)
                    val r1 = minDim * 0.28f
                    val r2 = minDim * 0.38f
                    drawLine(
                        color = if (i % 3 == 0) terracotta else subtitle,
                        start = Offset(cx + (r1 * cos(angle)).toFloat(), cy + (r1 * sin(angle)).toFloat()),
                        end = Offset(cx + (r2 * cos(angle)).toFloat(), cy + (r2 * sin(angle)).toFloat()),
                        strokeWidth = minDim * 0.05f,
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(color = terracotta, radius = minDim * 0.10f)
            }
            else -> {
                // 35: Signature Nura Radiant Dot
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(terracotta, terracotta.copy(alpha = 0.4f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = minDim * 0.42f
                    ),
                    radius = minDim * 0.40f
                )
                drawCircle(color = Color.White, radius = minDim * 0.12f)
            }
        }
    }
}
