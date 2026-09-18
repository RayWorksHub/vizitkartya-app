package hu.rayworks.vizit.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.Base64

object RemoteContactPhoto {
    suspend fun load(value: String?, supabaseUrl: String): String = withContext(Dispatchers.IO) {
        if (value.isNullOrEmpty()) return@withContext ""
        val input = if (value.startsWith(LegacyProfileCodec.INLINE_PREFIX)) {
            require(value.length <= 350000)
            val text = value.removePrefix(LegacyProfileCodec.INLINE_PREFIX)
            Base64.getDecoder().decode(text).also {
                require(it.size <= 262144 && Base64.getEncoder().encodeToString(it) == text)
            }
        } else {
            val uri = URI(value); val origin = URI(supabaseUrl)
            require(uri.scheme == "https" && uri.host == origin.host && uri.port == origin.port && uri.userInfo == null && uri.query == null && uri.fragment == null)
            require(Regex("^/storage/v1/object/public/avatars/[a-fA-F0-9-]{36}/[a-zA-Z0-9_.-]+$").matches(uri.path) && !uri.path.contains(".."))
            val connection = uri.toURL().openConnection() as HttpURLConnection
            try {
                connection.instanceFollowRedirects = false
                connection.connectTimeout = 8000; connection.readTimeout = 8000
                require(connection.responseCode == 200 && connection.contentLengthLong <= 3 * 1024 * 1024)
                connection.inputStream.use { stream ->
                    val output = ByteArrayOutputStream(); val buffer = ByteArray(8192)
                    while (true) { val count = stream.read(buffer); if (count < 0) break
                        require(output.size() + count <= 3 * 1024 * 1024); output.write(buffer,0,count) }
                    output.toByteArray()
                }
            } finally { connection.disconnect() }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(input,0,input.size,bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0 && bounds.outWidth.toLong() * bounds.outHeight <= 16_000_000)
        require(bounds.outMimeType in listOf("image/jpeg","image/png","image/webp"))
        val options = BitmapFactory.Options().apply {
            inSampleSize = 1
            while (maxOf(bounds.outWidth,bounds.outHeight)/inSampleSize > 1024) inSampleSize *= 2
        }
        val decoded = requireNotNull(BitmapFactory.decodeByteArray(input,0,input.size,options))
        val ratio = minOf(1.0,512.0/maxOf(decoded.width,decoded.height))
        val scaled = Bitmap.createScaledBitmap(decoded,maxOf(1,(decoded.width*ratio).toInt()),maxOf(1,(decoded.height*ratio).toInt()),true)
        val flattened = Bitmap.createBitmap(scaled.width,scaled.height,Bitmap.Config.ARGB_8888)
        try {
            Canvas(flattened).apply { drawColor(Color.WHITE); drawBitmap(scaled,0f,0f,null) }
            // Preserve an already-valid small JPEG byte-for-byte for stable resynchronisation.
            if (bounds.outMimeType == "image/jpeg" && input.size <= 262144) return@withContext Base64.getEncoder().encodeToString(input)
            val output = ByteArrayOutputStream(); check(flattened.compress(Bitmap.CompressFormat.JPEG,82,output))
            require(output.size() <= 262144)
            Base64.getEncoder().encodeToString(output.toByteArray())
        } finally { listOf(decoded,scaled,flattened).distinct().forEach(Bitmap::recycle) }
    }
}
