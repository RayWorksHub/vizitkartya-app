package hu.rayworks.vizit.data.remote

import hu.rayworks.vizit.data.sync.PendingProfileMutation
import hu.rayworks.vizit.data.sync.ProfileRemoteDataSource
import hu.rayworks.vizit.data.sync.ProfileSyncPayload
import hu.rayworks.vizit.data.sync.RemoteProfileSnapshot
import hu.rayworks.vizit.data.sync.RemoteProfileSyncResult
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SupabaseProfileRemoteDataSource(
    private val client: SupabaseClient?,
) : ProfileRemoteDataSource {
    private val json = Json { ignoreUnknownKeys = true }

    override fun authenticatedUserId(): String? = client
        ?.auth
        ?.currentSessionOrNull()
        ?.user
        ?.id

    override suspend fun push(mutation: PendingProfileMutation): RemoteProfileSyncResult {
        val actualClient = client ?: return RemoteProfileSyncResult.SessionUnavailable
        if (authenticatedUserId() != mutation.userId) return RemoteProfileSyncResult.SessionUnavailable

        val response = actualClient.postgrest.rpc(
            function = "sync_profile_snapshot",
            parameters = ProfileSyncRpcParameters(
                operationId = mutation.operationId,
                baseVersion = mutation.baseServerVersion,
                snapshot = mutation.payload,
            ),
        ).data.let { json.decodeFromString<ProfileSyncRpcResponse>(it) }

        return when (response.status) {
            "applied" -> RemoteProfileSyncResult.Applied(
                serverVersion = response.serverVersion,
                snapshot = requireNotNull(response.snapshot),
            )

            "conflict" -> RemoteProfileSyncResult.Conflict(
                serverVersion = response.serverVersion,
                serverSnapshot = requireNotNull(response.snapshot),
            )

            else -> error("Unexpected profile sync response")
        }
    }

    override suspend fun pull(userId: String): RemoteProfileSnapshot? {
        val actualClient = client ?: return null
        if (authenticatedUserId() != userId) return null
        val response = actualClient.postgrest.rpc("get_my_profile_snapshot")
            .data
            .let { json.decodeFromString<ProfileSyncRpcResponse>(it) }
        return response.snapshot?.let { RemoteProfileSnapshot(response.serverVersion, it) }
    }
}

@Serializable
private data class ProfileSyncRpcParameters(
    @SerialName("p_operation_id") val operationId: String,
    @SerialName("p_base_version") val baseVersion: Long,
    @SerialName("p_snapshot") val snapshot: ProfileSyncPayload,
)

@Serializable
private data class ProfileSyncRpcResponse(
    val status: String,
    val serverVersion: Long,
    val snapshot: ProfileSyncPayload? = null,
)
