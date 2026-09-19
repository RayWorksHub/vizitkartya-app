package hu.rayworks.vizit.qr

import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.PhotoProcessor
import hu.rayworks.vizit.nfc.VCardBuilder

object QrPayloadFactory {
    const val MAX_CONTACT_QR_BYTES = 2_200

    fun profileUrl(profile: ContactProfile, synchronized: Boolean = false): String? {
        if (!synchronized || !profile.isPublic || profile.publicSlug.isBlank()) return null
        return PublicProfileUrlFactory.createPreferred(
            baseUrl = BuildConfig.PUBLIC_PROFILE_BASE_URL,
            slug = profile.publicSlug,
            customDomain = profile.customDomain,
            customDomainVerified = profile.customDomainVerified,
        ).getOrNull()
    }

    fun contact(
        profile: ContactProfile,
        profileUrl: String? = profileUrl(profile),
    ): Result<String> = runCatching {
        require(profile.resolvedDisplayName.isNotBlank()) {
            "A Kontakt QR-hez add meg a nevedet."
        }
        require(profile.phone.isNotBlank() || profile.email.isNotBlank()) {
            "A Kontakt QR-hez adj meg telefonszámot vagy e-mail-címet."
        }

        val value = VCardBuilder.build(profile = profile, includePhoto = false, vizitProfileUrl = profileUrl)
        require(value.toByteArray(Charsets.UTF_8).size <= MAX_CONTACT_QR_BYTES) {
            "A névjegy túl hosszú az offline QR-hoz. Használd a szinkronizált Profil QR-t; adatot nem hagytunk ki."
        }
        value
    }

    fun photoContact(
        profile: ContactProfile,
        profileUrl: String? = profileUrl(profile),
    ): Result<String> = runCatching {
        require(profile.photoBase64.isNotBlank()) { "A fényképes QR-hez adj meg profilképet." }

        val noPhoto = contact(profile, profileUrl).getOrThrow()
        var maxJpegBytes = (((MAX_CONTACT_QR_BYTES - noPhoto.toByteArray(Charsets.UTF_8).size -
            PHOTO_LINE_RESERVE) * 3) / 4).coerceAtMost(MAX_QR_PHOTO_JPEG_BYTES)

        while (maxJpegBytes >= MIN_QR_PHOTO_JPEG_BYTES) {
            val optimized = PhotoProcessor.optimizeContactPhotoBase64(
                photoBase64 = profile.photoBase64,
                maxJpegBytes = maxJpegBytes,
            )
            if (optimized.isNotBlank()) {
                photoContactFromOptimized(profile, optimized, profileUrl).getOrNull()?.let {
                    return@runCatching it
                }
            }
            maxJpegBytes = (maxJpegBytes * 0.78f).toInt()
        }
        error("A profilkép nem fér bele megbízhatóan a Kontakt QR-ba.")
    }

    internal fun photoContactFromOptimized(
        profile: ContactProfile,
        optimizedPhotoBase64: String,
        profileUrl: String? = null,
    ): Result<String> = runCatching {
        require(optimizedPhotoBase64.isNotBlank()) { "Hiányzik az optimalizált profilkép." }
        val value = VCardBuilder.build(
            profile = profile.copy(photoBase64 = optimizedPhotoBase64),
            includePhoto = true,
            vizitProfileUrl = profileUrl,
        )
        require(value.startsWith("BEGIN:VCARD\r\n"))
        require(value.contains("PHOTO;ENCODING=b;TYPE=JPEG:"))
        require(value.toByteArray(Charsets.UTF_8).size <= MAX_CONTACT_QR_BYTES) {
            "A profilképes névjegy túl hosszú a QR-kódhoz."
        }
        value
    }

    private const val PHOTO_LINE_RESERVE = 140
    private const val MAX_QR_PHOTO_JPEG_BYTES = 1_500
    private const val MIN_QR_PHOTO_JPEG_BYTES = 420
}
