package hu.rayworks.vizit.qr

import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.nfc.VCardBuilder

object QrPayloadFactory {
    const val MAX_CONTACT_QR_BYTES = 1_800

    fun profileUrl(profile: ContactProfile, synchronized: Boolean = false): String? {
        if (!synchronized || !profile.isPublic || profile.publicSlug.isBlank()) return null
        return PublicProfileUrlFactory.create(
            baseUrl = BuildConfig.PUBLIC_PROFILE_BASE_URL,
            slug = profile.publicSlug,
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
}
