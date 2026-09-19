package hu.rayworks.vizit.qr

import java.net.URI
import java.text.Normalizer
import java.util.Locale

object PublicProfileUrlFactory {
    private val slugPattern = Regex("^[a-z0-9][a-z0-9-]{1,48}[a-z0-9]$")
    private val domainPattern = Regex(
        "^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?(?:\\.[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?)+$",
    )

    fun normalizeSlug(value: String): String = value.trim().lowercase()

    fun isValidSlug(value: String): Boolean = slugPattern.matches(normalizeSlug(value))

    fun automaticSlug(displayName: String, ownerId: String): String {
        val readable = Normalizer.normalize(displayName.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .take(50)
            .trimEnd('-')
        if (isValidSlug(readable)) return readable

        val ownerToken = ownerId.lowercase(Locale.ROOT).replace(Regex("[^a-z0-9]"), "")
            .ifBlank { "profil" }
        return "vizit-${ownerToken.take(32)}"
    }

    fun normalizeCustomDomain(value: String): String = value
        .trim()
        .lowercase(Locale.ROOT)
        .removePrefix("https://")
        .trimEnd('/')
        .trimEnd('.')

    fun isValidCustomDomain(value: String): Boolean {
        val host = normalizeCustomDomain(value)
        if (host.length !in 4..253 || !domainPattern.matches(host)) return false
        return runCatching {
            val uri = URI("https://$host")
            uri.host == host && uri.userInfo == null && uri.path.isEmpty() &&
                uri.query == null && uri.fragment == null
        }.getOrDefault(false)
    }

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

    fun createPreferred(
        baseUrl: String,
        slug: String,
        customDomain: String,
        customDomainVerified: Boolean,
    ): Result<String> = if (customDomainVerified && isValidCustomDomain(customDomain)) {
        Result.success("https://${normalizeCustomDomain(customDomain)}")
    } else {
        create(baseUrl = baseUrl, slug = slug)
    }

}
