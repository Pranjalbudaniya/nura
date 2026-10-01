package com.nura.messaging.features.auth.presentation.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

object QrCodeGenerator {

    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        foregroundColorArgb: Int,
        backgroundColorArgb: Int
    ): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1,
                EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val pixels = IntArray(width * height)
            for (y in 0 until height) {
                val offset = y * width
                for (x in 0 until width) {
                    pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColorArgb else backgroundColorArgb
                }
            }
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    fun decodeQrFromBitmap(bitmap: Bitmap): String? {
        return try {
            val width = bitmap.width
            val height = bitmap.height
            val pixels = IntArray(width * height)
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            val source = RGBLuminanceSource(width, height, pixels)
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))
            val hints = mapOf(
                DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),
                DecodeHintType.TRY_HARDER to true
            )
            val result = MultiFormatReader().apply {
                setHints(hints)
            }.decode(binaryBitmap)
            result.text
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates a branded share card bitmap containing the QR code, "nura." logo, username, and key.
     */
    fun createShareCardBitmap(
        qrBitmap: Bitmap,
        username: String,
        userKey: String,
        cardBgArgb: Int,
        textPrimaryArgb: Int,
        textSecondaryArgb: Int,
        accentArgb: Int
    ): Bitmap {
        val cardWidth = 720
        val cardHeight = 960
        val result = Bitmap.createBitmap(cardWidth, cardHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Card background
        val bgPaint = Paint().apply {
            color = cardBgArgb
            isAntiAlias = true
        }
        val cardRect = RectF(0f, 0f, cardWidth.toFloat(), cardHeight.toFloat())
        canvas.drawRoundRect(cardRect, 48f, 48f, bgPaint)

        // "nura." header
        val brandPaint = Paint().apply {
            color = textPrimaryArgb
            textSize = 52f
            isFakeBoldText = true
            isAntiAlias = true
        }
        canvas.drawText("nura.", 60f, 110f, brandPaint)

        // Subtitle
        val subPaint = Paint().apply {
            color = textSecondaryArgb
            textSize = 28f
            isAntiAlias = true
        }
        canvas.drawText("Scan to connect on Nura", 60f, 155f, subPaint)

        // QR Code in center
        val qrSize = 440
        val qrLeft = (cardWidth - qrSize) / 2
        val qrTop = 220
        val qrDest = Rect(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
        canvas.drawBitmap(qrBitmap, null, qrDest, null)

        // Center dot on QR
        val dotPaint = Paint().apply {
            color = accentArgb
            isAntiAlias = true
        }
        canvas.drawCircle(cardWidth / 2f, qrTop + qrSize / 2f, 18f, dotPaint)

        // Username
        val userPaint = Paint().apply {
            color = textPrimaryArgb
            textSize = 40f
            isFakeBoldText = true
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(username, cardWidth / 2f, 740f, userPaint)

        // Key code badge
        val keyPaint = Paint().apply {
            color = textSecondaryArgb
            textSize = 28f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText("Key: $userKey", cardWidth / 2f, 790f, keyPaint)

        // Footer
        val footerPaint = Paint().apply {
            color = textSecondaryArgb
            textSize = 22f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            alpha = 180
        }
        canvas.drawText("Private • Secure • Connected", cardWidth / 2f, 880f, footerPaint)

        return result
    }

    /**
     * Saves bitmap to cache directory and returns a shareable content Uri.
     */
    fun saveBitmapToCache(
        context: Context,
        bitmap: Bitmap,
        filename: String
    ): Uri? {
        return try {
            val cacheFolder = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(cacheFolder, filename)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            null
        }
    }
}
