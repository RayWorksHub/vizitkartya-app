package hu.rayworks.vizit.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Transaction
    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    fun observeAggregate(userId: String): Flow<RoomProfileAggregate?>

    @Query("SELECT * FROM profile_sync_metadata WHERE userId = :userId LIMIT 1")
    fun observeSyncMetadata(userId: String): Flow<ProfileSyncMetadataEntity?>

    @Query("SELECT * FROM profile_sync_outbox WHERE userId = :userId LIMIT 1")
    fun observeOutbox(userId: String): Flow<ProfileSyncOutboxEntity?>

    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    suspend fun getProfile(userId: String): ProfileEntity?

    @Query("SELECT * FROM profile_contacts WHERE profileOwnerId = :userId ORDER BY sortOrder, id")
    suspend fun getContacts(userId: String): List<ProfileContactEntity>

    @Query("SELECT * FROM profile_links WHERE profileOwnerId = :userId ORDER BY sortOrder, id")
    suspend fun getLinks(userId: String): List<ProfileLinkEntity>

    @Query("SELECT * FROM profile_addresses WHERE profileOwnerId = :userId ORDER BY sortOrder, id")
    suspend fun getAddresses(userId: String): List<ProfileAddressEntity>

    @Query("SELECT * FROM profile_field_settings WHERE userId = :userId LIMIT 1")
    suspend fun getFieldSettings(userId: String): ProfileFieldSettingsEntity?

    @Query("SELECT * FROM profile_sync_metadata WHERE userId = :userId LIMIT 1")
    suspend fun getSyncMetadata(userId: String): ProfileSyncMetadataEntity?

    @Query(
        """
        SELECT * FROM profile_sync_outbox
        WHERE userId = :userId
          AND state IN ('PENDING', 'RETRY', 'SYNCING')
          AND nextAttemptAtEpochMs <= :nowEpochMs
        LIMIT 1
        """,
    )
    suspend fun getDueOutbox(userId: String, nowEpochMs: Long): ProfileSyncOutboxEntity?

    @Query("SELECT * FROM profile_sync_outbox WHERE userId = :userId LIMIT 1")
    suspend fun getOutbox(userId: String): ProfileSyncOutboxEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContacts(contacts: List<ProfileContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLinks(links: List<ProfileLinkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAddresses(addresses: List<ProfileAddressEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertFieldSettings(settings: ProfileFieldSettingsEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSyncMetadata(metadata: ProfileSyncMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertOutbox(outbox: ProfileSyncOutboxEntity)

    @Query("DELETE FROM profile_contacts WHERE profileOwnerId = :userId")
    suspend fun deleteContacts(userId: String)

    @Query("DELETE FROM profile_links WHERE profileOwnerId = :userId")
    suspend fun deleteLinks(userId: String)

    @Query("DELETE FROM profile_addresses WHERE profileOwnerId = :userId")
    suspend fun deleteAddresses(userId: String)

    @Query("DELETE FROM profile_sync_outbox WHERE userId = :userId")
    suspend fun deleteOutbox(userId: String)

    @Query("DELETE FROM profile_field_settings WHERE userId = :userId")
    suspend fun deleteFieldSettings(userId: String)

    @Query("DELETE FROM profile_sync_metadata WHERE userId = :userId")
    suspend fun deleteSyncMetadata(userId: String)

    @Query("DELETE FROM profiles WHERE userId = :userId")
    suspend fun deleteProfile(userId: String)

    @Query(
        """
        UPDATE profile_sync_outbox
        SET state = :state,
            attemptCount = :attemptCount,
            nextAttemptAtEpochMs = :nextAttemptAtEpochMs,
            updatedAtEpochMs = :updatedAtEpochMs,
            lastError = :lastError
        WHERE queueKey = :queueKey AND operationId = :operationId
        """,
    )
    suspend fun updateOutboxIfCurrent(
        queueKey: String,
        operationId: String,
        state: String,
        attemptCount: Int,
        nextAttemptAtEpochMs: Long,
        updatedAtEpochMs: Long,
        lastError: String?,
    ): Int

    @Transaction
    suspend fun getSnapshot(userId: String): LocalProfileSnapshot? {
        val profile = getProfile(userId) ?: return null
        return LocalProfileSnapshot(
            profile = profile,
            contacts = getContacts(userId),
            links = getLinks(userId),
            addresses = getAddresses(userId),
            fieldSettings = getFieldSettings(userId),
        )
    }

    @Transaction
    suspend fun replaceSnapshot(
        snapshot: LocalProfileSnapshot,
        metadata: ProfileSyncMetadataEntity,
        outbox: ProfileSyncOutboxEntity?,
    ) {
        upsertProfile(snapshot.profile)
        deleteContacts(snapshot.profile.userId)
        deleteLinks(snapshot.profile.userId)
        deleteAddresses(snapshot.profile.userId)
        if (snapshot.contacts.isNotEmpty()) upsertContacts(snapshot.contacts)
        if (snapshot.links.isNotEmpty()) upsertLinks(snapshot.links)
        if (snapshot.addresses.isNotEmpty()) upsertAddresses(snapshot.addresses)
        snapshot.fieldSettings?.let { upsertFieldSettings(it) }
        upsertSyncMetadata(metadata)
        if (outbox == null) {
            deleteOutbox(snapshot.profile.userId)
        } else {
            upsertOutbox(outbox)
        }
    }

    @Transaction
    suspend fun completeSyncIfCurrent(
        queueKey: String,
        operationId: String,
        snapshot: LocalProfileSnapshot,
        metadata: ProfileSyncMetadataEntity,
    ): Boolean {
        val current = getOutbox(snapshot.profile.userId)
        if (current?.queueKey != queueKey || current.operationId != operationId) return false
        replaceSnapshot(snapshot, metadata, null)
        return true
    }

    @Transaction
    suspend fun updateOutboxAndMetadataIfCurrent(
        queueKey: String,
        operationId: String,
        userId: String,
        state: String,
        attemptCount: Int,
        nextAttemptAtEpochMs: Long,
        updatedAtEpochMs: Long,
        lastError: String?,
        metadata: ProfileSyncMetadataEntity,
    ): Boolean {
        if (getOutbox(userId)?.operationId != operationId) return false
        val updated = updateOutboxIfCurrent(
            queueKey = queueKey,
            operationId = operationId,
            state = state,
            attemptCount = attemptCount,
            nextAttemptAtEpochMs = nextAttemptAtEpochMs,
            updatedAtEpochMs = updatedAtEpochMs,
            lastError = lastError,
        )
        if (updated == 1) upsertSyncMetadata(metadata)
        return updated == 1
    }

    @Transaction
    suspend fun replaceRemoteSnapshotIfNoOutbox(
        snapshot: LocalProfileSnapshot,
        metadata: ProfileSyncMetadataEntity,
    ): Boolean {
        if (getOutbox(snapshot.profile.userId) != null) return false
        replaceSnapshot(snapshot, metadata, null)
        return true
    }

    @Transaction
    suspend fun deleteUserData(userId: String) {
        deleteOutbox(userId)
        deleteSyncMetadata(userId)
        deleteFieldSettings(userId)
        deleteContacts(userId)
        deleteLinks(userId)
        deleteAddresses(userId)
        deleteProfile(userId)
    }
}
