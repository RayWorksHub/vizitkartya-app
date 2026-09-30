package hu.rayworks.vizit.data.account

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.sync.ProfileSnapshotMapper
import hu.rayworks.vizit.data.sync.ProfileSyncPayload
import hu.rayworks.vizit.data.sync.ProfileSyncState
import hu.rayworks.vizit.data.sync.ProfileSyncStatus
import hu.rayworks.vizit.data.sync.RemoteProfileSnapshot

/** One complete profile. Neither its ID nor its contact email is an account identity. */
data class OwnedProfile(
    val ownerId: String,
    val id: String,
    val payload: ProfileSyncPayload,
    val serverVersion: Long,
    val isDefault: Boolean = false,
    val createdAt: String = "",
    val presentation: CardPresentation = CardPresentation(),
    val status: ProfileSyncStatus = ProfileSyncStatus.SYNCED,
    val operationId: String? = null,
    val attemptCount: Int = 0,
    val nextAttemptAtEpochMs: Long = 0,
    val lastSyncedAtEpochMs: Long? = null,
    val lastError: String? = null,
    val conflict: RemoteProfileSnapshot? = null,
) {
    val displayName: String get() = payload.displayName
    val slug: String get() = payload.publicSlug.orEmpty()
    val profile: ContactProfile
        get() = ProfileSnapshotMapper.toContactProfile(
            ProfileSnapshotMapper.toLocalSnapshot("$ownerId/$id", payload, null, 0),
        )
    val sync: ProfileSyncState
        get() = ProfileSyncState(status, operationId != null, lastSyncedAtEpochMs, lastError, conflict?.serverVersion)
}

enum class AccountProfileLoadStatus { LOADING, READY, UNAVAILABLE }

/** The UI observes this single value, so IDs, content and sync status change together. */
data class AccountProfileState(
    val ownerId: String? = null,
    val profiles: List<OwnedProfile> = emptyList(),
    val activeId: String? = null,
    val loadStatus: AccountProfileLoadStatus = AccountProfileLoadStatus.LOADING,
    val catalogVerified: Boolean = false,
    val operationBusy: Boolean = false,
    val message: String? = null,
) {
    val active: OwnedProfile? get() = profiles.firstOrNull { it.id == activeId && it.ownerId == ownerId }
    val needsFirstProfile: Boolean
        get() = ownerId != null && catalogVerified && profiles.isEmpty() && loadStatus == AccountProfileLoadStatus.READY
}
