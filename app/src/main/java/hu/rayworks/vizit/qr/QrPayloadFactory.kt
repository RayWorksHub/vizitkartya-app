package hu.rayworks.vizit.qr

import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.nfc.VCardBuilder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

object QrPayloadFactory {
    fun profileUrl(profile: ContactProfile): String? {
        if (!profile.isPublic || profile.publicSlug.isBlank()) return null
        val encodedSlug = URLEncoder.encode(profile.publicSlug.trim(), StandardCharsets.UTF_8.toString())
            .replace("+", "%20")
        return "${BuildConfig.PUBLIC_PROFILE_BASE_URL}/$encodedSlug"
    }

    fun contact(profile: ContactProfile, profileUrl: String? = profileUrl(profile)): String =
        VCardBuilder.build(
            profile = profile,
            includePhoto = false,
            vizitProfileUrl = profileUrl,
        )
}
