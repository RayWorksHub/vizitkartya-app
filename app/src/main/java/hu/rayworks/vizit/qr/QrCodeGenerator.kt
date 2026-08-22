package hu.rayworks.vizit.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import java.nio.charset.StandardCharsets

object QrCodeGenerator {
    fun create(payload: String, sizePx: Int = 1024): Bitmap {
        require(payload.isNotBlank())
        require(sizePx >= 256)

        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to StandardCharsets.UTF_8.name(),
            EncodeHintType.MARGIN to 4,
        )
        val matrix = QRCodeWriter().encode(
            payload,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            hints,
        )
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val rowOffset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[rowOffset + x] = if (matrix[x, y]) Color.BLACK else Color.WHITE
            }
        }
        return Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }
}
