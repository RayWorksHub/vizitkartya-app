package hu.rayworks.vizit.qr

import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.nfc.VCardBuilder

object QrPayloadFactory {
    const val MAX_CONTACT_QR_BYTES = 1_800

    fun profileUrl(profile: ContactProfile): String? {
        if (!profile.isPublic || profile.publicSlug.isBlank()) return null
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

        val candidates = sequenceOf(
            profile,
            profile.copy(address = "", linkedIn = ""),
            profile.copy(address = "", linkedIn = "", website = "", jobTitle = "", company = ""),
        )

        candidates
            .map { candidate ->
                VCardBuilder.build(
                    profile = candidate,
                    includePhoto = false,
                    vizitProfileUrl = profileUrl,
                )
            }
            .firstOrNull { value ->
                value.toByteArray(Charsets.UTF_8).size <= MAX_CONTACT_QR_BYTES
            }
            ?: error("A kontaktadat túl hosszú egy megbízhatóan olvasható QR-kódhoz.")
    }
}
