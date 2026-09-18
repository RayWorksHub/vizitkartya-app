package hu.rayworks.vizit.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Embedded
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey val userId: String,
    val firstName: String,
    val lastName: String,
    val displayName: String,
    val company: String,
    val jobTitle: String,
    val bio: String,
    val displayImagePath: String?,
    val contactImagePath: String?,
    val logoPath: String?,
    val publicSlug: String?,
    val isPublic: Boolean,
    val updatedAtEpochMs: Long,
    val pendingSync: Boolean,
    @ColumnInfo(defaultValue = "''") val localContactPhotoBase64: String = "",
)

@Entity(
    tableName = "profile_contacts",
    indices = [Index("profileOwnerId")],
)
data class ProfileContactEntity(
    @PrimaryKey val id: String,
    val profileOwnerId: String,
    val kind: String,
    val label: String,
    val value: String,
    val sortOrder: Int,
    val isPublic: Boolean,
    val updatedAtEpochMs: Long,
    val pendingSync: Boolean,
)

@Entity(
    tableName = "profile_links",
    indices = [Index("profileOwnerId")],
)
data class ProfileLinkEntity(
    @PrimaryKey val id: String,
    val profileOwnerId: String,
    val kind: String,
    val label: String,
    val url: String,
    val sortOrder: Int,
    val isPublic: Boolean,
    val updatedAtEpochMs: Long,
    val pendingSync: Boolean,
)

@Entity(
    tableName = "profile_addresses",
    indices = [Index("profileOwnerId")],
)
data class ProfileAddressEntity(
    @PrimaryKey val id: String,
    val profileOwnerId: String,
    val label: String,
    val formattedAddress: String,
    val sortOrder: Int,
    val isPublic: Boolean,
    val updatedAtEpochMs: Long,
    val pendingSync: Boolean,
)

@Entity(tableName = "profile_field_settings")
data class ProfileFieldSettingsEntity(
    @PrimaryKey val userId: String,
    val fieldOrderJson: String,
    val fieldVisibilityJson: String,
    val updatedAtEpochMs: Long,
    val pendingSync: Boolean,
)

@Entity(tableName = "profile_sync_metadata")
data class ProfileSyncMetadataEntity(
    @PrimaryKey val userId: String,
    val serverVersion: Long,
    val state: String,
    val lastSyncedAtEpochMs: Long?,
    val lastAttemptAtEpochMs: Long?,
    val lastError: String?,
    val conflictServerVersion: Long?,
    val conflictSnapshotJson: String?,
)

@Entity(
    tableName = "profile_sync_outbox",
    indices = [
        Index(value = ["userId"], unique = true),
        Index(value = ["state", "nextAttemptAtEpochMs"]),
    ],
)
data class ProfileSyncOutboxEntity(
    @PrimaryKey val queueKey: String,
    val operationId: String,
    val userId: String,
    val payloadJson: String,
    val baseServerVersion: Long,
    val state: String,
    val attemptCount: Int,
    val nextAttemptAtEpochMs: Long,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
    val lastError: String?,
)

data class LocalProfileSnapshot(
    val profile: ProfileEntity,
    val contacts: List<ProfileContactEntity>,
    val links: List<ProfileLinkEntity>,
    val addresses: List<ProfileAddressEntity>,
    val fieldSettings: ProfileFieldSettingsEntity?,
)

data class RoomProfileAggregate(
    @Embedded val profile: ProfileEntity,
    @Relation(parentColumn = "userId", entityColumn = "profileOwnerId")
    val contacts: List<ProfileContactEntity>,
    @Relation(parentColumn = "userId", entityColumn = "profileOwnerId")
    val links: List<ProfileLinkEntity>,
    @Relation(parentColumn = "userId", entityColumn = "profileOwnerId")
    val addresses: List<ProfileAddressEntity>,
    @Relation(parentColumn = "userId", entityColumn = "userId")
    val fieldSettings: ProfileFieldSettingsEntity?,
)
