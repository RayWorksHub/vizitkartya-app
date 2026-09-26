package hu.rayworks.vizit.qr

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.provider.MediaStore
import androidx.core.content.FileProvider
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.nfc.VCardBuilder
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object QrShareHelper {
    fun shareContact(context: Context, profile: ContactProfile, profileUrl: String?) {
        val directory = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(directory, "vizit-${UUID.randomUUID()}.vcf")
        file.writeText(
            VCardBuilder.build(profile, includePhoto = true, vizitProfileUrl = profileUrl),
            Charsets.UTF_8,
        )
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/vcard"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Fényképes névjegy megosztása"))
    }

    fun share(context: Context, bitmap: Bitmap, fileName: String = "vizit-qr.png") {
        val directory = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(directory, fileName)
        FileOutputStream(file).use { output ->
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
        }
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "VIZIT QR-kód megosztása"))
    }

    fun saveToPictures(context: Context, bitmap: Bitmap): Boolean {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "VIZIT-${System.currentTimeMillis()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/VIZIT")
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            resolver.openOutputStream(uri)?.use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            } == true
        } catch (_: Exception) {
            resolver.delete(uri, null, null)
            false
        }
    }
}
