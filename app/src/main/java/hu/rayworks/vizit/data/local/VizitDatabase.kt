package hu.rayworks.vizit.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

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

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles WHERE userId = :userId LIMIT 1")
    fun observeProfile(userId: String): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile_contacts WHERE profileOwnerId = :userId ORDER BY sortOrder, id")
    fun observeContacts(userId: String): Flow<List<ProfileContactEntity>>

    @Query("SELECT * FROM profile_links WHERE profileOwnerId = :userId ORDER BY sortOrder, id")
    fun observeLinks(userId: String): Flow<List<ProfileLinkEntity>>

    @Query("SELECT * FROM profile_addresses WHERE profileOwnerId = :userId ORDER BY sortOrder, id")
    fun observeAddresses(userId: String): Flow<List<ProfileAddressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertProfile(profile: ProfileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertContacts(contacts: List<ProfileContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLinks(links: List<ProfileLinkEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAddresses(addresses: List<ProfileAddressEntity>)

    @Query("DELETE FROM profile_contacts WHERE id = :id")
    suspend fun deleteContact(id: String)

    @Query("DELETE FROM profile_links WHERE id = :id")
    suspend fun deleteLink(id: String)

    @Query("DELETE FROM profile_addresses WHERE id = :id")
    suspend fun deleteAddress(id: String)
}

@Database(
    entities = [
        ProfileEntity::class,
        ProfileContactEntity::class,
        ProfileLinkEntity::class,
        ProfileAddressEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class VizitDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var instance: VizitDatabase? = null

        fun get(context: Context): VizitDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VizitDatabase::class.java,
                "vizit.db",
            ).build().also { instance = it }
        }
    }
}
