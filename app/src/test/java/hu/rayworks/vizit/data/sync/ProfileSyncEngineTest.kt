package hu.rayworks.vizit.data.sync

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.local.ProfileLocalStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileSyncEngineTest {
    @Test
    fun `returns without touching outbox when session is unavailable`() = runBlocking {
        val store = FakeProfileLocalStore(mutation())
        val remote = FakeRemoteDataSource(userId = null)

        val result = ProfileSyncEngine(store, remote, EpochClock { 100L }).run()

        assertEquals(ProfileSyncRunResult.WAITING_FOR_SESSION, result)
        assertFalse(store.syncing)
        assertEquals(0, remote.pushCount)
    }

    @Test
    fun `applied mutation is completed and followed by safe pull`() = runBlocking {
        val store = FakeProfileLocalStore(mutation())
        val remotePayload = payload("Felhő Elek")
        val remote = FakeRemoteDataSource(
            userId = USER_ID,
            pushResult = RemoteProfileSyncResult.Applied(4L, remotePayload),
            pullResult = RemoteProfileSnapshot(4L, remotePayload),
        )

        val result = ProfileSyncEngine(store, remote, EpochClock { 100L }).run()

        assertEquals(ProfileSyncRunResult.COMPLETE, result)
        assertTrue(store.syncing)
        assertEquals(4L, store.completed?.serverVersion)
        assertEquals(4L, store.appliedRemote?.serverVersion)
        assertEquals(1, remote.pushCount)
    }

    @Test
    fun `network error keeps mutation and requests retry`() = runBlocking {
        val store = FakeProfileLocalStore(mutation())
        val remote = FakeRemoteDataSource(userId = USER_ID, pushError = IllegalStateException("secret"))

        val result = ProfileSyncEngine(store, remote, EpochClock { 100L }).run()

        assertEquals(ProfileSyncRunResult.RETRY, result)
        assertTrue(store.pending)
        assertNotNull(store.retryError)
        assertFalse(store.retryError.orEmpty().contains("secret"))
    }

    @Test
    fun `version conflict blocks overwrite and stores remote snapshot`() = runBlocking {
        val store = FakeProfileLocalStore(mutation())
        val remotePayload = payload("Másik eszköz")
        val remote = FakeRemoteDataSource(
            userId = USER_ID,
            pushResult = RemoteProfileSyncResult.Conflict(8L, remotePayload),
        )

        val result = ProfileSyncEngine(store, remote, EpochClock { 100L }).run()

        assertEquals(ProfileSyncRunResult.CONFLICT, result)
        assertEquals(8L, store.conflict?.serverVersion)
        assertEquals("Másik eszköz", store.conflict?.payload?.displayName)
        assertTrue(store.pending)
    }

    @Test
    fun `clean cache accepts newer cloud snapshot`() = runBlocking {
        val store = FakeProfileLocalStore(null)
        val remote = FakeRemoteDataSource(
            userId = USER_ID,
            pullResult = RemoteProfileSnapshot(3L, payload("Távoli profil")),
        )

        val result = ProfileSyncEngine(store, remote, EpochClock { 100L }).run()

        assertEquals(ProfileSyncRunResult.COMPLETE, result)
        assertEquals("Távoli profil", store.appliedRemote?.payload?.displayName)
        assertEquals(0, remote.pushCount)
    }

    private class FakeProfileLocalStore(initialMutation: PendingProfileMutation?) : ProfileLocalStore {
        private var mutation = initialMutation
        var pending = initialMutation != null
        var syncing = false
        var retryError: String? = null
        var completed: RemoteProfileSnapshot? = null
        var conflict: RemoteProfileSnapshot? = null
        var appliedRemote: RemoteProfileSnapshot? = null

        override fun observe(userId: String): Flow<ProfileRepositoryState> = flowOf(ProfileRepositoryState())

        override suspend fun save(
            userId: String,
            profile: ContactProfile,
            queueForCloudSync: Boolean,
            operationId: String,
            nowEpochMs: Long,
        ) = Unit

        override suspend fun dueMutation(userId: String, nowEpochMs: Long): PendingProfileMutation? =
            mutation?.takeIf { !syncing }

        override suspend fun hasPendingMutation(userId: String): Boolean = pending

        override suspend fun hasRetryableMutation(userId: String): Boolean = pending && conflict == null

        override suspend fun queueUnsyncedSnapshotIfNeeded(
            userId: String,
            operationId: String,
            nowEpochMs: Long,
        ): Boolean = false

        override suspend fun markSyncing(mutation: PendingProfileMutation, nowEpochMs: Long): Boolean {
            syncing = true
            return true
        }

        override suspend fun markRetry(
            mutation: PendingProfileMutation,
            nowEpochMs: Long,
            safeError: String,
        ): Boolean {
            retryError = safeError
            syncing = false
            pending = true
            return true
        }

        override suspend fun markConflict(
            mutation: PendingProfileMutation,
            remote: RemoteProfileSnapshot,
            nowEpochMs: Long,
        ): Boolean {
            conflict = remote
            pending = true
            return true
        }

        override suspend fun completeSync(
            mutation: PendingProfileMutation,
            remote: RemoteProfileSnapshot,
            nowEpochMs: Long,
        ): Boolean {
            completed = remote
            this.mutation = null
            pending = false
            return true
        }

        override suspend fun applyRemoteIfClean(
            userId: String,
            remote: RemoteProfileSnapshot,
            nowEpochMs: Long,
        ): Boolean {
            appliedRemote = remote
            return true
        }

        override suspend fun retryNow(userId: String, nowEpochMs: Long): Boolean = pending

        override suspend fun deleteUserData(userId: String) {
            mutation = null
            pending = false
        }
    }

    private class FakeRemoteDataSource(
        private val userId: String?,
        private val pushResult: RemoteProfileSyncResult = RemoteProfileSyncResult.SessionUnavailable,
        private val pullResult: RemoteProfileSnapshot? = null,
        private val pushError: Throwable? = null,
    ) : ProfileRemoteDataSource {
        var pushCount = 0

        override fun authenticatedUserId(): String? = userId

        override suspend fun push(mutation: PendingProfileMutation): RemoteProfileSyncResult {
            pushCount += 1
            pushError?.let { throw it }
            return pushResult
        }

        override suspend fun pull(userId: String): RemoteProfileSnapshot? = pullResult
    }

    private companion object {
        const val USER_ID = "6bc9b40d-5317-4f78-a1d6-c44d50f4e4f4"

        fun payload(name: String) = ProfileSyncPayload(displayName = name)

        fun mutation() = PendingProfileMutation(
            queueKey = "profile:$USER_ID",
            operationId = "2d6c5880-ecea-48e6-af8a-f9e5a2440dc8",
            userId = USER_ID,
            payload = payload("Helyi profil"),
            baseServerVersion = 2L,
            attemptCount = 0,
        )
    }
}
