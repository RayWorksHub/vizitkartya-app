package hu.rayworks.vizit.qr

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.nfc.VCardBuilder

enum class QrMode {
    PROFILE,
    CONTACT,
}

data class QrPayload(
    val mode: QrMode,
    val content: String,
) {
    val byteCount: Int
        get() = content.toByteArray(Charsets.UTF_8).size
}

object QrPayloadFactory {
    const val MAX_CONTACT_QR_BYTES = 1_800

    fun createProfile(profile: ContactProfile, baseUrl: String): Result<QrPayload> =
        PublicProfileUrlFactory.create(baseUrl, profile.publicProfileSlug)
            .map { url -> QrPayload(mode = QrMode.PROFILE, content = url) }

    fun createContact(profile: ContactProfile): Result<QrPayload> = runCatching {
        require(profile.resolvedDisplayName.isNotBlank()) {
            "A Kontakt QR-hez add meg a nevedet."
        }
        require(profile.phone.isNotBlank() || profile.email.isNotBlank()) {
            "A Kontakt QR-hez adj meg telefonszámot vagy e-mail-címet."
        }

        val candidates = sequenceOf(
            profile,
            profile.copy(note = "", address = "", linkedIn = ""),
            ContactProfile(
                fullName = profile.fullName,
                firstName = profile.firstName,
                lastName = profile.lastName,
                phone = profile.phone,
                email = profile.email,
                publicProfileSlug = profile.publicProfileSlug,
                publicProfileUrl = profile.publicProfileUrl,
            ),
        )

        val vCard = candidates
            .map { candidate -> VCardBuilder.build(candidate, embeddedPhotoBase64 = "") }
            .firstOrNull { value -> value.toByteArray(Charsets.UTF_8).size <= MAX_CONTACT_QR_BYTES }
            ?: error("A kontaktadat túl hosszú egy megbízhatóan olvasható QR-kódhoz.")

        QrPayload(mode = QrMode.CONTACT, content = vCard)
    }
}
