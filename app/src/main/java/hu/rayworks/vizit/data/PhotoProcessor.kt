package hu.rayworks.vizit.data

import android.content.Context
import android.graphics.Bitmap
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
            jpegBytes = ByteArrayOutputStream().use { output ->
                flattened.compress(Bitmap.CompressFormat.JPEG, quality, output)
                output.toByteArray()
            }
            quality -= 8
        } while (jpegBytes.size > MAX_ENCODED_BYTES && quality >= 40)

        listOf(decoded, cropped, scaled, flattened)
            .distinct()
            .forEach(Bitmap::recycle)

        return Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
    }
}
