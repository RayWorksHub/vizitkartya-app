package hu.rayworks.vizit.qr

import android.graphics.Bitmap

object QrCodeGenerator {
    fun create(payload: String, sizePx: Int = 1024): Bitmap {
        val rendered = QrCodeEncoder.encode(payload, sizePx)
        return Bitmap.createBitmap(
            rendered.pixels,
            rendered.size,
            rendered.size,
            Bitmap.Config.ARGB_8888,
        )
    }
}
