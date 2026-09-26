package hu.rayworks.vizit.data.remote

import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.data.sync.*
import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.json.*

/** Retains Room/outbox behaviour; profile reads and writes use the Node API. */
class NodeProfileRemoteDataSource(client: SupabaseClient?) : ProfileRemoteDataSource {
    private val api = NodeBackendApi(client)
    private val json = Json { ignoreUnknownKeys = true }
    private val editableSocial = setOf("linkedin", "facebook", "instagram", "tiktok", "youtube")
    private data class Record(val raw: JsonObject, val row: LegacyProfileRecord, val fingerprint: String)
    override fun authenticatedUserId(): String? = api.userId()

    private suspend fun record(userId: String): Record? {
        check(authenticatedUserId() == userId)
        val response = api.request("GET", "/api/profile")
        val raw = response["profile"]?.takeUnless { it is JsonNull }?.jsonObject ?: return null
        val row = json.decodeFromJsonElement<LegacyProfileRecord>(raw)
        check(row.ownerId == userId) { "A profil nem a bejelentkezett fiókhoz tartozik." }
        val fingerprint = requireNotNull(response["fingerprint"]?.jsonPrimitive?.contentOrNull)
        require(fingerprint.matches(Regex("[a-f0-9]{64}")))
        return Record(raw, row, fingerprint)
    }
    private suspend fun snapshot(record: Record) = RemoteProfileSnapshot(record.row.version,
        record.row.payload(RemoteContactPhoto.load(record.row.avatarUrl, BuildConfig.SUPABASE_URL)))
    override suspend fun pull(userId: String): RemoteProfileSnapshot? {
        if (authenticatedUserId() != userId) return null
        return record(userId)?.let { snapshot(it) }
    }
    private suspend fun conflict(userId: String): RemoteProfileSyncResult.Conflict {
        val latest = record(userId)?.let { snapshot(it) }
            ?: RemoteProfileSnapshot(0, ProfileSyncPayload(photoBase64 = "", displayImagePath = ""))
        return RemoteProfileSyncResult.Conflict(latest.serverVersion, latest.payload)
    }

    override suspend fun push(mutation: PendingProfileMutation): RemoteProfileSyncResult {
        if (authenticatedUserId() != mutation.userId) return RemoteProfileSyncResult.SessionUnavailable
        try {
            val current = record(mutation.userId)
            if (current == null && mutation.baseServerVersion != 0L) return conflict(mutation.userId)
            val sourceChanged = mutation.payload.displayImagePath?.takeIf(String::isNotEmpty)
                ?.let { it != current?.row?.avatarUrl } == true
            if (sourceChanged && current != null) return conflict(mutation.userId)
            val values = LegacyProfileCodec.write(mutation.payload, current?.row, mutation.userId)
            val oldLinks = current?.raw?.get("social_links")?.jsonArray.orEmpty().map { it.jsonObject }
            // Preserve custom/unknown links, their visibility and the existing order.
            val links = oldLinks.filter { it["platform"]?.jsonPrimitive?.contentOrNull !in editableSocial }.toMutableList()
            for (kind in editableSocial) {
                val desired = mutation.payload.links.firstOrNull { it.kind == kind } ?: continue
                val old = oldLinks.firstOrNull { it["platform"]?.jsonPrimitive?.contentOrNull == kind }
                links += buildJsonObject {
                    old?.get("id")?.let { put("id", it) }
                    put("platform", kind)
                    put("label", old?.get("label")?.jsonPrimitive?.contentOrNull ?: desired.label)
                    put("url", desired.url)
                    put("enabled", old?.get("enabled")?.jsonPrimitive?.booleanOrNull ?: desired.isPublic)
                    put("sort_order", old?.get("sort_order")?.jsonPrimitive?.intOrNull ?: desired.sortOrder)
                }
            }
            val desiredLinks = JsonArray(links.sortedBy { it["sort_order"]?.jsonPrimitive?.intOrNull ?: 0 }.mapIndexed { index, link -> buildJsonObject {
                for (key in listOf("id", "platform", "label", "url", "enabled")) link[key]?.let { put(key, it) }
                put("sort_order", index)
            } })
            val body = buildJsonObject {
                for ((key, value) in values) if (key !in setOf("id", "owner_id")) put(key, value)
                put("avatar_url", values["avatar_url"] ?: current?.raw?.get("avatar_url") ?: JsonNull)
                put("theme", current?.raw?.get("theme") ?: JsonPrimitive("midnight"))
                put("accent_color", current?.raw?.get("accent_color") ?: JsonPrimitive("#0b5ce8"))
                put("social_links", desiredLinks)
                put("base_fingerprint", current?.fingerprint)
                put("base_updated_at", current?.row?.updatedAt)
            }
            val expected = mutation.payload.copy(publicSlug = values["slug"]?.jsonPrimitive?.content,
                customDomainVerified = current?.row?.customDomainVerified ?: false,
                displayImagePath = body["avatar_url"]?.jsonPrimitive?.contentOrNull.orEmpty())
            if (current != null && !LegacyProfileCodec.canApply(mutation.baseServerVersion, mutation.payload.baseFingerprint, current.row)) {
                // An offline retry can see its own already-committed profile.
                if (LegacyProfileCodec.fingerprint(expected) == LegacyProfileCodec.fingerprint(current.row.payload())) {
                    val saved = snapshot(current)
                    return RemoteProfileSyncResult.Applied(saved.serverVersion, saved.payload)
                }
                return conflict(mutation.userId)
            }
            val response = api.request("PUT", "/api/profile", body)
            val raw = requireNotNull(response["profile"]?.jsonObject)
            val row = json.decodeFromJsonElement<LegacyProfileRecord>(raw)
            check(row.ownerId == mutation.userId)
            val saved = snapshot(Record(raw, row, requireNotNull(response["fingerprint"]?.jsonPrimitive?.contentOrNull)))
            return RemoteProfileSyncResult.Applied(saved.serverVersion, saved.payload)
        } catch (error: NodeBackendException) {
            if (error.status == 401) return RemoteProfileSyncResult.SessionUnavailable
            if (error.status == 409 && error.conflict) return conflict(mutation.userId)
            throw error
        }
    }
}
