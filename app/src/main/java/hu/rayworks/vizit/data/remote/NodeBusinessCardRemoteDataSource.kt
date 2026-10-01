package hu.rayworks.vizit.data.remote

import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.cards.OwnedBusinessCard
import hu.rayworks.vizit.data.sync.ProfileAddressPayload
import hu.rayworks.vizit.data.sync.ProfileContactPayload
import hu.rayworks.vizit.data.sync.ProfileLinkPayload
import hu.rayworks.vizit.data.sync.ProfileSyncPayload
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class NodeBusinessCardRemoteDataSource(private val client: SupabaseClient?) {
    private val api = NodeBackendApi(client)
    private val json = Json { ignoreUnknownKeys = true }
    private val editableSocial = setOf(
        "linkedin", "facebook", "instagram", "tiktok", "youtube", "x", "github", "custom",
    )

    private data class Record(
        val raw: JsonObject,
        val row: LegacyProfileRecord,
        val fingerprint: String,
        val profile: ContactProfile,
    )

    fun authenticatedOwnerId(): String? = api.userId()

    suspend fun list(ownerId: String): List<OwnedBusinessCard> {
        requireSessionOwner(ownerId)
        val response = api.request("GET", "/api/profiles")
        val records = response["profiles"]?.jsonArray.orEmpty().map { element ->
            decodeRecord(element.jsonObject, ownerId)
        }
        check(records.all { it.row.ownerId == ownerId }) {
            "A szerver idegen fiókhoz tartozó névjegyet adott vissza."
        }
        return records.map { it.toOwnedCard(CardPresentation()) }
    }

    suspend fun save(
        ownerId: String,
        existing: OwnedBusinessCard?,
        profile: ContactProfile,
        presentation: CardPresentation,
    ): OwnedBusinessCard {
        requireSessionOwner(ownerId)
        require(existing == null || existing.ownerId == ownerId)
        val current = existing?.let { selected ->
            record(ownerId, selected.profileId).also {
                if (it.fingerprint != selected.fingerprint) {
                    throw NodeBackendException(
                        status = 409,
                        code = "profile_conflict",
                        message = "A névjegy másik készüléken megváltozott. Töltsd újra, majd próbáld ismét.",
                        conflict = true,
                    )
                }
            }
        }
        val photoChanged = current == null || profile.photoBase64 != current.profile.photoBase64
        val payload = profile.toSyncPayload(
            displayImagePath = current?.row?.avatarUrl?.takeUnless { photoChanged },
        )
        val values = LegacyProfileCodec.write(payload, current?.row, ownerId)
        val oldLinks = current?.raw?.get("social_links")?.jsonArray.orEmpty().map { it.jsonObject }
        val requestedLinks = oldLinks
            .filter { it["platform"]?.jsonPrimitive?.contentOrNull !in editableSocial }
            .toMutableList()
        for (kind in editableSocial) {
            val desired = payload.links.firstOrNull { it.kind == kind } ?: continue
            val old = oldLinks.firstOrNull { it["platform"]?.jsonPrimitive?.contentOrNull == kind }
            requestedLinks += buildJsonObject {
                old?.get("id")?.let { put("id", it) }
                put("platform", kind)
                put("label", old?.get("label")?.jsonPrimitive?.contentOrNull ?: desired.label)
                put("url", desired.url)
                put("enabled", old?.get("enabled")?.jsonPrimitive?.booleanOrNull ?: desired.isPublic)
                put("sort_order", old?.get("sort_order")?.jsonPrimitive?.intOrNull ?: desired.sortOrder)
            }
        }
        val normalizedLinks = JsonArray(
            requestedLinks
                .sortedBy { it["sort_order"]?.jsonPrimitive?.intOrNull ?: 0 }
                .mapIndexed { index, link ->
                    buildJsonObject {
                        for (key in listOf("id", "platform", "label", "url", "enabled")) {
                            link[key]?.let { put(key, it) }
                        }
                        put("sort_order", index)
                    }
                },
        )
        val remoteLogo = current?.profile?.logoBase64.orEmpty()
        val theme = current?.raw?.get("theme")?.jsonPrimitive?.contentOrNull ?: "midnight"
        val appearance = RemoteProfileLogo.prepare(
            client = requireNotNull(client),
            owner = ownerId,
            appearance = current?.row?.appearance,
            remoteLogoBase64 = remoteLogo,
            requested = payload.logoPath,
            theme = theme,
        )
        val body = buildJsonObject {
            for ((key, value) in values) if (key !in setOf("id", "owner_id")) put(key, value)
            put("avatar_url", values["avatar_url"] ?: current?.raw?.get("avatar_url") ?: JsonNull)
            put("theme", current?.raw?.get("theme") ?: JsonPrimitive("midnight"))
            put("accent_color", current?.raw?.get("accent_color") ?: JsonPrimitive("#0b5ce8"))
            put("appearance", appearance ?: JsonNull)
            put("social_links", normalizedLinks)
            put("base_fingerprint", current?.fingerprint?.let(::JsonPrimitive) ?: JsonNull)
            put("base_updated_at", current?.row?.updatedAt?.let(::JsonPrimitive) ?: JsonNull)
        }
        val path = existing?.let { "/api/profiles/${it.profileId}" } ?: "/api/profiles"
        val method = if (existing == null) "POST" else "PUT"
        val response = api.request(method, path, body)
        val raw = requireNotNull(response["profile"]?.jsonObject)
        val saved = decodeRecord(
            raw = raw,
            expectedOwnerId = ownerId,
            fingerprint = response["fingerprint"]?.jsonPrimitive?.contentOrNull,
        )
        check(existing == null || saved.row.id == existing.profileId) {
            "A szerver eltérő névjegyet adott vissza."
        }
        return saved.toOwnedCard(presentation)
    }

    suspend fun delete(ownerId: String, profileId: String) {
        requireSessionOwner(ownerId)
        requireUuid(profileId)
        api.request("DELETE", "/api/profiles/$profileId")
        requireSessionOwner(ownerId)
    }

    private suspend fun record(ownerId: String, profileId: String): Record {
        requireUuid(profileId)
        val response = api.request("GET", "/api/profiles/$profileId")
        val raw = requireNotNull(response["profile"]?.jsonObject)
        return decodeRecord(
            raw = raw,
            expectedOwnerId = ownerId,
            fingerprint = response["fingerprint"]?.jsonPrimitive?.contentOrNull,
        ).also { check(it.row.id == profileId) }
    }

    private suspend fun decodeRecord(
        raw: JsonObject,
        expectedOwnerId: String,
        fingerprint: String? = raw["fingerprint"]?.jsonPrimitive?.contentOrNull,
    ): Record {
        val row = json.decodeFromJsonElement<LegacyProfileRecord>(raw)
        check(row.ownerId == expectedOwnerId) {
            "A névjegy nem a bejelentkezett fiókhoz tartozik."
        }
        val verifiedFingerprint = requireNotNull(fingerprint)
        require(verifiedFingerprint.matches(FINGERPRINT))
        val profile = row.payload(
            photo = RemoteContactPhoto.load(row.avatarUrl, BuildConfig.SUPABASE_URL, expectedOwnerId),
            logo = RemoteContactPhoto.load(
                RemoteProfileLogo.url(row.appearance),
                BuildConfig.SUPABASE_URL,
                expectedOwnerId,
            ),
        ).toContactProfile()
        return Record(raw, row, verifiedFingerprint, profile)
    }

    private fun Record.toOwnedCard(presentation: CardPresentation) = OwnedBusinessCard(
        ownerId = row.ownerId,
        profileId = row.id,
        profile = profile,
        fingerprint = fingerprint,
        isPrimary = row.isPrimary,
        createdAt = row.createdAt,
        updatedAt = row.updatedAt,
        presentation = presentation,
    )

    private fun requireSessionOwner(ownerId: String) {
        check(api.userId() == ownerId) { "A munkamenet megváltozott." }
    }

    private fun requireUuid(value: String) {
        require(UUID.matches(value)) { "Érvénytelen névjegy-azonosító." }
    }

    private fun ContactProfile.toSyncPayload(displayImagePath: String?): ProfileSyncPayload = ProfileSyncPayload(
        photoBase64 = photoBase64,
        displayImagePath = displayImagePath,
        displayName = resolvedDisplayName,
        firstName = firstName,
        lastName = lastName,
        company = company,
        jobTitle = jobTitle,
        bio = bio,
        publicSlug = publicSlug.ifBlank { null },
        customDomain = customDomain.ifBlank { null },
        customDomainVerified = customDomainVerified,
        isPublic = isPublic,
        logoPath = if (logoBase64.isBlank()) "" else LegacyProfileCodec.INLINE_PREFIX + logoBase64,
        contacts = listOfNotNull(
            phone.takeIf(String::isNotBlank)?.let {
                ProfileContactPayload("phone", "phone", "Telefon", it, 0, true)
            },
            email.takeIf(String::isNotBlank)?.let {
                ProfileContactPayload("email", "email", "E-mail", it, 1, true)
            },
        ),
        addresses = address.takeIf(String::isNotBlank)?.let {
            listOf(ProfileAddressPayload("address", "Cím", it, 0, true))
        }.orEmpty(),
        links = listOfNotNull(
            website.takeIf(String::isNotBlank)?.let { ProfileLinkPayload("website", "website", "Weboldal", it, 0, true) },
            linkedIn.takeIf(String::isNotBlank)?.let { ProfileLinkPayload("linkedin", "linkedin", "LinkedIn", it, 1, true) },
            facebook.takeIf(String::isNotBlank)?.let { ProfileLinkPayload("facebook", "facebook", "Facebook", it, 2, true) },
            instagram.takeIf(String::isNotBlank)?.let { ProfileLinkPayload("instagram", "instagram", "Instagram", it, 3, true) },
            tiktok.takeIf(String::isNotBlank)?.let { ProfileLinkPayload("tiktok", "tiktok", "TikTok", it, 4, true) },
            youtube.takeIf(String::isNotBlank)?.let { ProfileLinkPayload("youtube", "youtube", "YouTube", it, 5, true) },
            x.takeIf(String::isNotBlank)?.let { ProfileLinkPayload("x", "x", "X", it, 6, true) },
            github.takeIf(String::isNotBlank)?.let { ProfileLinkPayload("github", "github", "GitHub", it, 7, true) },
            customSocial.takeIf(String::isNotBlank)?.let { ProfileLinkPayload("custom", "custom", "Egyéb", it, 8, true) },
        ),
    )

    private fun ProfileSyncPayload.toContactProfile() = ContactProfile(
        fullName = displayName,
        firstName = firstName,
        lastName = lastName,
        jobTitle = jobTitle,
        company = company,
        bio = bio,
        phone = contacts.firstOrNull { it.kind == "phone" }?.value.orEmpty(),
        email = contacts.firstOrNull { it.kind == "email" }?.value.orEmpty(),
        website = links.firstOrNull { it.kind == "website" }?.url.orEmpty(),
        address = addresses.firstOrNull()?.formattedAddress.orEmpty(),
        linkedIn = links.firstOrNull { it.kind == "linkedin" }?.url.orEmpty(),
        facebook = links.firstOrNull { it.kind == "facebook" }?.url.orEmpty(),
        instagram = links.firstOrNull { it.kind == "instagram" }?.url.orEmpty(),
        tiktok = links.firstOrNull { it.kind == "tiktok" }?.url.orEmpty(),
        youtube = links.firstOrNull { it.kind == "youtube" }?.url.orEmpty(),
        x = links.firstOrNull { it.kind == "x" }?.url.orEmpty(),
        github = links.firstOrNull { it.kind == "github" }?.url.orEmpty(),
        customSocial = links.firstOrNull { it.kind == "custom" }?.url.orEmpty(),
        photoBase64 = photoBase64.orEmpty(),
        logoBase64 = logoPath?.removePrefix(LegacyProfileCodec.INLINE_PREFIX).orEmpty(),
        publicSlug = publicSlug.orEmpty(),
        isPublic = isPublic,
        customDomain = customDomain.orEmpty(),
        customDomainVerified = customDomainVerified,
    )

    private companion object {
        val UUID = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[1-5][0-9a-fA-F]{3}-[89abAB][0-9a-fA-F]{3}-[0-9a-fA-F]{12}$")
        val FINGERPRINT = Regex("^[a-f0-9]{64}$")
    }
}
