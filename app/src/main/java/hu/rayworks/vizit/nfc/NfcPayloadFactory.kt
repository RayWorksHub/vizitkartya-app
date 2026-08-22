package hu.rayworks.vizit.nfc

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.PhotoProcessor

object NfcPayloadFactory {
    private const val TARGET_NDEF_BYTES = 24_000
    private const val HARD_MAX_NDEF_BYTES = 0x7FFF
    private const val PHOTO_OVERHEAD_RESERVE = 700
    private const val MIN_PHOTO_JPEG_BYTES = 1_200

    data class Result(
        val bytes: ByteArray,
        val photoIncluded: Boolean,
        val byteSize: Int,
    )

    fun create(profile: ContactProfile, fallbackUrl: String?): Result {
        val noPhotoCard = VCardBuilder.build(
            profile = profile,
            includePhoto = false,
            vizitProfileUrl = fallbackUrl,
        )
        val noPhotoPayload = NdefVCardEncoder.encode(noPhotoCard, fallbackUrl)
        require(noPhotoPayload.size <= HARD_MAX_NDEF_BYTES) {
            "A névjegy szöveges NFC payloadja túl nagy."
        }

        if (profile.photoBase64.isBlank()) {
            return Result(noPhotoPayload, photoIncluded = false, byteSize = noPhotoPayload.size)
        }

        val originalPayload = encodeWithPhoto(profile, profile.photoBase64, fallbackUrl)
        if (originalPayload.size <= TARGET_NDEF_BYTES) {
            return Result(originalPayload, photoIncluded = true, byteSize = originalPayload.size)
        }

        var maxJpegBytes = (((TARGET_NDEF_BYTES - noPhotoPayload.size - PHOTO_OVERHEAD_RESERVE) * 3) / 4)
            .coerceAtMost(14_000)

        while (maxJpegBytes >= MIN_PHOTO_JPEG_BYTES) {
            val optimizedPhoto = PhotoProcessor.optimizeContactPhotoBase64(
                photoBase64 = profile.photoBase64,
                maxJpegBytes = maxJpegBytes,
            )
            if (optimizedPhoto.isNotBlank()) {
                val optimizedPayload = encodeWithPhoto(profile, optimizedPhoto, fallbackUrl)
                if (optimizedPayload.size <= TARGET_NDEF_BYTES) {
                    return Result(
                        bytes = optimizedPayload,
                        photoIncluded = true,
                        byteSize = optimizedPayload.size,
                    )
                }
            }
            maxJpegBytes = (maxJpegBytes * 0.72f).toInt()
        }

        return Result(noPhotoPayload, photoIncluded = false, byteSize = noPhotoPayload.size)
    }

    private fun encodeWithPhoto(
        profile: ContactProfile,
        photoBase64: String,
        fallbackUrl: String?,
    ): ByteArray {
        val vCard = VCardBuilder.build(
            profile = profile.copy(photoBase64 = photoBase64),
            includePhoto = true,
            vizitProfileUrl = fallbackUrl,
        )
        return NdefVCardEncoder.encode(vCard, fallbackUrl)
    }
}
