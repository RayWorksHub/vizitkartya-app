package hu.rayworks.vizit.data.sync

import hu.rayworks.vizit.data.ContactProfile
import kotlinx.serialization.Serializable

enum class ProfileSyncStatus {
    LOCAL_ONLY,
    SYNCED,
    PENDING,
    SYNCING,
    RETRY_SCHEDULED,
    CONFLICT,
}

data class ProfileSyncState(
    val status: ProfileSyncStatus = ProfileSyncStatus.LOCAL_ONLY,
    val pendingChanges: Boolean = false,
    val lastSyncedAtEpochMs: Long? = null,
    val lastError: String? = null,
    val conflictServerVersion: Long? = null,
)

data class ProfileRepositoryState(
    val profile: ContactProfile = ContactProfile(),
    val sync: ProfileSyncState = ProfileSyncState(),
)

@Serializable
data class ProfileSyncPayload(
    val firstName: String = "",
    val lastName: String = "",
    val displayName: String = "",
    val company: String = "",
    val jobTitle: String = "",
    val bio: String = "",
    val displayImagePath: String? = null,
    val contactImagePath: String? = null,
    val logoPath: String? = null,
    val publicSlug: String? = null,
    val isPublic: Boolean = false,
    val fieldOrder: List<String> = emptyList(),
    val fieldVisibility: Map<String, Boolean> = emptyMap(),
    val contacts: List<ProfileContactPayload> = emptyList(),
    val addresses: List<ProfileAddressPayload> = emptyList(),
    val links: List<ProfileLinkPayload> = emptyList(),
)

@Serializable
data class ProfileContactPayload(
    val id: String,
    val kind: String,
    val label: String,
    val value: String,
    val sortOrder: Int,
    val isPublic: Boolean,
)

@Serializable
data class ProfileAddressPayload(
    val id: String,
    val label: String,
    val formattedAddress: String,
    val sortOrder: Int,
    val isPublic: Boolean,
)

@Serializable
data class ProfileLinkPayload(
    val id: String,
    val kind: String,
    val label: String,
    val url: String,
    val sortOrder: Int,
    val isPublic: Boolean,
)

data class PendingProfileMutation(
    val queueKey: String,
    val operationId: String,
    val userId: String,
    val payload: ProfileSyncPayload,
    val baseServerVersion: Long,
    val attemptCount: Int,
)

sealed interface RemoteProfileSyncResult {
    data class Applied(
        val serverVersion: Long,
        val snapshot: ProfileSyncPayload,
    ) : RemoteProfileSyncResult

    data class Conflict(
        val serverVersion: Long,
        val serverSnapshot: ProfileSyncPayload,
    ) : RemoteProfileSyncResult

    data object SessionUnavailable : RemoteProfileSyncResult
}

data class RemoteProfileSnapshot(
    val serverVersion: Long,
    val payload: ProfileSyncPayload,
)

interface ProfileRemoteDataSource {
    fun authenticatedUserId(): String?

    suspend fun push(mutation: PendingProfileMutation): RemoteProfileSyncResult

    suspend fun pull(userId: String): RemoteProfileSnapshot?
}

interface ProfileSyncScheduler {
    fun enqueue()

    fun cancel()
}

fun interface EpochClock {
    fun nowEpochMs(): Long
}

fun interface OperationIdFactory {
    fun create(): String
}
