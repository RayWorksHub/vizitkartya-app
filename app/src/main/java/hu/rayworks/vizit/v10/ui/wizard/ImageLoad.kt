package hu.rayworks.vizit.v10.ui.wizard

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.ui.graphics.asImageBitmap
import hu.rayworks.vizit.v10.data.Pic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.roundToInt

sealed interface ImageResult {
    data class Ok(val pic: Pic) : ImageResult
    data class Err(val message: String) : ImageResult
}

private const val NOT_IMAGE = "Ez nem képfájl. Válassz JPG, PNG vagy WebP képet."
private const val TOO_BIG = "A kép nagyobb 25 MB-nál. Válassz kisebbet."
private const val CANT_OPEN = "Ezt a képformátumot nem tudjuk megnyitni. Válassz JPG, PNG vagy WebP képet."

/**
 * readImage() a prototípusból: ellenőrzi a típust és a 25 MB-os korlátot, majd a hosszabb oldalt
 * [maxSide] képpontra kicsinyíti (profilkép 640, logó 320), a telefonos fotók elforgatását is kezelve.
 */
suspend fun loadImage(context: Context, uri: Uri, maxSide: Int): ImageResult = withContext(Dispatchers.IO) {
    val cr = context.contentResolver
    val mime = cr.getType(uri)
    if (mime != null && !mime.startsWith("image/")) return@withContext ImageResult.Err(NOT_IMAGE)

    val size = runCatching {
        cr.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst() && !c.isNull(0)) c.getLong(0) else null
        }
    }.getOrNull()
    if (size != null && size > 25L * 1024 * 1024) return@withContext ImageResult.Err(TOO_BIG)

    try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@withContext ImageResult.Err(CANT_OPEN)

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return@withContext ImageResult.Err(CANT_OPEN)

        val rotation = runCatching {
            cr.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees() } ?: 0
        }.getOrDefault(0)
        if (rotation != 0) {
            val m = Matrix().apply { postRotate(rotation.toFloat()) }
            bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        }

        val s = minOf(1f, maxSide.toFloat() / max(bmp.width, bmp.height))
        if (s < 1f) {
            bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * s).roundToInt(), (bmp.height * s).roundToInt(), true)
        }
        ImageResult.Ok(Pic.Bmp(bmp.asImageBitmap()))
    } catch (e: Exception) {
        ImageResult.Err(CANT_OPEN)
    } catch (e: OutOfMemoryError) {
        ImageResult.Err(TOO_BIG)
    }
}

private fun ExifInterface.rotationDegrees(): Int = when (getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
    ExifInterface.ORIENTATION_ROTATE_90 -> 90
    ExifInterface.ORIENTATION_ROTATE_180 -> 180
    ExifInterface.ORIENTATION_ROTATE_270 -> 270
    else -> 0
}
