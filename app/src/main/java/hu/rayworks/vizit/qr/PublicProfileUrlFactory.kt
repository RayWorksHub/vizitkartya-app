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

    fun belongsToBase(url: String, baseUrl: String): Boolean {
        val normalizedBase = baseUrl.trim().trimEnd('/')
        val expectedPrefix = "$normalizedBase/"
        if (!url.startsWith(expectedPrefix)) return false
        return isValidSlug(url.removePrefix(expectedPrefix))
    }
}
