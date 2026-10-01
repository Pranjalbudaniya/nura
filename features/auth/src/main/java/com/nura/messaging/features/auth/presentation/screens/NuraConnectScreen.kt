package com.nura.messaging.features.auth.presentation.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nura.messaging.core.common.ui.component.NuraWordmark
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import com.nura.messaging.features.auth.presentation.util.QrCodeGenerator
import com.nura.messaging.features.auth.presentation.viewmodel.AuthViewModel

@Composable
fun NuraConnectScreen(
    viewModel: AuthViewModel,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val rawHandle = when {
        !uiState.username.isNullOrBlank() -> uiState.username
        !uiState.currentUser?.username.isNullOrBlank() -> uiState.currentUser!!.username
        !uiState.name.isNullOrBlank() -> uiState.name
        !uiState.currentUser?.name.isNullOrBlank() -> uiState.currentUser!!.name
        else -> "Julian Vance"
    }
    val displayHandle = if (rawHandle.startsWith("@")) rawHandle else "@$rawHandle"

    val userKey = "NU-" + (uiState.currentUser?.id?.take(4)?.uppercase()?.ifEmpty { null } ?: "7K4P") +
            "-" + (uiState.currentUser?.id?.takeLast(4)?.uppercase()?.ifEmpty { null } ?: "92MX")

    var showQrScanner by remember { mutableStateOf(false) }

    val qrPayload = "nura://user/$rawHandle?key=$userKey"
    val qrBitmap = remember(qrPayload, colors.brandLogoText) {
        QrCodeGenerator.generateQrBitmap(
            content = qrPayload,
            sizePx = 512,
            foregroundColorArgb = colors.brandLogoText.toArgb(),
            backgroundColorArgb = Color.Transparent.toArgb()
        )
    }

    val onProceed = {
        viewModel.completePostSignUpOnboarding()
        onContinue()
    }

    val onSkipStep = {
        viewModel.completePostSignUpOnboarding()
        onSkip()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Atmospheric Subtle Radiant Background Aura
            Box(
                modifier = Modifier
                    .size(width = 320.dp, height = 280.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-40).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                colors.terracottaAccent.copy(alpha = 0.12f),
                                colors.terracottaAccent.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        ),
                        shape = CircleShape
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 22.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Header Wordmark Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NuraWordmark(fontSize = 24.sp)
                    }

                    // Typography & Spatial Context
                    Text(
                        text = "This is your Nura key.",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 26.sp,
                        lineHeight = 34.sp,
                        letterSpacing = (-0.02).sp,
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Share it with someone to connect on Nura.",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = colors.subtitleText
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Identity Panel & QR Module Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(colorScheme.surfaceContainerLow)
                            .shadow(
                                elevation = 8.dp,
                                shape = RoundedCornerShape(28.dp),
                                ambientColor = colorScheme.surfaceContainerLowest,
                                spotColor = colorScheme.surfaceContainerLowest
                            )
                            .padding(vertical = 28.dp, horizontal = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Architectural Framing for QR Code
                            Box(
                                modifier = Modifier
                                    .size(240.dp)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(colorScheme.surfaceVariant)
                                    .padding(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                val bmp = qrBitmap
                                if (bmp != null) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Nura Key QR Code",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                    )
                                } else {
                                    NuraQrMatrixCanvas(
                                        modifier = Modifier.fillMaxSize(),
                                        accentColor = colors.terracottaAccent,
                                        matrixColor = colorScheme.surfaceContainerLowest,
                                        frameColor = colorScheme.surfaceVariant,
                                        anchorColor = colorScheme.outline.copy(alpha = 0.5f)
                                    )
                                }

                                // Stylized Minimalist Radiant Center Dot
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(colors.terracottaAccent, CircleShape)
                                        .border(3.dp, colorScheme.surfaceVariant, CircleShape)
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // Identity Details: User handle
                            Text(
                                text = displayHandle,
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp,
                                letterSpacing = (-0.01).sp,
                                color = colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Secondary Actions: Share & Scan
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Share Key Action
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.surfaceContainerHigh)
                                .clickable {
                                    val qrBmp = qrBitmap ?: QrCodeGenerator.generateQrBitmap(
                                        content = qrPayload,
                                        sizePx = 512,
                                        foregroundColorArgb = Color.Black.toArgb(),
                                        backgroundColorArgb = Color.White.toArgb()
                                    )
                                    val helloMessage = "Hey! Let's connect on Nura.\n\nAdd me by username: $displayHandle"

                                    if (qrBmp != null) {
                                        val cardBmp = QrCodeGenerator.createShareCardBitmap(
                                            qrBitmap = qrBmp,
                                            username = displayHandle,
                                            userKey = userKey,
                                            cardBgArgb = colorScheme.surfaceContainerLow.toArgb(),
                                            textPrimaryArgb = colorScheme.onSurface.toArgb(),
                                            textSecondaryArgb = colors.subtitleText.toArgb(),
                                            accentArgb = colors.terracottaAccent.toArgb()
                                        )
                                        val contentUri = QrCodeGenerator.saveBitmapToCache(
                                            context = context,
                                            bitmap = cardBmp,
                                            filename = "nura_key_${rawHandle.filter { it.isLetterOrDigit() }}.png"
                                        )
                                        if (contentUri != null) {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "image/png"
                                                putExtra(Intent.EXTRA_STREAM, contentUri)
                                                putExtra(Intent.EXTRA_TEXT, helloMessage)
                                                putExtra(Intent.EXTRA_SUBJECT, "Connect with me on Nura: $displayHandle")
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Nura Key"))
                                        } else {
                                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_TEXT, helloMessage)
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Share Nura Key"))
                                        }
                                    } else {
                                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_TEXT, helloMessage)
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Share Nura Key"))
                                    }
                                }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                tint = colors.terracottaAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Share Key",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = colorScheme.onSurface
                            )
                        }

                        // Scan QR Action
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.surfaceContainerHigh)
                                .clickable {
                                    showQrScanner = true
                                }
                                .padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.QrCodeScanner,
                                contentDescription = null,
                                tint = colors.terracottaAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Scan QR",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = colorScheme.onSurface
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Reassuring Architectural Privacy Statement
                    Text(
                        text = "Your key lets people find and connect with you. You choose who to connect with.",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                        color = colors.subtitleText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                    )
                }

                // Primary Bottom Action: Continue to Nura & Skip for now
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 28.dp, bottom = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onProceed,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.terracottaAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Continue to Nura",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                letterSpacing = 0.01.sp
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = "Skip for now",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = colors.subtitleText,
                        modifier = Modifier
                            .clickable { onSkipStep() }
                            .padding(vertical = 8.dp, horizontal = 16.dp)
                    )
                }
            }
        }

        // In-app QR Scanner
        if (showQrScanner) {
            QrScannerScreen(
                onNavigateBack = { showQrScanner = false },
                onQrDetected = { payload ->
                    showQrScanner = false
                    val scannedUser = Regex("user/([^?&]+)").find(payload)?.groupValues?.get(1)
                        ?: Regex("@([a-zA-Z0-9_]+)").find(payload)?.groupValues?.get(1)
                        ?: payload.take(20)
                    Toast.makeText(context, "Connected with @$scannedUser on Nura!", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

@Composable
private fun NuraQrMatrixCanvas(
    accentColor: Color,
    matrixColor: Color,
    frameColor: Color,
    anchorColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // 1. Minimalist Corner Bracket Anchors
        val anchorLen = 14.dp.toPx()
        val strokeW = 2.dp.toPx()
        val pad = 4.dp.toPx()

        // Top-Left Anchor
        drawLine(anchorColor, Offset(pad, pad), Offset(pad + anchorLen, pad), strokeW)
        drawLine(anchorColor, Offset(pad, pad), Offset(pad, pad + anchorLen), strokeW)

        // Top-Right Anchor
        drawLine(anchorColor, Offset(w - pad, pad), Offset(w - pad - anchorLen, pad), strokeW)
        drawLine(anchorColor, Offset(w - pad, pad), Offset(w - pad, pad + anchorLen), strokeW)

        // Bottom-Left Anchor
        drawLine(anchorColor, Offset(pad, h - pad), Offset(pad + anchorLen, h - pad), strokeW)
        drawLine(anchorColor, Offset(pad, h - pad), Offset(pad, h - pad - anchorLen), strokeW)

        // Bottom-Right Anchor
        drawLine(anchorColor, Offset(w - pad, h - pad), Offset(w - pad - anchorLen, h - pad), strokeW)
        drawLine(anchorColor, Offset(w - pad, h - pad), Offset(w - pad, h - pad - anchorLen), strokeW)

        // 2. High-Contrast QR Matrix Position Patterns (Top-Left, Top-Right, Bottom-Left)
        val finderSize = w * 0.25f
        val finderRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
        val innerClearRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
        val innerDotRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())

        fun drawFinder(x: Float, y: Float) {
            // Outer solid square
            drawRoundRect(
                color = matrixColor,
                topLeft = Offset(x, y),
                size = Size(finderSize, finderSize),
                cornerRadius = finderRadius
            )
            // Inner cutout
            val cutPad = finderSize * 0.15f
            drawRoundRect(
                color = frameColor,
                topLeft = Offset(x + cutPad, y + cutPad),
                size = Size(finderSize - (cutPad * 2), finderSize - (cutPad * 2)),
                cornerRadius = innerClearRadius
            )
            // Center solid dot
            val dotPad = finderSize * 0.30f
            drawRoundRect(
                color = matrixColor,
                topLeft = Offset(x + dotPad, y + dotPad),
                size = Size(finderSize - (dotPad * 2), finderSize - (dotPad * 2)),
                cornerRadius = innerDotRadius
            )
        }

        val margin = w * 0.08f
        // Top-Left Finder
        drawFinder(margin, margin)
        // Top-Right Finder
        drawFinder(w - margin - finderSize, margin)
        // Bottom-Left Finder
        drawFinder(margin, h - margin - finderSize)

        // 3. Data Modules Matrix (Architectural Grid Pattern)
        val gridStep = (w - (margin * 2)) / 16f
        val moduleSize = gridStep * 0.82f
        val modCorner = CornerRadius(2.dp.toPx(), 2.dp.toPx())

        val modulePoints = listOf(
            Pair(6, 1), Pair(8, 1), Pair(10, 1),
            Pair(11, 2), Pair(7, 3), Pair(9, 3),
            Pair(6, 5), Pair(8, 5), Pair(11, 5),
            Pair(1, 6), Pair(3, 6), Pair(5, 6), Pair(7, 6), Pair(10, 6), Pair(12, 6), Pair(14, 6),
            Pair(1, 8), Pair(4, 8), Pair(13, 8), Pair(15, 8),
            Pair(2, 10), Pair(5, 10), Pair(12, 10), Pair(14, 10),
            Pair(6, 7), Pair(9, 7), Pair(6, 9), Pair(9, 9),
            Pair(7, 11), Pair(9, 11), Pair(11, 11), Pair(13, 11),
            Pair(6, 13), Pair(8, 13), Pair(10, 13), Pair(12, 13), Pair(14, 13),
            Pair(7, 14), Pair(9, 14), Pair(13, 14),
            Pair(15, 13), Pair(15, 15)
        )

        for ((col, row) in modulePoints) {
            val posX = margin + (col * gridStep)
            val posY = margin + (row * gridStep)
            drawRoundRect(
                color = matrixColor,
                topLeft = Offset(posX, posY),
                size = Size(moduleSize, moduleSize),
                cornerRadius = modCorner
            )
        }

        // 4. Centerpiece: Radiant Terracotta Focal Dot
        val centerCardSize = w * 0.20f
        val centerCardRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        val centerX = (w - centerCardSize) / 2f
        val centerY = (h - centerCardSize) / 2f

        // Center squircle container
        drawRoundRect(
            color = frameColor,
            topLeft = Offset(centerX, centerY),
            size = Size(centerCardSize, centerCardSize),
            cornerRadius = centerCardRadius,
            style = Fill
        )
        // Terracotta accent dot with soft glowing radiance
        val dotRadius = centerCardSize * 0.22f
        drawCircle(
            color = accentColor.copy(alpha = 0.35f),
            radius = dotRadius * 1.6f,
            center = Offset(w / 2f, h / 2f)
        )
        drawCircle(
            color = accentColor,
            radius = dotRadius,
            center = Offset(w / 2f, h / 2f)
        )
    }
}
