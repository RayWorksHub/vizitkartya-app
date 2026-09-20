package hu.rayworks.vizit.qr

/**
 * Three codes, three jobs: the contact card without a photo, the same card with
 * one, and the public address. Only [CONTACT] is always available — [PHOTO]
 * needs a profile picture small enough to fit, [PROFILE] needs a synced public
 * profile.
 */
enum class QrMode {
    CONTACT,
    PHOTO,
    PROFILE,
}
