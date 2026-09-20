package hu.rayworks.vizit.data.card

import hu.rayworks.vizit.data.ContactProfile

/**
 * How the card looks and which of the owner's data it carries.
 *
 * This is a presentation preference, not contact data: it lives on the device
 * next to the profile rather than inside it, so restyling a card here never
 * rewrites the synchronised profile record. The iOS `CardPresentation` carries
 * the same fields with the same defaults.
 */
data class CardPresentation(
    val colorway: CardColorway = CardColorway.INK,
    val layout: CardLayout = CardLayout.PORTRAIT,
    // Which elements the card surface itself shows.
    val showsPhoto: Boolean = true,
    val showsQr: Boolean = false,
    val showsSocial: Boolean = false,
    // Which fields leave the device at all — on the shared card, in the QR and
    // in the vCard.
    val sharesCompany: Boolean = true,
    val sharesEmail: Boolean = true,
    val sharesPhone: Boolean = true,
    val sharesWebsite: Boolean = true,
    val sharesSocial: Boolean = true,
    val sharesAddress: Boolean = true,
) {
    /** The number of optional fields currently shared, for the Settings row. */
    val sharedFieldCount: Int
        get() = listOf(
            sharesCompany, sharesEmail, sharesPhone,
            sharesWebsite, sharesSocial, sharesAddress,
        ).count { it }

    companion object {
        const val OPTIONAL_FIELD_COUNT = 6
    }
}

enum class CardColorway(
    val label: String,
    val gradientStart: Long,
    val gradientMid: Long,
    val gradientEnd: Long,
    val accent: Long,
) {
    // Every variant stays dark enough for white type to clear 4.5:1.
    INK("Tinta", 0xFF0C2C63, 0xFF071F4C, 0xFF05163A, 0xFF0FBEE6),
    BRAND("Márkakék", 0xFF1668F0, 0xFF0B5CE8, 0xFF0742A8, 0xFF7FD9FF),
    EMERALD("Smaragd", 0xFF0E6B4A, 0xFF0A5138, 0xFF063526, 0xFF4FE0A8),
    AMETHYST("Ametiszt", 0xFF5B2E9E, 0xFF452278, 0xFF2C1550, 0xFFC9A6FF),
    COPPER("Réz", 0xFF9A4A18, 0xFF7A3A12, 0xFF50250B, 0xFFFFBE7A),
    GRAPHITE("Grafit", 0xFF2B3240, 0xFF1D222D, 0xFF12161E, 0xFF8FD8F0),
}

enum class CardLayout(val label: String) {
    PORTRAIT("Álló"),
    LANDSCAPE("Fekvő"),
}

/**
 * The profile as the recipient sees it: everything the owner switched off in
 * Adatláthatóság is removed before the card, the QR or the vCard is built.
 */
fun ContactProfile.visibleThrough(presentation: CardPresentation): ContactProfile = copy(
    company = if (presentation.sharesCompany) company else "",
    jobTitle = if (presentation.sharesCompany) jobTitle else "",
    email = if (presentation.sharesEmail) email else "",
    phone = if (presentation.sharesPhone) phone else "",
    website = if (presentation.sharesWebsite) website else "",
    address = if (presentation.sharesAddress) address else "",
    linkedIn = if (presentation.sharesSocial) linkedIn else "",
    facebook = if (presentation.sharesSocial) facebook else "",
    instagram = if (presentation.sharesSocial) instagram else "",
    tiktok = if (presentation.sharesSocial) tiktok else "",
    youtube = if (presentation.sharesSocial) youtube else "",
)
