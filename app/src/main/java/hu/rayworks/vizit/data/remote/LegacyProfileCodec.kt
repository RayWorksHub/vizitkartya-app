package hu.rayworks.vizit.data.remote

import hu.rayworks.vizit.data.sync.*
import hu.rayworks.vizit.qr.PublicProfileUrlFactory
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

/** The existing web/iOS table contract; no production schema mutation is required. */
@Serializable
data class LegacyProfileRecord(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val slug: String,
    @SerialName("display_name") val displayName: String,
    @SerialName("job_title") val jobTitle: String = "",
    val company: String = "",
    val bio: String = "",
    @SerialName("public_email") val publicEmail: String = "",
    val phone: String = "",
    val website: String = "",
    val address: String = "",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("custom_domain") val customDomain: String? = null,
    @SerialName("custom_domain_verified") val customDomainVerified: Boolean = false,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("social_links") val socialLinks: List<LegacySocialLink> = emptyList(),
) {
    val version: Long get() = Instant.parse(updatedAt).let { it.epochSecond * 1_000_000 + it.nano / 1_000 }
    fun payload(photo: String? = null): ProfileSyncPayload = ProfileSyncPayload(
        displayName = displayName, company = company, jobTitle = jobTitle, bio = bio,
        displayImagePath = avatarUrl.orEmpty(), publicSlug = slug, isPublic = isPublic,
        customDomain = customDomain, customDomainVerified = customDomainVerified,
        photoBase64 = photo,
        contacts = listOfNotNull(
            phone.takeIf(String::isNotBlank)?.let { ProfileContactPayload(stableId(ownerId,"phone"),"phone","Telefon",it,0,true) },
            publicEmail.takeIf(String::isNotBlank)?.let { ProfileContactPayload(stableId(ownerId,"email"),"email","E-mail",it,1,true) },
        ),
        addresses = address.takeIf(String::isNotBlank)?.let { listOf(ProfileAddressPayload(stableId(ownerId,"address"),"Cím",it,0,true)) }.orEmpty(),
        links = listOfNotNull(website.takeIf(String::isNotBlank)?.let {
            ProfileLinkPayload(stableId(ownerId,"website"),"website","Weboldal",it,0,true)
        }) + socialLinks.sortedBy { it.sortOrder }.map {
            ProfileLinkPayload(it.id,it.platform,it.label,it.url,it.sortOrder,it.enabled)
        },
    )
}

@Serializable
data class LegacySocialLink(
    val id: String,
    val platform: String,
    val label: String,
    val url: String,
    @SerialName("sort_order") val sortOrder: Int = 0,
    val enabled: Boolean = true,
)

internal fun stableId(owner: String, kind: String): String = UUID.nameUUIDFromBytes("vizit:$owner:$kind".toByteArray()).toString()

object LegacyProfileCodec {
    const val INLINE_PREFIX = "data:image/jpeg;base64,"
    fun avatar(payload: ProfileSyncPayload): String? = payload.displayImagePath
        ?: payload.photoBase64?.let { if (it.isEmpty()) "" else INLINE_PREFIX + it }

    fun fingerprint(payload: ProfileSyncPayload): String {
        val fields = listOf(payload.displayName, payload.company, payload.jobTitle, payload.bio,
            payload.publicSlug.orEmpty(), payload.customDomain.orEmpty(),
            payload.customDomainVerified.toString(), payload.isPublic.toString(), avatar(payload).orEmpty(),
            payload.contacts.firstOrNull { it.kind == "phone" }?.value.orEmpty(),
            payload.contacts.firstOrNull { it.kind == "email" }?.value.orEmpty(),
            payload.addresses.firstOrNull()?.formattedAddress.orEmpty(),
            payload.links.firstOrNull { it.kind == "website" }?.url.orEmpty(),
            payload.links.firstOrNull { it.kind == "linkedin" }?.url.orEmpty(),
            payload.links.firstOrNull { it.kind == "facebook" }?.url.orEmpty(),
            payload.links.firstOrNull { it.kind == "instagram" }?.url.orEmpty(),
            payload.links.firstOrNull { it.kind == "tiktok" }?.url.orEmpty(),
            payload.links.firstOrNull { it.kind == "youtube" }?.url.orEmpty())
        val bytes = Json.encodeToString(JsonArray.serializer(),JsonArray(fields.map(::JsonPrimitive))).toByteArray()
        return MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
    }

    fun write(payload: ProfileSyncPayload, current: LegacyProfileRecord?, ownerId: String): JsonObject {
        require(payload.displayName.trim().length in 2..80) { "A név 2–80 karakter legyen." }
        require(payload.contacts.count { it.kind == "phone" } <= 1 && payload.contacts.count { it.kind == "email" } <= 1 && payload.addresses.size <= 1) {
            "A közös profil egyszerre egy telefonszámot, e-mail-címet és címet támogat. Az adatokat nem csonkítottuk."
        }
        payload.photoBase64?.takeIf(String::isNotEmpty)?.let {
            val bytes = java.util.Base64.getDecoder().decode(it)
            require(bytes.size in 3..262144 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte())
            require(java.util.Base64.getEncoder().encodeToString(bytes) == it)
        }
        val slug = payload.publicSlug?.takeIf { it.isNotBlank() }
            ?: current?.slug
            ?: PublicProfileUrlFactory.automaticSlug(payload.displayName, ownerId)
        require(PublicProfileUrlFactory.isValidSlug(slug))
        val customDomain = payload.customDomain?.takeIf(String::isNotBlank)
            ?.let(PublicProfileUrlFactory::normalizeCustomDomain)
        require(customDomain == null || PublicProfileUrlFactory.isValidCustomDomain(customDomain))
        return buildJsonObject {
            if (current == null) { put("id", stableId(ownerId,"profile")); put("owner_id",ownerId) }
            put("slug",slug); put("display_name",payload.displayName); put("company",payload.company)
            put("job_title",payload.jobTitle); put("bio",payload.bio); put("is_public",payload.isPublic)
            put("custom_domain", customDomain)
            put("phone",payload.contacts.firstOrNull { it.kind == "phone" }?.value.orEmpty())
            put("public_email",payload.contacts.firstOrNull { it.kind == "email" }?.value.orEmpty())
            put("website",payload.links.firstOrNull { it.kind == "website" }?.url.orEmpty())
            put("address",payload.addresses.firstOrNull()?.formattedAddress.orEmpty())
            // Missing photo in an old outbox is KEEP, empty photo is DELETE.
            if (payload.photoBase64 != null || payload.displayImagePath != null) {
                val source = payload.displayImagePath
                require(source.isNullOrEmpty() || source == current?.avatarUrl) { "Ismeretlen képforrás." }
                put("avatar_url",avatar(payload).orEmpty())
            }
        }
    }

    fun canApply(baseVersion: Long, baseFingerprint: String?, current: LegacyProfileRecord): Boolean =
        if (baseFingerprint != null) baseFingerprint == fingerprint(current.payload()) else baseVersion == current.version
}
