package hu.rayworks.vizit.nfc

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.core.graphics.scale
import hu.rayworks.vizit.data.ContactProfile
import java.io.ByteArrayOutputStream

data class NfcSharePayload(
    val ndefMessage: ByteArray,
    val vCard: String,
    val embeddedPhotoBytes: Int,
)

object NfcPayloadFactory {
    const val PRACTICAL_MAX_NDEF_BYTES = 16_384

    private val imageDimensions = listOf(192, 160, 128, 112, 96, 80, 64)
    private val jpegQualities = listOf(72, 62, 52, 42, 34)

    fun create(
        profile: ContactProfile,
        maxNdefBytes: Int = PRACTICAL_MAX_NDEF_BYTES,
    ): Result<NfcSharePayload> = runCatching {
        require(maxNdefBytes in 256..Type4TagApduProcessor.MAX_NDEF_SIZE)

        val withoutPhoto = encode(profile, embeddedPhotoBase64 = "", embeddedPhotoBytes = 0)
        require(withoutPhoto.ndefMessage.size <= maxNdefBytes) {
            "A névjegy szöveges adatai nem férnek el az NFC payloadban."
        }

        if (profile.photoBase64.isBlank()) return@runCatching withoutPhoto

        findPayloadWithOptimizedPhoto(profile, maxNdefBytes) ?: withoutPhoto
    }

    private fun encode(
        profile: ContactProfile,
        embeddedPhotoBase64: String,
        embeddedPhotoBytes: Int,
    ): NfcSharePayload {
        val vCard = VCardBuilder.build(profile, embeddedPhotoBase64)
        return NfcSharePayload(
            ndefMessage = NdefVCardEncoder.encodeVCard(vCard),
            vCard = vCard,
            embeddedPhotoBytes = embeddedPhotoBytes,
        )
    }

    private fun findPayloadWithOptimizedPhoto(
        profile: ContactProfile,
        maxNdefBytes: Int,
    ): NfcSharePayload? {
        val sourceBytes = Base64.decode(profile.photoBase64, Base64.DEFAULT)
        val source = BitmapFactory.decodeByteArray(sourceBytes, 0, sourceBytes.size)
            ?: return null

        try {
            for (dimension in imageDimensions) {
                val scaled = if (source.width == dimension && source.height == dimension) {
                    source
                } else {
                    source.scale(dimension, dimension)
                }

                try {
                    for (quality in jpegQualities) {
                        val bytes = ByteArrayOutputStream().use { output ->
                            scaled.compress(Bitmap.CompressFormat.JPEG, quality, output)
                            output.toByteArray()
                        }
                        val payload = encode(
                            profile = profile,
                            embeddedPhotoBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP),
                            embeddedPhotoBytes = bytes.size,
                        )
                        if (payload.ndefMessage.size <= maxNdefBytes) return payload
                    }
                } finally {
                    if (scaled !== source) scaled.recycle()
                }
            }
        } finally {
            source.recycle()
        }
        return null
    }
}
