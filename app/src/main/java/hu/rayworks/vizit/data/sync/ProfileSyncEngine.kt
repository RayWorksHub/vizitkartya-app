package hu.rayworks.vizit.data.sync

import hu.rayworks.vizit.data.local.ProfileLocalStore
import kotlinx.coroutines.CancellationException

enum class ProfileSyncRunResult {
    COMPLETE,
    RETRY,
    WAITING_FOR_SESSION,
    CONFLICT,
}

class ProfileSyncEngine(
    private val localStore: ProfileLocalStore,
    private val remoteDataSource: ProfileRemoteDataSource,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
) {
    suspend fun run(maxMutations: Int = 20): ProfileSyncRunResult {
        val userId = remoteDataSource.authenticatedUserId()
            ?: return ProfileSyncRunResult.WAITING_FOR_SESSION

        repeat(maxMutations) {
            val mutation = localStore.dueMutation(userId, clock.nowEpochMs()) ?: return@repeat
            if (!localStore.markSyncing(mutation, clock.nowEpochMs())) return@repeat

            val result = try {
                remoteDataSource.push(mutation)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                localStore.markRetry(
                    mutation = mutation,
                    nowEpochMs = clock.nowEpochMs(),
                    safeError = RETRYABLE_ERROR,
                )
                return ProfileSyncRunResult.RETRY
            }

            when (result) {
                is RemoteProfileSyncResult.Applied -> localStore.completeSync(
                    mutation = mutation,
                    remote = RemoteProfileSnapshot(result.serverVersion, result.snapshot),
                    nowEpochMs = clock.nowEpochMs(),
                )

                is RemoteProfileSyncResult.Conflict -> {
                    localStore.markConflict(
                        mutation = mutation,
                        remote = RemoteProfileSnapshot(result.serverVersion, result.serverSnapshot),
                        nowEpochMs = clock.nowEpochMs(),
                    )
                    return ProfileSyncRunResult.CONFLICT
                }

                RemoteProfileSyncResult.SessionUnavailable -> {
                    localStore.markRetry(
                        mutation = mutation,
                        nowEpochMs = clock.nowEpochMs(),
                        safeError = SESSION_ERROR,
                    )
                    return ProfileSyncRunResult.WAITING_FOR_SESSION
                }
            }
        }

        if (localStore.hasPendingMutation(userId)) {
            return if (localStore.hasRetryableMutation(userId)) {
                ProfileSyncRunResult.RETRY
            } else {
                ProfileSyncRunResult.COMPLETE
            }
        }

        return try {
            remoteDataSource.pull(userId)?.let { remote ->
                localStore.applyRemoteIfClean(
                    userId = userId,
                    remote = remote,
                    nowEpochMs = clock.nowEpochMs(),
                )
            }
            ProfileSyncRunResult.COMPLETE
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            ProfileSyncRunResult.RETRY
        }
    }

    private companion object {
        const val RETRYABLE_ERROR = "A felhőszinkron átmenetileg nem érhető el. Az adatok helyben megmaradtak."
        const val SESSION_ERROR = "A szinkron a következő érvényes bejelentkezéskor folytatódik."
    }
}
