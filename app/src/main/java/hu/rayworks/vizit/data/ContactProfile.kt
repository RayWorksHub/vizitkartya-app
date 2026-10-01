package hu.rayworks.vizit.data

import hu.rayworks.vizit.qr.PublicProfileUrlFactory
import java.net.URI
import kotlinx.serialization.Serializable

@Serializable
data class ContactProfile(
    val fullName: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val jobTitle: String = "",
    val company: String = "",
    val bio: String = "",
    val phone: String = "",
    val email: String = "",
    val website: String = "",
    val address: String = "",
    val linkedIn: String = "",
    val facebook: String = "",
    val instagram: String = "",
    val tiktok: String = "",
    val youtube: String = "",
    val x: String = "",
    val github: String = "",
    val customSocial: String = "",
    val photoBase64: String = "",
    val logoBase64: String = "",
    val publicSlug: String = "",
    val isPublic: Boolean = false,
    val customDomain: String = "",
    val customDomainVerified: Boolean = false,
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
        profile.bio.length > 420 -> "A bemutatkozás legfeljebb 420 karakter lehet."
        profile.email.isNotBlank() && !profile.email.contains("@") ->
            "Az e-mail-cím formátuma nem megfelelő."

        profile.socialAndWebsiteUrls().any { !isValidHttpsUrl(it) } ->
            "A webes és közösségi hivatkozások teljes, https:// kezdetű címek legyenek."

        profile.publicSlug.isNotBlank() && !PublicProfileUrlFactory.isValidSlug(profile.publicSlug) ->
            "A profilazonosító 3–50 kisbetűből, számból vagy kötőjelből állhat."

        profile.customDomain.isNotBlank() && !PublicProfileUrlFactory.isValidCustomDomain(profile.customDomain) ->
            "Az egyedi domain csak egy teljes domainnév lehet, például nevjegy.cegem.hu."

        else -> null
    }

    private fun ContactProfile.socialAndWebsiteUrls(): List<String> = listOf(
        website,
        linkedIn,
        facebook,
        instagram,
        tiktok,
        youtube,
        x,
        github,
        customSocial,
    ).map(String::trim).filter(String::isNotBlank)

    private fun isValidHttpsUrl(value: String): Boolean = runCatching {
        URI(value).let { uri ->
            uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null
        }
    }.getOrDefault(false)
}
