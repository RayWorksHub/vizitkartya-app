package hu.rayworks.vizit.data.local

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "business_card_cache",
    primaryKeys = ["ownerId", "profileId"],
    indices = [Index("ownerId")],
)
data class BusinessCardCacheEntity(
    val ownerId: String,
    val profileId: String,
    val payloadJson: String,
    val fingerprint: String,
    val isPrimary: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val cachedAtEpochMs: Long,
)

@Entity(tableName = "business_card_selection")
data class BusinessCardSelectionEntity(
    @androidx.room.PrimaryKey val ownerId: String,
    val profileId: String,
)
