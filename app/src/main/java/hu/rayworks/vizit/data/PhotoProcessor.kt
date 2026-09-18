package hu.rayworks.vizit.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.net.Uri
import android.util.Base64
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

object PhotoProcessor {
    private const val TARGET_SIZE = 512
    private const val MAX_SOURCE_SIZE = 1_200
    private const val MAX_ENCODED_BYTES = 60_000
    private const val NFC_START_SIZE = 192
    private const val NFC_MIN_SIZE = 48

    fun loadSquareJpegBase64(context: Context, uri: Uri): String {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val width = info.size.width
            val height = info.size.height
            val longestSide = max(width, height)
            if (longestSide > MAX_SOURCE_SIZE) {
                val ratio = MAX_SOURCE_SIZE.toFloat() / longestSide
                decoder.setTargetSize(
                    (width * ratio).roundToInt(),
                    (height * ratio).roundToInt(),
                )
            }
        }

        val cropSize = minOf(decoded.width, decoded.height)
        val cropped = Bitmap.createBitmap(
            decoded,
            (decoded.width - cropSize) / 2,
            (decoded.height - cropSize) / 2,
            cropSize,
            cropSize,
        )
        val scaled = cropped.scale(TARGET_SIZE, TARGET_SIZE)
        val flattened = createBitmap(TARGET_SIZE, TARGET_SIZE)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(scaled, 0f, 0f, null)
        }

        var quality = 88
        var jpegBytes: ByteArray
        do {
            jpegBytes = compressJpeg(flattened, quality)
            quality -= 8
        } while (jpegBytes.size > MAX_ENCODED_BYTES && quality >= 40)

        listOf(decoded, cropped, scaled, flattened)
            .distinct()
            .forEach(Bitmap::recycle)

        return Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
    }

    fun optimizeContactPhotoBase64(photoBase64: String, maxJpegBytes: Int): String {
        if (photoBase64.isBlank() || maxJpegBytes <= 0) return ""

        val sourceBytes = runCatching { Base64.decode(photoBase64, Base64.DEFAULT) }.getOrNull()
            ?: return ""
        val decoded = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size) ?: return ""
        val cropSize = minOf(decoded.width, decoded.height)
        val square = Bitmap.createBitmap(
            decoded,
            (decoded.width - cropSize) / 2,
            (decoded.height - cropSize) / 2,
            cropSize,
            cropSize,
        )

        var size = minOf(NFC_START_SIZE, square.width, square.height)
        var quality = 78
        var result: ByteArray? = null

        while (size >= NFC_MIN_SIZE) {
            val scaled = if (square.width == size && square.height == size) {
                square
            } else {
                Bitmap.createScaledBitmap(square, size, size, true)
            }
            val flattened = createBitmap(size, size)
            Canvas(flattened).apply {
                drawColor(Color.WHITE)
                drawBitmap(scaled, 0f, 0f, null)
            }

            quality = 78
            while (quality >= 38) {
                val candidate = compressJpeg(flattened, quality)
                if (candidate.size <= maxJpegBytes) {
                    result = candidate
                    break
                }
                quality -= 8
            }

            flattened.recycle()
            if (scaled !== square) scaled.recycle()
            if (result != null) break
            size = (size * 0.80f).roundToInt()
        }

        if (square !== decoded) square.recycle()
        decoded.recycle()

        return result?.let { Base64.encodeToString(it, Base64.NO_WRAP) }.orEmpty()
    }

    private fun compressJpeg(bitmap: Bitmap, quality: Int): ByteArray =
        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, output)
            output.toByteArray()
        }
}
