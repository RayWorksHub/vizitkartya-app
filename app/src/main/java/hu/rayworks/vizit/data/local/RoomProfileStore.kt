package hu.rayworks.vizit.data.local

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.sync.PendingProfileMutation
import hu.rayworks.vizit.data.sync.ProfilePayloadCodec
import hu.rayworks.vizit.data.sync.ProfileRepositoryState
import hu.rayworks.vizit.data.sync.ProfileSnapshotMapper
import hu.rayworks.vizit.data.sync.ProfileSyncState
import hu.rayworks.vizit.data.sync.ProfileSyncStatus
import hu.rayworks.vizit.data.sync.RemoteProfileSnapshot
import hu.rayworks.vizit.data.sync.SyncRetryPolicy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

interface ProfileLocalStore {
    fun observe(userId: String): Flow<ProfileRepositoryState>

    suspend fun save(
        userId: String,
        profile: ContactProfile,
        queueForCloudSync: Boolean,
        operationId: String,
        nowEpochMs: Long,
    )

    suspend fun dueMutation(userId: String, nowEpochMs: Long): PendingProfileMutation?

    suspend fun hasPendingMutation(userId: String): Boolean

    suspend fun hasRetryableMutation(userId: String): Boolean

    suspend fun queueUnsyncedSnapshotIfNeeded(
        userId: String,
        operationId: String,
        nowEpochMs: Long,
    ): Boolean

    suspend fun markSyncing(mutation: PendingProfileMutation, nowEpochMs: Long): Boolean

    suspend fun markRetry(
        mutation: PendingProfileMutation,
        nowEpochMs: Long,
        safeError: String,
    ): Boolean

    suspend fun markConflict(
        mutation: PendingProfileMutation,
        remote: RemoteProfileSnapshot,
        nowEpochMs: Long,
    ): Boolean

    suspend fun completeSync(
        mutation: PendingProfileMutation,
        remote: RemoteProfileSnapshot,
        nowEpochMs: Long,
    ): Boolean

    suspend fun applyRemoteIfClean(
        userId: String,
        remote: RemoteProfileSnapshot,
        nowEpochMs: Long,
    ): Boolean

    suspend fun retryNow(userId: String, nowEpochMs: Long): Boolean
}

class RoomProfileStore(private val dao: ProfileDao) : ProfileLocalStore {
    override fun observe(userId: String): Flow<ProfileRepositoryState> {
        val snapshot = dao.observeAggregate(userId).map { aggregate ->
            aggregate?.let {
                LocalProfileSnapshot(
                    profile = it.profile,
                    contacts = it.contacts.sortedWith(
                        compareBy(ProfileContactEntity::sortOrder, ProfileContactEntity::id),
                    ),
                    links = it.links.sortedWith(
                        compareBy(ProfileLinkEntity::sortOrder, ProfileLinkEntity::id),
                    ),
                    addresses = it.addresses.sortedWith(
                        compareBy(ProfileAddressEntity::sortOrder, ProfileAddressEntity::id),
                    ),
                    fieldSettings = it.fieldSettings,
                )
            }
        }
        return combine(
            snapshot,
            dao.observeSyncMetadata(userId),
            dao.observeOutbox(userId),
        ) { localSnapshot, metadata, outbox ->
            ProfileRepositoryState(
                profile = ProfileSnapshotMapper.toContactProfile(localSnapshot),
                sync = syncState(metadata, outbox),
            )
        }
    }

    override suspend fun save(
        userId: String,
        profile: ContactProfile,
        queueForCloudSync: Boolean,
        operationId: String,
        nowEpochMs: Long,
    ) {
        val previous = dao.getSnapshot(userId)
        val previousMetadata = dao.getSyncMetadata(userId)
        val snapshot = ProfileSnapshotMapper.toLocalSnapshot(
            userId = userId,
            profile = profile,
            previous = previous,
            updatedAtEpochMs = nowEpochMs,
            pendingSync = queueForCloudSync,
        )
        val payload = ProfileSnapshotMapper.toPayload(snapshot)
        val outbox = if (queueForCloudSync) {
            val previousOutbox = dao.getOutbox(userId)
            ProfileSyncOutboxEntity(
                queueKey = queueKey(userId),
                operationId = operationId,
                userId = userId,
                payloadJson = ProfilePayloadCodec.encode(payload),
                baseServerVersion = previousMetadata?.serverVersion ?: 0L,
                state = OUTBOX_PENDING,
                attemptCount = 0,
                nextAttemptAtEpochMs = nowEpochMs,
                createdAtEpochMs = previousOutbox?.createdAtEpochMs ?: nowEpochMs,
                updatedAtEpochMs = nowEpochMs,
                lastError = null,
            )
        } else {
            null
        }
        val metadata = (previousMetadata ?: emptyMetadata(userId)).copy(
            state = if (queueForCloudSync) STATE_PENDING else STATE_LOCAL_ONLY,
            lastError = null,
        )
        dao.replaceSnapshot(snapshot, metadata, outbox)
    }

    override suspend fun dueMutation(userId: String, nowEpochMs: Long): PendingProfileMutation? =
        dao.getDueOutbox(userId, nowEpochMs)?.toDomain()

    override suspend fun hasPendingMutation(userId: String): Boolean = dao.getOutbox(userId) != null

    override suspend fun hasRetryableMutation(userId: String): Boolean =
        dao.getOutbox(userId)?.state?.let { it != OUTBOX_CONFLICT } == true

    override suspend fun queueUnsyncedSnapshotIfNeeded(
        userId: String,
        operationId: String,
        nowEpochMs: Long,
    ): Boolean {
        if (dao.getOutbox(userId) != null) return false
        val snapshot = dao.getSnapshot(userId) ?: return false
        val hasUnsyncedRows = snapshot.profile.pendingSync ||
            snapshot.contacts.any { it.pendingSync } ||
            snapshot.links.any { it.pendingSync } ||
            snapshot.addresses.any { it.pendingSync } ||
            snapshot.fieldSettings?.pendingSync == true
        if (!hasUnsyncedRows) return false
        val metadata = dao.getSyncMetadata(userId) ?: emptyMetadata(userId)
        dao.replaceSnapshot(
            snapshot = snapshot,
            metadata = metadata.copy(state = STATE_PENDING, lastError = null),
            outbox = ProfileSyncOutboxEntity(
                queueKey = queueKey(userId),
                operationId = operationId,
                userId = userId,
                payloadJson = ProfilePayloadCodec.encode(ProfileSnapshotMapper.toPayload(snapshot)),
                baseServerVersion = metadata.serverVersion,
                state = OUTBOX_PENDING,
                attemptCount = 0,
                nextAttemptAtEpochMs = nowEpochMs,
                createdAtEpochMs = nowEpochMs,
                updatedAtEpochMs = nowEpochMs,
                lastError = null,
            ),
        )
        return true
    }

    override suspend fun markSyncing(
        mutation: PendingProfileMutation,
        nowEpochMs: Long,
    ): Boolean {
        val metadata = (dao.getSyncMetadata(mutation.userId) ?: emptyMetadata(mutation.userId)).copy(
            state = STATE_SYNCING,
            lastAttemptAtEpochMs = nowEpochMs,
            lastError = null,
        )
        return dao.updateOutboxAndMetadataIfCurrent(
            queueKey = mutation.queueKey,
            operationId = mutation.operationId,
            userId = mutation.userId,
            state = OUTBOX_SYNCING,
            attemptCount = mutation.attemptCount,
            nextAttemptAtEpochMs = nowEpochMs + SYNC_LEASE_MILLIS,
            updatedAtEpochMs = nowEpochMs,
            lastError = null,
            metadata = metadata,
        )
    }

    override suspend fun markRetry(
        mutation: PendingProfileMutation,
        nowEpochMs: Long,
        safeError: String,
    ): Boolean {
        val nextAttempt = nowEpochMs + SyncRetryPolicy.delayMillis(mutation.attemptCount)
        val metadata = (dao.getSyncMetadata(mutation.userId) ?: emptyMetadata(mutation.userId)).copy(
            state = STATE_RETRY,
            lastAttemptAtEpochMs = nowEpochMs,
            lastError = safeError,
        )
        return dao.updateOutboxAndMetadataIfCurrent(
            queueKey = mutation.queueKey,
            operationId = mutation.operationId,
            userId = mutation.userId,
            state = OUTBOX_RETRY,
            attemptCount = mutation.attemptCount + 1,
            nextAttemptAtEpochMs = nextAttempt,
            updatedAtEpochMs = nowEpochMs,
            lastError = safeError,
            metadata = metadata,
        )
    }

    override suspend fun markConflict(
        mutation: PendingProfileMutation,
        remote: RemoteProfileSnapshot,
        nowEpochMs: Long,
    ): Boolean {
        val safeError = "A helyi és a felhőprofil egyszerre módosult. Feloldás szükséges."
        val metadata = (dao.getSyncMetadata(mutation.userId) ?: emptyMetadata(mutation.userId)).copy(
            state = STATE_CONFLICT,
            lastAttemptAtEpochMs = nowEpochMs,
            lastError = safeError,
            conflictServerVersion = remote.serverVersion,
            conflictSnapshotJson = ProfilePayloadCodec.encode(remote.payload),
        )
        return dao.updateOutboxAndMetadataIfCurrent(
            queueKey = mutation.queueKey,
            operationId = mutation.operationId,
            userId = mutation.userId,
            state = OUTBOX_CONFLICT,
            attemptCount = mutation.attemptCount,
            nextAttemptAtEpochMs = Long.MAX_VALUE,
            updatedAtEpochMs = nowEpochMs,
            lastError = safeError,
            metadata = metadata,
        )
    }

    override suspend fun completeSync(
        mutation: PendingProfileMutation,
        remote: RemoteProfileSnapshot,
        nowEpochMs: Long,
    ): Boolean {
        val previous = dao.getSnapshot(mutation.userId)
        val snapshot = ProfileSnapshotMapper.toLocalSnapshot(
            userId = mutation.userId,
            payload = remote.payload,
            previous = previous,
            updatedAtEpochMs = nowEpochMs,
        )
        val metadata = ProfileSyncMetadataEntity(
            userId = mutation.userId,
            serverVersion = remote.serverVersion,
            state = STATE_SYNCED,
            lastSyncedAtEpochMs = nowEpochMs,
            lastAttemptAtEpochMs = nowEpochMs,
            lastError = null,
            conflictServerVersion = null,
            conflictSnapshotJson = null,
        )
        return dao.completeSyncIfCurrent(
            queueKey = mutation.queueKey,
            operationId = mutation.operationId,
            snapshot = snapshot,
            metadata = metadata,
        )
    }

    override suspend fun applyRemoteIfClean(
        userId: String,
        remote: RemoteProfileSnapshot,
        nowEpochMs: Long,
    ): Boolean {
        val currentMetadata = dao.getSyncMetadata(userId)
        if (remote.serverVersion < (currentMetadata?.serverVersion ?: 0L)) return false
        val snapshot = ProfileSnapshotMapper.toLocalSnapshot(
            userId = userId,
            payload = remote.payload,
            previous = dao.getSnapshot(userId),
            updatedAtEpochMs = nowEpochMs,
        )
        return dao.replaceRemoteSnapshotIfNoOutbox(
            snapshot = snapshot,
            metadata = ProfileSyncMetadataEntity(
                userId = userId,
                serverVersion = remote.serverVersion,
                state = STATE_SYNCED,
                lastSyncedAtEpochMs = nowEpochMs,
                lastAttemptAtEpochMs = nowEpochMs,
                lastError = null,
                conflictServerVersion = null,
                conflictSnapshotJson = null,
            ),
        )
    }

    override suspend fun retryNow(userId: String, nowEpochMs: Long): Boolean {
        val outbox = dao.getOutbox(userId) ?: return false
        if (outbox.state == OUTBOX_CONFLICT) return false
        val metadata = (dao.getSyncMetadata(userId) ?: emptyMetadata(userId)).copy(
            state = STATE_PENDING,
            lastError = null,
        )
        return dao.updateOutboxAndMetadataIfCurrent(
            queueKey = outbox.queueKey,
            operationId = outbox.operationId,
            userId = userId,
            state = OUTBOX_PENDING,
            attemptCount = outbox.attemptCount,
            nextAttemptAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs,
            lastError = null,
            metadata = metadata,
        )
    }

    private fun syncState(
        metadata: ProfileSyncMetadataEntity?,
        outbox: ProfileSyncOutboxEntity?,
    ): ProfileSyncState {
        val status = when (outbox?.state ?: metadata?.state) {
            OUTBOX_PENDING, STATE_PENDING -> ProfileSyncStatus.PENDING
            OUTBOX_SYNCING, STATE_SYNCING -> ProfileSyncStatus.SYNCING
            OUTBOX_RETRY, STATE_RETRY -> ProfileSyncStatus.RETRY_SCHEDULED
            OUTBOX_CONFLICT, STATE_CONFLICT -> ProfileSyncStatus.CONFLICT
            STATE_SYNCED -> ProfileSyncStatus.SYNCED
            else -> ProfileSyncStatus.LOCAL_ONLY
        }
        return ProfileSyncState(
            status = status,
            pendingChanges = outbox != null,
            lastSyncedAtEpochMs = metadata?.lastSyncedAtEpochMs,
            lastError = metadata?.lastError,
            conflictServerVersion = metadata?.conflictServerVersion,
        )
    }

    private fun ProfileSyncOutboxEntity.toDomain(): PendingProfileMutation = PendingProfileMutation(
        queueKey = queueKey,
        operationId = operationId,
        userId = userId,
        payload = ProfilePayloadCodec.decode(payloadJson),
        baseServerVersion = baseServerVersion,
        attemptCount = attemptCount,
    )

    private companion object {
        const val OUTBOX_PENDING = "PENDING"
        const val OUTBOX_SYNCING = "SYNCING"
        const val OUTBOX_RETRY = "RETRY"
        const val OUTBOX_CONFLICT = "CONFLICT"
        const val STATE_LOCAL_ONLY = "LOCAL_ONLY"
        const val STATE_PENDING = "PENDING"
        const val STATE_SYNCING = "SYNCING"
        const val STATE_RETRY = "RETRY"
        const val STATE_CONFLICT = "CONFLICT"
        const val STATE_SYNCED = "SYNCED"
        const val SYNC_LEASE_MILLIS = 5 * 60 * 1_000L

        fun queueKey(userId: String): String = "profile:$userId"

        fun emptyMetadata(userId: String) = ProfileSyncMetadataEntity(
            userId = userId,
            serverVersion = 0L,
            state = STATE_LOCAL_ONLY,
            lastSyncedAtEpochMs = null,
            lastAttemptAtEpochMs = null,
            lastError = null,
            conflictServerVersion = null,
            conflictSnapshotJson = null,
        )
    }
}
