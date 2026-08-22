package hu.rayworks.vizit.data

data class ContactProfile(
    val fullName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",
    val linkedIn: String = "",
    val publicProfileSlug: String = "",
    val publicProfileUrl: String = "",
    val note: String = "",
    val photoBase64: String = "",
) {
    val resolvedDisplayName: String
        get() = fullName.trim().ifBlank {
            listOf(lastName, firstName)
                .map(String::trim)
                .filter(String::isNotBlank)
                .joinToString(" ")
        }

    val initials: String
        get() = resolvedDisplayName
            .trim()
            .split(Regex("\\s+"))
            .filter(String::isNotBlank)
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercase() }
            .joinToString("")
            .ifBlank { "V" }
}

object ContactProfileValidator {
    fun validate(profile: ContactProfile): String? = when {
        profile.resolvedDisplayName.isBlank() -> "Add meg a nevedet a névjegyben."
        profile.phone.isBlank() && profile.email.isBlank() ->
            "Legalább egy telefonszámot vagy e-mail-címet adj meg."

        profile.email.isNotBlank() && !profile.email.contains("@") ->
            "Az e-mail-cím formátuma nem megfelelő."

        profile.publicProfileSlug.isNotBlank() &&
            !hu.rayworks.vizit.qr.PublicProfileUrlFactory.isValidSlug(profile.publicProfileSlug) ->
            "A profilazonosító 3–50 kisbetűből, számból vagy kötőjelből állhat."

        else -> null
    }
}
