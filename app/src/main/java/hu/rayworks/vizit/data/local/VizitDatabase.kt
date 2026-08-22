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
    ],
    version = 2,
    exportSchema = true,
)
abstract class VizitDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

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

        fun get(context: Context): VizitDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                VizitDatabase::class.java,
                "vizit.db",
            ).addMigrations(MIGRATION_1_2)
                .build()
                .also { instance = it }
        }
    }
}
