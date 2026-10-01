package com.nura.messaging.features.auth.presentation.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import com.nura.messaging.features.auth.presentation.util.QrCodeGenerator
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

@Composable
fun QrScannerScreen(
    onNavigateBack: () -> Unit,
    onQrDetected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    // Scanned result dialog state
    var scannedContent by remember { mutableStateOf<String?>(null) }

    // Gallery QR picker launcher
    val galleryPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val bitmap = BitmapFactory.decodeStream(stream)
                    if (bitmap != null) {
                        val decoded = QrCodeGenerator.decodeQrFromBitmap(bitmap)
                        if (!decoded.isNullOrBlank()) {
                            scannedContent = decoded
                        } else {
                            Toast.makeText(context, "No Nura QR code found in image", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Could not read image file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Animated scanner laser line
    val infiniteTransition = rememberInfiniteTransition(label = "laser")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_progress"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color.Black
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Camera Preview Viewfinder
            if (hasCameraPermission) {
                val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }

                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.surfaceProvider = previewView.surfaceProvider
                            }

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                if (scannedContent == null) {
                                    val text = analyzeImageProxy(imageProxy)
                                    if (!text.isNullOrBlank()) {
                                        scannedContent = text
                                    }
                                }
                                imageProxy.close()
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                            } catch (e: Exception) {
                                // Camera bind failed
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                DisposableEffect(Unit) {
                    onDispose {
                        cameraExecutor.shutdown()
                    }
                }
            } else {
                // Camera permission denied placeholder
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.QrCodeScanner,
                        contentDescription = null,
                        tint = colors.terracottaAccent,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "To scan a Nura QR key in real-time, please grant camera access, or add a QR picture from your gallery below.",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 14.sp,
                        color = colors.subtitleText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        colors = ButtonDefaults.buttonColors(containerColor = colors.terracottaAccent),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Grant Permission",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Viewfinder Scrim & Reticle Overlay
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val boxWidth = constraints.maxWidth.toFloat()
                val boxHeight = constraints.maxHeight.toFloat()
                val reticleSizeDp = 260.dp
                val reticleSizePx = with(density) { reticleSizeDp.toPx() }
                val cornerRadiusPx = with(density) { 32.dp.toPx() }

                val left = (boxWidth - reticleSizePx) / 2f
                val top = (boxHeight - reticleSizePx) / 2f - 40f
                val rect = androidx.compose.ui.geometry.Rect(left, top, left + reticleSizePx, top + reticleSizePx)

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        addRoundRect(
                            RoundRect(rect, CornerRadius(cornerRadiusPx, cornerRadiusPx))
                        )
                    }

                    // Scrim darkens outer frame
                    clipPath(path, clipOp = ClipOp.Difference) {
                        drawRect(Color.Black.copy(alpha = 0.65f))
                    }

                    // Corner brackets
                    val bracketLen = 32.dp.toPx()
                    val strokeW = 4.dp.toPx()
                    val bracketColor = colors.terracottaAccent

                    // Top Left
                    drawLine(bracketColor, Offset(left, top + bracketLen), Offset(left, top + 16.dp.toPx()), strokeW)
                    drawLine(bracketColor, Offset(left, top), Offset(left + bracketLen, top), strokeW)

                    // Top Right
                    drawLine(bracketColor, Offset(left + reticleSizePx - bracketLen, top), Offset(left + reticleSizePx, top), strokeW)
                    drawLine(bracketColor, Offset(left + reticleSizePx, top), Offset(left + reticleSizePx, top + bracketLen), strokeW)

                    // Bottom Left
                    drawLine(bracketColor, Offset(left, top + reticleSizePx - bracketLen), Offset(left, top + reticleSizePx), strokeW)
                    drawLine(bracketColor, Offset(left, top + reticleSizePx), Offset(left + bracketLen, top + reticleSizePx), strokeW)

                    // Bottom Right
                    drawLine(bracketColor, Offset(left + reticleSizePx - bracketLen, top + reticleSizePx), Offset(left + reticleSizePx, top + reticleSizePx), strokeW)
                    drawLine(bracketColor, Offset(left + reticleSizePx, top + reticleSizePx - bracketLen), Offset(left + reticleSizePx, top + reticleSizePx), strokeW)

                    // Animated Laser line
                    val laserY = top + (reticleSizePx * laserProgress)
                    drawLine(
                        color = colors.terracottaAccent.copy(alpha = 0.85f),
                        start = Offset(left + 8.dp.toPx(), laserY),
                        end = Offset(left + reticleSizePx - 8.dp.toPx(), laserY),
                        strokeWidth = 2.5.dp.toPx()
                    )
                }
            }

            // Top Header: Back Button on top-left and Centered Title below status bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        .border(1.dp, colors.badgeBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Text(
                    text = "Scan Nura Key",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Below Option Button: "Add a QR picture" from gallery
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Align the QR code within the frame to connect",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { galleryPicker.launch("image/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.badgeBorder)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        tint = colors.terracottaAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Add a QR picture",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    )
                }
            }

            // Scanned Result Confirmation Dialog
            scannedContent?.let { payload ->
                val usernameMatch = Regex("user/([^?&]+)").find(payload)?.groupValues?.get(1)
                    ?: Regex("@([a-zA-Z0-9_]+)").find(payload)?.groupValues?.get(1)
                    ?: payload.take(24)

                Dialog(onDismissRequest = { scannedContent = null }) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp)),
                        color = colorScheme.surfaceContainerLow,
                        tonalElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .background(colors.terracottaAccent.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = colors.terracottaAccent,
                                    modifier = Modifier.size(32.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Nura Key Detected!",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = colorScheme.onSurface
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Connect with @$usernameMatch on Nura?",
                                fontFamily = PlusJakartaSansFamily,
                                fontSize = 14.sp,
                                color = colors.subtitleText,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { scannedContent = null },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.badgeBorder)
                                ) {
                                    Text(
                                        text = "Scan Again",
                                        fontFamily = PlusJakartaSansFamily,
                                        fontSize = 14.sp,
                                        color = colors.brandLogoText
                                    )
                                }

                                Button(
                                    onClick = {
                                        scannedContent = null
                                        onQrDetected(payload)
                                        onNavigateBack()
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(46.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = colors.terracottaAccent)
                                ) {
                                    Text(
                                        text = "Connect",
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fast YUV frame analyzer using ZXing PlanarYUVLuminanceSource
 */
@OptIn(ExperimentalGetImage::class)
private fun analyzeImageProxy(imageProxy: ImageProxy): String? {
    val mediaImage = imageProxy.image ?: return null
    return try {
        val planes = mediaImage.planes
        val yBuffer = planes[0].buffer
        val ySize = yBuffer.remaining()
        val yBytes = ByteArray(ySize)
        yBuffer.get(yBytes)

        val width = imageProxy.width
        val height = imageProxy.height
        val rowStride = planes[0].rowStride

        val source = PlanarYUVLuminanceSource(
            yBytes,
            rowStride,
            height,
            0,
            0,
            width,
            height,
            false
        )
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
        MultiFormatReader().decode(binaryBitmap).text
    } catch (e: Exception) {
        null
    }
}
