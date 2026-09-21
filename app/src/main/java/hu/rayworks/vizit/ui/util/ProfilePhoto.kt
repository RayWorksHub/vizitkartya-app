package hu.rayworks.vizit.ui.util

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Decodes the stored base64 profile photo once per value change.
 * Returns null for an empty or malformed payload so callers fall back to initials.
 */
@Composable
fun rememberProfilePhoto(photoBase64: String): ImageBitmap? = remember(photoBase64) {
    if (photoBase64.isBlank()) {
        null
    } else {
        runCatching {
            val bytes = Base64.decode(photoBase64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        }.getOrNull()
    }
}
