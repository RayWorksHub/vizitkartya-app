package hu.rayworks.vizit.data

import hu.rayworks.vizit.data.local.LegacyContactProfileStore
import hu.rayworks.vizit.data.local.ProfileLocalStore
import hu.rayworks.vizit.data.settings.AppSettingsStore
import hu.rayworks.vizit.data.sync.EpochClock
import hu.rayworks.vizit.data.sync.OperationIdFactory
import hu.rayworks.vizit.data.sync.ProfileRepositoryState
import hu.rayworks.vizit.data.sync.ProfileSyncScheduler
import java.util.UUID
import kotlinx.coroutines.flow.Flow

class ContactProfileRepository(
    private val localStore: ProfileLocalStore,
    private val settingsStore: AppSettingsStore,
    private val legacyStore: LegacyContactProfileStore,
    private val syncScheduler: ProfileSyncScheduler,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
    private val operationIdFactory: OperationIdFactory = OperationIdFactory { UUID.randomUUID().toString() },
) {
    fun observe(userId: String): Flow<ProfileRepositoryState> = localStore.observe(userId)

    suspend fun prepare(
        userId: String,
        cloudSyncEnabled: Boolean,
    ) {
        val settings = settingsStore.current()
        if (!settings.legacyProfileMigrated) {
            legacyStore.loadOrNull()?.let { legacyProfile ->
                localStore.save(
                    userId = userId,
                    profile = legacyProfile,
                    queueForCloudSync = cloudSyncEnabled,
                    operationId = operationIdFactory.create(),
                    nowEpochMs = clock.nowEpochMs(),
                )
                legacyStore.clear()
            }
            settingsStore.markLegacyProfileMigrated()
        }
        if (cloudSyncEnabled) {
            localStore.queueUnsyncedSnapshotIfNeeded(
                userId = userId,
                operationId = operationIdFactory.create(),
                nowEpochMs = clock.nowEpochMs(),
            )
        }
        settingsStore.setActiveProfileOwnerId(userId)
        if (cloudSyncEnabled && settings.automaticSyncEnabled) syncScheduler.enqueue()
    }

    suspend fun save(
        userId: String,
        profile: ContactProfile,
        cloudSyncEnabled: Boolean,
        automaticSyncEnabled: Boolean,
    ) {
        localStore.save(
            userId = userId,
            profile = profile,
            queueForCloudSync = cloudSyncEnabled,
            operationId = operationIdFactory.create(),
            nowEpochMs = clock.nowEpochMs(),
        )
        if (cloudSyncEnabled && automaticSyncEnabled) syncScheduler.enqueue()
    }

    suspend fun retrySync(userId: String) {
        if (localStore.retryNow(userId, clock.nowEpochMs())) syncScheduler.enqueue()
    }

    fun setAutomaticSyncEnabled(enabled: Boolean) {
        if (enabled) syncScheduler.enqueue() else syncScheduler.cancel()
    }
}
