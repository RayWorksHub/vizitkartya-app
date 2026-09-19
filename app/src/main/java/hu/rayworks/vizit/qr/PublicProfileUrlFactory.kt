package hu.rayworks.vizit.qr

import java.net.URI

object PublicProfileUrlFactory {
    private val slugPattern = Regex("^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$")

    fun normalizeSlug(value: String): String = value.trim().lowercase()

    fun isValidSlug(value: String): Boolean = slugPattern.matches(normalizeSlug(value))

    fun create(baseUrl: String, slug: String): Result<String> = runCatching {
        val normalizedSlug = normalizeSlug(slug)
        require(isValidSlug(normalizedSlug)) { "A profilazonosító formátuma nem megfelelő." }

        val normalizedBase = baseUrl.trim().trimEnd('/')
        val baseUri = URI(normalizedBase)
        require(baseUri.scheme.equals("https", ignoreCase = true)) {
            "A publikus profil URL-jének HTTPS-címnek kell lennie."
        }
        require(!baseUri.host.isNullOrBlank() && baseUri.userInfo == null) {
            "A publikus profil alap URL-je nem érvényes."
        }
        require(baseUri.query == null && baseUri.fragment == null) {
            "A publikus profil alap URL-je nem tartalmazhat queryt vagy fragmentet."
        }

        "$normalizedBase/$normalizedSlug"
    }

    fun createVCardUrl(profileUrl: String): Result<String> = runCatching {
        val normalizedProfileUrl = profileUrl.trim().trimEnd('/')
        val profileUri = URI(normalizedProfileUrl)
        require(profileUri.scheme.equals("https", ignoreCase = true)) {
            "A kontaktfájl URL-jének HTTPS-címnek kell lennie."
        }
        require(!profileUri.host.isNullOrBlank() && profileUri.userInfo == null) {
            "A kontaktfájl alap URL-je nem érvényes."
        }
        require(profileUri.query == null && profileUri.fragment == null) {
            "A kontaktfájl alap URL-je nem tartalmazhat queryt vagy fragmentet."
        }

        "$normalizedProfileUrl/vcard"
    }
}
