package hu.rayworks.vizit.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ProfileEntity::class,
        ProfileContactEntity::class,
        ProfileLinkEntity::class,
        ProfileAddressEntity::class,
        ProfileFieldSettingsEntity::class,
        ProfileSyncMetadataEntity::class,
        ProfileSyncOutboxEntity::class,
        BusinessCardCacheEntity::class,
        BusinessCardSelectionEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class VizitDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun businessCardDao(): BusinessCardDao

    companion object {
        @Volatile
        private var instance: VizitDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE profiles ADD COLUMN localContactPhotoBase64 TEXT NOT NULL DEFAULT ''",
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS profile_field_settings (
                        userId TEXT NOT NULL PRIMARY KEY,
                        fieldOrderJson TEXT NOT NULL,
                        fieldVisibilityJson TEXT NOT NULL,
                        updatedAtEpochMs INTEGER NOT NULL,
                        pendingSync INTEGER NOT NULL
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS profile_sync_metadata (
                        userId TEXT NOT NULL PRIMARY KEY,
                        serverVersion INTEGER NOT NULL,
                        state TEXT NOT NULL,
                        lastSyncedAtEpochMs INTEGER,
                        lastAttemptAtEpochMs INTEGER,
                        lastError TEXT,
                        conflictServerVersion INTEGER,
                        conflictSnapshotJson TEXT
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS profile_sync_outbox (
                        queueKey TEXT NOT NULL PRIMARY KEY,
                        operationId TEXT NOT NULL,
                        userId TEXT NOT NULL,
                        payloadJson TEXT NOT NULL,
                        baseServerVersion INTEGER NOT NULL,
                        state TEXT NOT NULL,
                        attemptCount INTEGER NOT NULL,
                        nextAttemptAtEpochMs INTEGER NOT NULL,
                        createdAtEpochMs INTEGER NOT NULL,
                        updatedAtEpochMs INTEGER NOT NULL,
                        lastError TEXT
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_profile_sync_outbox_userId ON profile_sync_outbox(userId)",
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_profile_sync_outbox_state_nextAttemptAtEpochMs " +
                        "ON profile_sync_outbox(state, nextAttemptAtEpochMs)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE profiles ADD COLUMN customDomain TEXT DEFAULT NULL")
                database.execSQL(
                    "ALTER TABLE profiles ADD COLUMN customDomainVerified INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS business_card_cache (
                        ownerId TEXT NOT NULL,
                        profileId TEXT NOT NULL,
                        payloadJson TEXT NOT NULL,
                        fingerprint TEXT NOT NULL,
                        isPrimary INTEGER NOT NULL,
                        createdAt TEXT NOT NULL,
                        updatedAt TEXT NOT NULL,
                        cachedAtEpochMs INTEGER NOT NULL,
                        PRIMARY KEY(ownerId, profileId)
                    )
                    """.trimIndent(),
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_business_card_cache_ownerId " +
                        "ON business_card_cache(ownerId)",
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS business_card_selection (
                        ownerId TEXT NOT NULL PRIMARY KEY,
                        profileId TEXT NOT NULL
                    )
                    """.trimIndent(),
                )
            }
        }

        fun get(context: Context): VizitDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VizitDatabase::class.java,
                "vizit.db",
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build()
                .also { instance = it }
        }
    }
}
