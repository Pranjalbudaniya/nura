package com.nura.messaging.features.chat.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import android.media.ExifInterface
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.min

enum class CropAspectRatio(val label: String, val ratioWidth: Float, val ratioHeight: Float) {
    FREE("Free", 1f, 1f),
    SQUARE("1:1", 1f, 1f),
    FOUR_THREE("4:3", 4f, 3f),
    SIXTEEN_NINE("16:9", 16f, 9f)
}

@Composable
fun ChatImageCropDialog(
    sourceUri: Uri,
    onDismiss: () -> Unit,
    onCropConfirmed: (ByteArray) -> Unit
) {
    val context = LocalContext.current
    val colors = NuraTheme.colors
    val density = LocalDensity.current

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var displayedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var rotationAngle by remember { mutableIntStateOf(0) }
    var selectedRatio by remember { mutableStateOf(CropAspectRatio.FREE) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Load bitmap with memory-safe downsampling and EXIF orientation
    LaunchedEffect(sourceUri) {
        isLoading = true
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
                    } catch (_: Exception) {
                        null
                    }
                }

                val bytes = openStream()?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    originalBitmap = null
                    displayedBitmap = null
                    isLoading = false
                    return@withContext
                }

                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)

                val maxDim = max(boundsOpts.outWidth, boundsOpts.outHeight)
                var sampleSize = 1
                while (maxDim / sampleSize > 2048) {
                    sampleSize *= 2
                }

                val decodeOpts = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                }
                var decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)

                // EXIF rotation detection
                try {
                    val exifStream = openStream()
                    if (exifStream != null) {
                        val exif = ExifInterface(exifStream)
                        val orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL
                        )
                        val exifAngle = when (orientation) {
                            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                            else -> 0f
                        }
                        if (exifAngle != 0f && decoded != null) {
                            val matrix = Matrix().apply { postRotate(exifAngle) }
                            val rotated = Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
                            if (rotated != decoded) {
                                decoded.recycle()
                                decoded = rotated
                            }
                        }
                        exifStream.close()
                    }
                } catch (_: Exception) {}

                originalBitmap = decoded
                displayedBitmap = decoded
            } catch (_: Exception) {
                originalBitmap = null
                displayedBitmap = null
            } finally {
                isLoading = false
            }
        }
    }

    // Handle manual 90° rotation
    LaunchedEffect(rotationAngle) {
        val base = originalBitmap ?: return@LaunchedEffect
        if (rotationAngle == 0) {
            displayedBitmap = base
        } else {
            withContext(Dispatchers.Default) {
                val matrix = Matrix().apply { postRotate(rotationAngle.toFloat()) }
                displayedBitmap = Bitmap.createBitmap(base, 0, 0, base.width, base.height, matrix, true)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.badgeBackground)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cancel",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = "Crop Photo",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = Color.White
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = {
                                rotationAngle = (rotationAngle + 90) % 360
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.badgeBackground)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.RotateRight,
                                contentDescription = "Rotate 90 degrees",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                                rotationAngle = 0
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.badgeBackground)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Zoom and Position",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Center Crop Viewport
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clipToBounds(),
                    contentAlignment = Alignment.Center
                ) {
                    val containerWidth = constraints.maxWidth.toFloat()
                    val containerHeight = constraints.maxHeight.toFloat()

                    // Calculate crop box dimensions based on chosen aspect ratio
                    val maxCropWidth = containerWidth * 0.90f
                    val maxCropHeight = containerHeight * 0.85f

                    val cropWidth: Float
                    val cropHeight: Float

                    when (selectedRatio) {
                        CropAspectRatio.FREE -> {
                            val bmp = displayedBitmap
                            if (bmp != null) {
                                val bmpRatio = bmp.width.toFloat() / bmp.height.toFloat()
                                if (bmpRatio > maxCropWidth / maxCropHeight) {
                                    cropWidth = maxCropWidth
                                    cropHeight = maxCropWidth / bmpRatio
                                } else {
                                    cropHeight = maxCropHeight
                                    cropWidth = maxCropHeight * bmpRatio
                                }
                            } else {
                                cropWidth = min(maxCropWidth, maxCropHeight)
                                cropHeight = cropWidth
                            }
                        }
                        CropAspectRatio.SQUARE -> {
                            val dim = min(maxCropWidth, maxCropHeight)
                            cropWidth = dim
                            cropHeight = dim
                        }
                        CropAspectRatio.FOUR_THREE -> {
                            val targetRatio = 4f / 3f
                            if (maxCropWidth / targetRatio <= maxCropHeight) {
                                cropWidth = maxCropWidth
                                cropHeight = maxCropWidth / targetRatio
                            } else {
                                cropHeight = maxCropHeight
                                cropWidth = maxCropHeight * targetRatio
                            }
                        }
                        CropAspectRatio.SIXTEEN_NINE -> {
                            val targetRatio = 16f / 9f
                            if (maxCropWidth / targetRatio <= maxCropHeight) {
                                cropWidth = maxCropWidth
                                cropHeight = maxCropWidth / targetRatio
                            } else {
                                cropHeight = maxCropHeight
                                cropWidth = maxCropHeight * targetRatio
                            }
                        }
                    }

                    val cropLeft = (containerWidth - cropWidth) / 2f
                    val cropTop = (containerHeight - cropHeight) / 2f
                    val cropRect = Rect(cropLeft, cropTop, cropLeft + cropWidth, cropTop + cropHeight)

                    val bmp = displayedBitmap
                    if (bmp != null) {
                        // Transformable image layer
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        scale = (scale * zoom).coerceIn(0.5f, 5f)
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

                                val fitScale = max(cropWidth / bmpWidth, cropHeight / bmpHeight)
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

                        // Darkened Scrim Overlay outside Crop Area
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val cropPath = Path().apply {
                                addRect(cropRect)
                            }

                            clipPath(cropPath, clipOp = ClipOp.Difference) {
                                drawRect(color = Color.Black.copy(alpha = 0.72f))
                            }

                            // Rule of thirds grid lines
                            val gridColor = colors.terracottaAccent.copy(alpha = 0.35f)
                            val stepX = cropWidth / 3f
                            val stepY = cropHeight / 3f

                            drawLine(gridColor, Offset(cropLeft + stepX, cropTop), Offset(cropLeft + stepX, cropTop + cropHeight), 1.dp.toPx())
                            drawLine(gridColor, Offset(cropLeft + stepX * 2, cropTop), Offset(cropLeft + stepX * 2, cropTop + cropHeight), 1.dp.toPx())
                            drawLine(gridColor, Offset(cropLeft, cropTop + stepY), Offset(cropLeft + cropWidth, cropTop + stepY), 1.dp.toPx())
                            drawLine(gridColor, Offset(cropLeft, cropTop + stepY * 2), Offset(cropLeft + cropWidth, cropTop + stepY * 2), 1.dp.toPx())
                        }

                        // Crisp Crop Box Border
                        Box(
                            modifier = Modifier
                                .size(with(density) { cropWidth.toDp() }, with(density) { cropHeight.toDp() })
                                .border(1.5.dp, colors.terracottaAccent, RoundedCornerShape(2.dp))
                        )
                    } else if (isLoading) {
                        CircularProgressIndicator(
                            color = colors.terracottaAccent,
                            modifier = Modifier.size(36.dp)
                        )
                    } else {
                        Text(
                            text = "Could not load image",
                            fontFamily = PlusJakartaSansFamily,
                            color = colors.subtitleText,
                            fontSize = 14.sp
                        )
                    }
                }

                // Bottom Controls: Ratio Selectors & Send Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Aspect ratio chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CropAspectRatio.values().forEach { ratio ->
                            val isSelected = selectedRatio == ratio
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) colors.terracottaAccent else colors.badgeBackground)
                                    .border(
                                        1.dp,
                                        if (isSelected) colors.terracottaAccent else colors.badgeBorder,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable {
                                        selectedRatio = ratio
                                        scale = 1f
                                        offsetX = 0f
                                        offsetY = 0f
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = ratio.label,
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                    color = if (isSelected) Color.White else colors.subtitleText
                                )
                            }
                        }
                    }

                    // Bottom Confirm Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Pinch to zoom & adjust frame",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 12.sp,
                            color = colors.subtitleText
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(24.dp))
                                .background(colors.terracottaAccent)
                                .clickable {
                                    val bmp = displayedBitmap ?: return@clickable
                                    // Execute crop calculation on background thread
                                    val stream = ByteArrayOutputStream()
                                    try {
                                        // In standard display, calculate crop ratio relative to displayed center
                                        val bmpWidth = bmp.width.toFloat()
                                        val bmpHeight = bmp.height.toFloat()

                                        // Apply scale and translation bounds to accurately crop visible region
                                        val effectiveScale = max(scale, 0.1f)
                                        val cropAspect = when (selectedRatio) {
                                            CropAspectRatio.FREE -> bmpWidth / bmpHeight
                                            CropAspectRatio.SQUARE -> 1f
                                            CropAspectRatio.FOUR_THREE -> 4f / 3f
                                            CropAspectRatio.SIXTEEN_NINE -> 16f / 9f
                                        }

                                        // Determine crop dimensions on original bitmap
                                        var targetW = (bmpWidth / effectiveScale).toInt()
                                        var targetH = (targetW / cropAspect).toInt()

                                        if (targetH > bmpHeight) {
                                            targetH = (bmpHeight / effectiveScale).toInt()
                                            targetW = (targetH * cropAspect).toInt()
                                        }

                                        targetW = targetW.coerceIn(1, bmp.width)
                                        targetH = targetH.coerceIn(1, bmp.height)

                                        // Offset mapped to bitmap coordinate space
                                        val centerX = (bmp.width / 2f) - (offsetX / effectiveScale)
                                        val centerY = (bmp.height / 2f) - (offsetY / effectiveScale)

                                        val startX = (centerX - targetW / 2f).toInt().coerceIn(0, max(0, bmp.width - targetW))
                                        val startY = (centerY - targetH / 2f).toInt().coerceIn(0, max(0, bmp.height - targetH))

                                        val croppedBmp = Bitmap.createBitmap(bmp, startX, startY, targetW, targetH)
                                        croppedBmp.compress(Bitmap.CompressFormat.JPEG, 92, stream)
                                        if (croppedBmp != bmp) {
                                            croppedBmp.recycle()
                                        }
                                        onCropConfirmed(stream.toByteArray())
                                    } catch (_: Exception) {
                                        bmp.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                                        onCropConfirmed(stream.toByteArray())
                                    } finally {
                                        stream.close()
                                    }
                                }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Send",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = Color.White
                                )
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
