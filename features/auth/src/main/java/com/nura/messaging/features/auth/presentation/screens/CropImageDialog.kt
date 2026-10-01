package com.nura.messaging.features.auth.presentation.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

@Composable
fun CropImageDialog(
    sourceUri: Uri,
    onDismiss: () -> Unit,
    onCropConfirmed: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val cropBoxSizeDp = 260.dp
    val cropBoxSizePx = with(density) { cropBoxSizeDp.toPx() }

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Transform states: zoom and pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Load bitmap asynchronously with proper bounds sampling & EXIF rotation
    LaunchedEffect(sourceUri) {
        withContext(Dispatchers.IO) {
            try {
                val openStream: () -> java.io.InputStream? = {
                    try {
                        val scheme = sourceUri.scheme
                        if (scheme == "file" || (sourceUri.path != null && scheme != "content")) {
                            val path = sourceUri.path ?: sourceUri.toString().removePrefix("file://")
                            val f = File(path)
                            if (f.exists()) f.inputStream() else context.contentResolver.openInputStream(sourceUri)
                        } else {
                            context.contentResolver.openInputStream(sourceUri)
                        }
                    } catch (e: Exception) {
                        null
                    }
                }

                val bytes = openStream()?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    sourceBitmap = null
                    return@withContext
                }

                // 1. Decode bounds
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)

                val maxDim = max(boundsOpts.outWidth, boundsOpts.outHeight)
                var sampleSize = 1
                while (maxDim / sampleSize > 2048) {
                    sampleSize *= 2
                }

                // 2. Decode sampled bitmap
                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                val sampledBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)

                // 3. Check EXIF rotation if available
                val finalBitmap = sampledBitmap?.let { bmp ->
                    try {
                        val orientation = java.io.ByteArrayInputStream(bytes).use { exifStream ->
                            android.media.ExifInterface(exifStream).getAttributeInt(
                                android.media.ExifInterface.TAG_ORIENTATION,
                                android.media.ExifInterface.ORIENTATION_NORMAL
                            )
                        }

                        val matrix = Matrix()
                        when (orientation) {
                            android.media.ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                            android.media.ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                            android.media.ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                        }
                        if (!matrix.isIdentity) {
                            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
                        } else {
                            bmp
                        }
                    } catch (_: Exception) {
                        bmp
                    }
                }

                sourceBitmap = finalBitmap
            } catch (e: Exception) {
                sourceBitmap = null
            } finally {
                isLoading = false
            }
        }
    }

    BackHandler(onBack = onDismiss)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = colorScheme.surface
    ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Top Header Bar with status bar insets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.badgeBackground, CircleShape)
                            .border(1.dp, colors.badgeBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Cancel",
                            tint = colors.brandLogoText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Crop Avatar",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = colorScheme.onSurface
                    )

                    // Reset button
                    IconButton(
                        onClick = {
                            scale = 1f
                            offsetX = 0f
                            offsetY = 0f
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.badgeBackground, CircleShape)
                            .border(1.dp, colors.badgeBorder, CircleShape)
                    ) {
                        Text(
                            text = "1x",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = colors.subtitleText
                        )
                    }
                }

                // Middle Crop Viewport
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidth = constraints.maxWidth.toFloat()
                    val containerHeight = constraints.maxHeight.toFloat()

                    val cropCornerRadiusPx = with(density) { 56.dp.toPx() }

                    val cropLeft = (containerWidth - cropBoxSizePx) / 2f
                    val cropTop = (containerHeight - cropBoxSizePx) / 2f
                    val cropRect = Rect(cropLeft, cropTop, cropLeft + cropBoxSizePx, cropTop + cropBoxSizePx)

                    val bmp = sourceBitmap
                    if (bmp != null) {
                        // Image layer with gestures
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.8f, 5f)
                                        offsetX += pan.x
                                        offsetY += pan.y
                                    }
                                }
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                        translationX = offsetX
                                        translationY = offsetY
                                    }
                            ) {
                                val bmpWidth = bmp.width.toFloat()
                                val bmpHeight = bmp.height.toFloat()

                                // Fit bitmap nicely in the crop area by default
                                val fitScale = max(cropBoxSizePx / bmpWidth, cropBoxSizePx / bmpHeight)
                                val drawWidth = bmpWidth * fitScale
                                val drawHeight = bmpHeight * fitScale
                                val drawLeft = (size.width - drawWidth) / 2f
                                val drawTop = (size.height - drawHeight) / 2f

                                drawImage(
                                    image = bmp.asImageBitmap(),
                                    dstOffset = androidx.compose.ui.unit.IntOffset(drawLeft.toInt(), drawTop.toInt()),
                                    dstSize = androidx.compose.ui.unit.IntSize(drawWidth.toInt(), drawHeight.toInt())
                                )
                            }
                        }

                        // Scrim Overlay: Dim outside the crop box
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cropPath = Path().apply {
                                addRoundRect(
                                    RoundRect(
                                        rect = cropRect,
                                        cornerRadius = CornerRadius(cropCornerRadiusPx, cropCornerRadiusPx)
                                    )
                                )
                            }

                            clipPath(cropPath, clipOp = ClipOp.Difference) {
                                drawRect(color = Color.Black.copy(alpha = 0.72f))
                            }

                            // Subtle rule-of-thirds grid inside crop window
                            val step = cropBoxSizePx / 3f
                            val gridColor = colors.terracottaAccent.copy(alpha = 0.25f)
                            drawLine(gridColor, Offset(cropLeft + step, cropTop), Offset(cropLeft + step, cropTop + cropBoxSizePx), 1.dp.toPx())
                            drawLine(gridColor, Offset(cropLeft + step * 2, cropTop), Offset(cropLeft + step * 2, cropTop + cropBoxSizePx), 1.dp.toPx())
                            drawLine(gridColor, Offset(cropLeft, cropTop + step), Offset(cropLeft + cropBoxSizePx, cropTop + step), 1.dp.toPx())
                            drawLine(gridColor, Offset(cropLeft, cropTop + step * 2), Offset(cropLeft + cropBoxSizePx, cropTop + step * 2), 1.dp.toPx())
                        }

                        // Rounded squircle border indicator
                        Box(
                            modifier = Modifier
                                .size(cropBoxSizeDp)
                                .border(2.dp, colors.terracottaAccent.copy(alpha = 0.85f), RoundedCornerShape(56.dp))
                        )
                    } else if (isLoading) {
                        Text(
                            text = "Loading photo...",
                            fontFamily = PlusJakartaSansFamily,
                            color = colors.subtitleText,
                            fontSize = 15.sp
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Text(
                                text = "Unable to process captured photo",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.brandLogoText,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Please try capturing again or select an image from gallery.",
                                fontFamily = PlusJakartaSansFamily,
                                color = colors.subtitleText,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Bottom Confirmation Card / Dialog
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    color = colorScheme.surfaceContainerLow,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Pinch to zoom, drag to adjust avatar framing",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            color = colors.subtitleText
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.badgeBorder),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = colors.badgeBackground,
                                    contentColor = colors.brandLogoText
                                )
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }

                            Button(
                                onClick = {
                                    val bmp = sourceBitmap ?: return@Button
                                    coroutineScope.launch(Dispatchers.IO) {
                                        try {
                                            val baseDim = min(bmp.width, bmp.height)
                                            val safeScale = max(scale, 0.8f)
                                            val cropSize = (baseDim / safeScale).toInt().coerceIn(64, baseDim)

                                            val maxStartX = max(0, bmp.width - cropSize)
                                            val maxStartY = max(0, bmp.height - cropSize)

                                            val offsetXRatio = if (cropBoxSizePx > 0f) offsetX / (cropBoxSizePx * scale) else 0f
                                            val offsetYRatio = if (cropBoxSizePx > 0f) offsetY / (cropBoxSizePx * scale) else 0f

                                            val idealStartX = ((bmp.width - cropSize) / 2f) - (offsetXRatio * bmp.width)
                                            val idealStartY = ((bmp.height - cropSize) / 2f) - (offsetYRatio * bmp.height)

                                            val startX = idealStartX.toInt().coerceIn(0, maxStartX)
                                            val startY = idealStartY.toInt().coerceIn(0, maxStartY)

                                            val cropped = Bitmap.createBitmap(bmp, startX, startY, cropSize, cropSize)
                                            val finalBitmap = if (cropSize > 640) {
                                                Bitmap.createScaledBitmap(cropped, 640, 640, true)
                                            } else {
                                                cropped
                                            }

                                            val cacheFolder = File(context.cacheDir, "cropped_avatars").apply { mkdirs() }
                                            val cropFile = File(cacheFolder, "avatar_${System.currentTimeMillis()}.png")
                                            FileOutputStream(cropFile).use { out ->
                                                finalBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
                                            }

                                            withContext(Dispatchers.Main) {
                                                onCropConfirmed(Uri.fromFile(cropFile))
                                            }
                                        } catch (e: Exception) {
                                            withContext(Dispatchers.Main) {
                                                onDismiss()
                                            }
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.terracottaAccent,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(
                                    text = "Confirm",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
