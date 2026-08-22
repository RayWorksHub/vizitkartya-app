package hu.rayworks.vizit.qr

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.util.EnumMap

data class RenderedQrCode(
    val size: Int,
    val pixels: IntArray,
) {
    init {
        require(pixels.size == size * size)
    }
}

object QrCodeEncoder {
    const val DEFAULT_SIZE = 1_024
    const val QUIET_ZONE_MODULES = 4

    fun encode(content: String, size: Int = DEFAULT_SIZE): RenderedQrCode {
        require(content.isNotBlank()) { "Üres adatból nem készíthető QR-kód." }
        require(size in 256..2_048) { "A QR-kép mérete 256 és 2048 pixel közötti lehet." }

        val hints = EnumMap<EncodeHintType, Any>(EncodeHintType::class.java).apply {
            put(EncodeHintType.CHARACTER_SET, Charsets.UTF_8.name())
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, QUIET_ZONE_MODULES)
        }
        val matrix = MultiFormatWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints,
        )
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            val rowOffset = y * size
            for (x in 0 until size) {
                pixels[rowOffset + x] = if (matrix[x, y]) BLACK else WHITE
            }
        }
        return RenderedQrCode(size = size, pixels = pixels)
    }

    private const val BLACK: Int = -0x1000000
    private const val WHITE: Int = -0x1
}
