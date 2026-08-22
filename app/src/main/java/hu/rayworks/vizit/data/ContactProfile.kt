package hu.rayworks.vizit.data

data class ContactProfile(
    val fullName: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",
    val linkedIn: String = "",
    val photoBase64: String = "",
    val publicSlug: String = "",
    val isPublic: Boolean = false,
) {
    val initials: String
        get() = fullName
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
        profile.fullName.isBlank() -> "Add meg a nevedet a névjegyben."
        profile.phone.isBlank() && profile.email.isBlank() ->
            "Legalább egy telefonszámot vagy e-mail-címet adj meg."

        profile.email.isNotBlank() && !profile.email.contains("@") ->
            "Az e-mail-cím formátuma nem megfelelő."

        else -> null
    }
}
