package hu.rayworks.vizit.qr

import android.graphics.Bitmap
import com.google.zxing.BinaryBitmap
import com.google.zxing.ChecksumException
import com.google.zxing.DecodeHintType
import com.google.zxing.FormatException
import com.google.zxing.LuminanceSource
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.EnumMap

private const val MAX_STILL_PIXELS = 40_000_000L

private fun LuminanceSource.binarized(): BinaryBitmap = BinaryBitmap(HybridBinarizer(this))

/**
 * QR decoding for camera frames and picked stills, on the ZXing core the app
 * already ships for generating codes — no barcode SDK is pulled in for it.
 *
 * The reader keeps state between calls, so each decode resets it; an instance
 * is confined to the single analyzer thread that owns it.
 */
class QrFrameDecoder {
    private val reader = QRCodeReader()
    private val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java).apply {
        put(DecodeHintType.TRY_HARDER, true)
        put(DecodeHintType.CHARACTER_SET, Charsets.UTF_8.name())
    }

    /**
     * Decodes a camera frame's luminance plane. A QR reads the same at any
     * rotation, so the frame's orientation never has to be corrected first.
     */
    fun decodeLuminance(data: ByteArray, width: Int, height: Int, rowStride: Int): String? {
        if (width <= 0 || height <= 0 || rowStride < width) return null

        // A row stride wider than the image means the plane is padded; ZXing
        // needs a tightly packed buffer, so drop the padding first.
        val packed = if (rowStride == width) {
            data
        } else {
            if (data.size < rowStride * (height - 1) + width) return null
            ByteArray(width * height).also { out ->
                for (row in 0 until height) {
                    System.arraycopy(data, row * rowStride, out, row * width, width)
                }
            }
        }
        if (packed.size < width * height) return null

        val source = PlanarYUVLuminanceSource(packed, width, height, 0, 0, width, height, false)
        return decode(source.binarized())
    }

    /** Decodes a still the owner picked from their gallery. */
    fun decodeBitmap(bitmap: Bitmap): String? {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= 0 || height <= 0 || width.toLong() * height > MAX_STILL_PIXELS) return null
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return decode(RGBLuminanceSource(width, height, pixels).binarized())
    }

    private fun decode(binary: BinaryBitmap): String? = try {
        reader.decode(binary, hints).text
    } catch (_: NotFoundException) {
        null
    } catch (_: ChecksumException) {
        null
    } catch (_: FormatException) {
        null
    } finally {
        reader.reset()
    }
}
