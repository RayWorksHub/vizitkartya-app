package hu.rayworks.vizit.data.remote

import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.data.sync.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.*

class LegacyProfileRemoteDataSource(private val client: SupabaseClient?) : ProfileRemoteDataSource {
    override fun authenticatedUserId(): String? = client?.auth?.currentSessionOrNull()?.user?.id

    private suspend fun record(userId: String): LegacyProfileRecord? {
        val api = requireNotNull(client)
        check(authenticatedUserId() == userId)
        return api.from("profiles").select(Columns.raw("*,social_links(*)")) {
            filter { eq("owner_id",userId) }; limit(1)
        }.decodeList<LegacyProfileRecord>().firstOrNull()
    }

    private suspend fun snapshot(row: LegacyProfileRecord): RemoteProfileSnapshot = RemoteProfileSnapshot(
        row.version, row.payload(RemoteContactPhoto.load(row.avatarUrl, BuildConfig.SUPABASE_URL))
    )

    override suspend fun pull(userId: String): RemoteProfileSnapshot? {
        if (authenticatedUserId() != userId) return null
        return record(userId)?.let { snapshot(it) }
    }

    override suspend fun push(mutation: PendingProfileMutation): RemoteProfileSyncResult {
        val api = client ?: return RemoteProfileSyncResult.SessionUnavailable
        if (authenticatedUserId() != mutation.userId) return RemoteProfileSyncResult.SessionUnavailable
        var current = record(mutation.userId)
        if (current == null && mutation.baseServerVersion != 0L) {
            return RemoteProfileSyncResult.Conflict(0, ProfileSyncPayload(photoBase64 = "", displayImagePath = ""))
        }
        val sourceChanged = mutation.payload.displayImagePath?.takeIf(String::isNotEmpty)?.let { it != current?.avatarUrl } == true
        if (sourceChanged && current != null) {
            val remote = snapshot(current)
            return RemoteProfileSyncResult.Conflict(remote.serverVersion, remote.payload)
        }
        val values = LegacyProfileCodec.write(mutation.payload,current,mutation.userId)
        if (current != null) {
            val before = current
            val alreadyApplied = values.all { (key,value) ->
                when (key) {
                    "slug" -> value.jsonPrimitive.content == before.slug
                    "display_name" -> value.jsonPrimitive.content == before.displayName
                    "company" -> value.jsonPrimitive.content == before.company
                    "job_title" -> value.jsonPrimitive.content == before.jobTitle
                    "bio" -> value.jsonPrimitive.content == before.bio
                    "is_public" -> value.jsonPrimitive.boolean == before.isPublic
                    "phone" -> value.jsonPrimitive.content == before.phone
                    "public_email" -> value.jsonPrimitive.content == before.publicEmail
                    "website" -> value.jsonPrimitive.content == before.website
                    "address" -> value.jsonPrimitive.content == before.address
                    "avatar_url" -> value.jsonPrimitive.content == before.avatarUrl.orEmpty()
                    else -> false
                }
            } && mutation.payload.links.firstOrNull { it.kind == "linkedin" }?.url.orEmpty() ==
                before.socialLinks.firstOrNull { it.platform == "linkedin" }?.url.orEmpty()
            if (!alreadyApplied && !LegacyProfileCodec.canApply(mutation.baseServerVersion,mutation.payload.baseFingerprint,current)) {
                val remote = snapshot(current)
                return RemoteProfileSyncResult.Conflict(remote.serverVersion,remote.payload)
            }
            if (!alreadyApplied) {
                val rows = api.from("profiles").update(values) {
                    filter { eq("owner_id",mutation.userId); eq("id",before.id); eq("updated_at",before.updatedAt) }
                    select()
                }.decodeList<LegacyProfileRecord>()
                if (rows.isEmpty()) {
                    val raced = record(mutation.userId)?.let { snapshot(it) }
                        ?: RemoteProfileSnapshot(0,ProfileSyncPayload(photoBase64 = "",displayImagePath = ""))
                    return RemoteProfileSyncResult.Conflict(raced.serverVersion,raced.payload)
                }
                current = rows.single().copy(socialLinks = before.socialLinks)
            }
        } else {
            current = api.from("profiles").insert(values) { select() }.decodeSingle<LegacyProfileRecord>()
        }
        check(authenticatedUserId() == mutation.userId)
        val saved = requireNotNull(current)
        val desiredLink = mutation.payload.links.firstOrNull { it.kind == "linkedin" }
        val existingLink = saved.socialLinks.firstOrNull { it.platform == "linkedin" }
        if (desiredLink == null && existingLink != null) {
            api.from("social_links").delete { filter { eq("id",existingLink.id); eq("url",existingLink.url) } }
        } else if (desiredLink != null && (existingLink == null || existingLink.url != desiredLink.url)) {
            val link = buildJsonObject {
                put("url",desiredLink.url)
                if (existingLink == null) {
                    put("id",stableId(mutation.userId,"linkedin")); put("profile_id",saved.id)
                    put("platform","linkedin"); put("label","LinkedIn"); put("sort_order",1)
                    put("enabled", true)
                }
            }
            if (existingLink == null) api.from("social_links").upsert(link)
            else api.from("social_links").update(link) { filter { eq("id",existingLink.id); eq("url",existingLink.url) } }
        }
        val verified = requireNotNull(record(mutation.userId))
        val result = snapshot(verified)
        // Never announce success after a concurrent edit of the just-written primary fields.
        val expected = mutation.payload.copy(publicSlug = saved.slug,
            displayImagePath = saved.avatarUrl.orEmpty(), photoBase64 = mutation.payload.photoBase64)
        if (LegacyProfileCodec.fingerprint(expected) != LegacyProfileCodec.fingerprint(result.payload)) {
            return RemoteProfileSyncResult.Conflict(result.serverVersion,result.payload)
        }
        return RemoteProfileSyncResult.Applied(result.serverVersion,result.payload)
    }
}
