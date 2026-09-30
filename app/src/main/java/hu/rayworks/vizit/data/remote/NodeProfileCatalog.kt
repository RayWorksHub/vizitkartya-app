package hu.rayworks.vizit.data.remote

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.qr.PublicProfileUrlFactory
import io.github.jan.supabase.SupabaseClient
import java.util.UUID
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

data class AccountProfile(
    val id: String,
    val displayName: String,
    val slug: String,
    val isDefault: Boolean,
)

/** Account-level profile operations. Profile content sync stays in the Room outbox. */
class NodeProfileCatalog(private val client: SupabaseClient?) {
    private val api = NodeBackendApi(client)

    suspend fun list(): List<AccountProfile> {
        val response = api.request("GET", "/api/profiles")
        return response["profiles"]?.jsonArray.orEmpty().map { it.jsonObject.toAccountProfile() }
    }

    suspend fun makeDefault(profileId: String): AccountProfile {
        require(UUID.fromString(profileId).toString().equals(profileId, ignoreCase = true))
        val response = api.request("POST", "/api/profiles/$profileId/default")
        return requireNotNull(response["profile"]?.jsonObject).toAccountProfile()
    }

    suspend fun delete(profileId: String) {
        require(UUID.fromString(profileId).toString().equals(profileId, ignoreCase = true))
        api.request("DELETE", "/api/profiles/$profileId")
    }

    suspend fun create(profile: ContactProfile): AccountProfile {
        profile.validateForCatalog()
        val ownerId = api.userId() ?: throw NodeBackendException(
            401,
            "session_unavailable",
            "Jelentkezz be újra.",
        )
        val requestedLogo = profile.logoBase64.takeIf(String::isNotBlank)
            ?.let { LegacyProfileCodec.INLINE_PREFIX + it }
        val appearance = requestedLogo?.let {
            RemoteProfileLogo.prepare(
                client = requireNotNull(client),
                owner = ownerId,
                appearance = null,
                remoteLogoBase64 = "",
                requested = it,
                theme = "midnight",
            )
        }
        val requestedSlug = profile.publicSlug.trim().lowercase()
            .takeIf(PublicProfileUrlFactory::isValidSlug)
            ?: PublicProfileUrlFactory.automaticSlug(profile.resolvedDisplayName, ownerId)

        suspend fun send(slug: String): AccountProfile {
            val response = api.request("POST", "/api/profiles", profile.body(slug, appearance))
            return requireNotNull(response["profile"]?.jsonObject).toAccountProfile()
        }

        return try {
            send(requestedSlug)
        } catch (error: NodeBackendException) {
            if (error.status != 409 || error.code == "MULTI_PROFILE_DISABLED") throw error
            val suffix = UUID.randomUUID().toString().take(6)
            val base = requestedSlug.take(43).trimEnd('-').ifBlank { "vizit" }
            send("$base-$suffix")
        }
    }

    private fun ContactProfile.body(slug: String, appearance: JsonObject?): JsonObject = buildJsonObject {
        put("slug", slug)
        put("display_name", resolvedDisplayName.trim())
        put("job_title", jobTitle.trim())
        put("company", company.trim())
        put("bio", bio.trim())
        put("public_email", email.trim())
        put("phone", phone.trim())
        put("website", website.trim())
        put("address", address.trim())
        put("avatar_url", photoBase64.takeIf(String::isNotBlank)
            ?.let { JsonPrimitive(LegacyProfileCodec.INLINE_PREFIX + it) } ?: JsonNull)
        put("appearance", appearance ?: JsonNull)
        put("theme", "midnight")
        put("accent_color", "#0b5ce8")
        put("is_public", isPublic)
        put("custom_domain", PublicProfileUrlFactory.normalizeCustomDomain(customDomain))
        put("make_default", true)
        put("social_links", socialLinks())
    }

    private fun ContactProfile.socialLinks(): JsonArray = buildJsonArray {
        val values = listOf(
            Triple("linkedin", "LinkedIn", linkedIn),
            Triple("facebook", "Facebook", facebook),
            Triple("instagram", "Instagram", instagram),
            Triple("youtube", "YouTube", youtube),
            Triple("tiktok", "TikTok", tiktok),
            Triple("x", "X", x),
            Triple("github", "GitHub", github),
            Triple("custom", "Egyéb", customSocial),
        ).filter { it.third.isNotBlank() }
        values.forEachIndexed { index, (platform, label, url) ->
            add(buildJsonObject {
                put("platform", platform)
                put("label", label)
                put("url", url.trim())
                put("sort_order", index)
                put("enabled", true)
            })
        }
    }

    private fun ContactProfile.validateForCatalog() {
        val error = hu.rayworks.vizit.data.ContactProfileValidator.validate(this)
        require(error == null) { error ?: "A profil nem menthető." }
    }

    private fun JsonObject.toAccountProfile(): AccountProfile = AccountProfile(
        id = requireNotNull(this["id"]?.jsonPrimitive?.contentOrNull),
        displayName = requireNotNull(this["display_name"]?.jsonPrimitive?.contentOrNull),
        slug = requireNotNull(this["slug"]?.jsonPrimitive?.contentOrNull),
        isDefault = this["is_default"]?.jsonPrimitive?.booleanOrNull == true,
    )
}
