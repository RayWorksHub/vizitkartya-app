package hu.rayworks.vizit.data.account

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.sync.ProfilePayloadCodec
import hu.rayworks.vizit.data.sync.ProfileSyncStatus
import hu.rayworks.vizit.data.sync.RemoteProfileSnapshot
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json

@Entity(tableName = "account_profile_cache", primaryKeys = ["ownerId", "profileId"])
data class AccountProfileEntity(
    val ownerId: String,
    val profileId: String,
    val payloadJson: String,
    val serverVersion: Long,
    val isDefault: Boolean,
    val createdAt: String,
    val presentationJson: String,
    val status: String,
    val operationId: String?,
    val attemptCount: Int,
    val nextAttemptAtEpochMs: Long,
    val lastSyncedAtEpochMs: Long?,
    val lastError: String?,
    val conflictJson: String?,
    val conflictVersion: Long?,
)

@Entity(tableName = "account_profile_selection")
data class AccountProfileSelection(@PrimaryKey val ownerId: String, val profileId: String)

@Dao
interface AccountProfileDao {
    @Query("SELECT * FROM account_profile_cache WHERE ownerId = :ownerId ORDER BY createdAt, profileId")
    fun observe(ownerId: String): Flow<List<AccountProfileEntity>>

    @Query("SELECT * FROM account_profile_cache WHERE ownerId = :ownerId ORDER BY createdAt, profileId")
    suspend fun list(ownerId: String): List<AccountProfileEntity>

    @Query("SELECT * FROM account_profile_cache WHERE ownerId = :ownerId AND profileId = :profileId")
    suspend fun get(ownerId: String, profileId: String): AccountProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(profile: AccountProfileEntity)

    @Query("DELETE FROM account_profile_cache WHERE ownerId = :ownerId AND profileId = :profileId")
    suspend fun delete(ownerId: String, profileId: String)

    @Query("DELETE FROM account_profile_cache WHERE ownerId = :ownerId")
    suspend fun deleteAccount(ownerId: String)

    @Query("SELECT * FROM account_profile_selection WHERE ownerId = :ownerId")
    suspend fun selection(ownerId: String): AccountProfileSelection?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun select(selection: AccountProfileSelection)

    @Query("DELETE FROM account_profile_selection WHERE ownerId = :ownerId")
    suspend fun deleteSelection(ownerId: String)
}

private val presentationCodec = Json { ignoreUnknownKeys = true }

internal fun AccountProfileEntity.toOwned() = OwnedProfile(
    ownerId = ownerId, id = profileId, payload = ProfilePayloadCodec.decode(payloadJson),
    serverVersion = serverVersion, isDefault = isDefault, createdAt = createdAt,
    presentation = presentationCodec.decodeFromString(CardPresentation.serializer(), presentationJson),
    status = ProfileSyncStatus.valueOf(status), operationId = operationId, attemptCount = attemptCount,
    nextAttemptAtEpochMs = nextAttemptAtEpochMs, lastSyncedAtEpochMs = lastSyncedAtEpochMs,
    lastError = lastError,
    conflict = conflictJson?.let { RemoteProfileSnapshot(requireNotNull(conflictVersion), ProfilePayloadCodec.decode(it)) },
)

internal fun OwnedProfile.toEntity() = AccountProfileEntity(
    ownerId, id, ProfilePayloadCodec.encode(payload), serverVersion, isDefault, createdAt,
    presentationCodec.encodeToString(CardPresentation.serializer(), presentation), status.name,
    operationId, attemptCount, nextAttemptAtEpochMs, lastSyncedAtEpochMs, lastError,
    conflict?.payload?.let(ProfilePayloadCodec::encode), conflict?.serverVersion,
)
