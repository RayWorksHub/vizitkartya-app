package hu.rayworks.vizit.qr

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF

object QrCodeGenerator {
    fun create(payload: String, logo: Bitmap? = null, sizePx: Int = 1024): Bitmap {
        val rendered = QrCodeEncoder.encode(payload, sizePx)
        val qr = Bitmap.createBitmap(
            rendered.pixels,
            rendered.size,
            rendered.size,
            Bitmap.Config.ARGB_8888,
        )
        if (logo == null) return qr

        val result = qr.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val plateSize = sizePx * LOGO_PLATE_RATIO
        val plateLeft = (sizePx - plateSize) / 2f
        val plateTop = (sizePx - plateSize) / 2f
        val plate = RectF(plateLeft, plateTop, plateLeft + plateSize, plateTop + plateSize)
        val platePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        canvas.drawRoundRect(plate, plateSize * 0.18f, plateSize * 0.18f, platePaint)

        val content = plateSize * 0.78f
        val scale = minOf(content / logo.width, content / logo.height)
        val width = logo.width * scale
        val height = logo.height * scale
        val destination = RectF(
            (sizePx - width) / 2f,
            (sizePx - height) / 2f,
            (sizePx + width) / 2f,
            (sizePx + height) / 2f,
        )
        canvas.drawBitmap(
            logo,
            null,
            destination,
            Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
        )
        return result
    }

    private const val LOGO_PLATE_RATIO = 0.14f
}
