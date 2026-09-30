package hu.rayworks.vizit

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.account.*
import hu.rayworks.vizit.data.card.CardColorway
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.local.*
import hu.rayworks.vizit.data.remote.AccountProfile
import hu.rayworks.vizit.data.sync.*
import kotlinx.coroutines.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.util.UUID

/** Actual Android SQLite, with a controlled remote so races are reproducible. */
@RunWith(AndroidJUnit4::class)
class AccountProfileInstrumentedTest {
    private lateinit var db: VizitDatabase
    private lateinit var remote: ProfileTestRemote
    private lateinit var repo: AccountProfileRepository
    @Before fun setup() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), VizitDatabase::class.java).build()
        remote = ProfileTestRemote()
        repo = repository(db, remote)
    }
    @After fun cleanup() { db.close() }
    private fun repository(database: VizitDatabase, api: ProfileTestRemote) = AccountProfileRepository(
        database.accountProfileDao(), api, object : ProfileSyncScheduler {
            override fun enqueue() {}
            override fun cancel() {}
        }, automaticSync = { true }, clock = EpochClock { 1000 },
    )
    private suspend fun seed() {
        remote.profiles += listOf(testRemoteProfile("one", "Céges", true), testRemoteProfile("one", "Magán"))
        remote.owner = "one"; repo.refresh("one")
    }

    @Test fun separateProfilesHaveSeparateContentOutboxAndPresentation() = runBlocking {
        seed()
        val a = repo.list("one")[0]; val b = repo.list("one")[1]
        repo.save("one", a.id, a.profile.copy(fullName = "Céges szerkesztve"), CardPresentation(colorway = CardColorway.BRAND))
        repo.save("one", b.id, b.profile.copy(fullName = "Magán szerkesztve"), CardPresentation(colorway = CardColorway.EMERALD))
        val stored = repo.list("one").associateBy { it.id }
        assertNotEquals(stored[a.id]!!.operationId, stored[b.id]!!.operationId)
        assertEquals("Céges szerkesztve", stored[a.id]!!.profile.fullName)
        assertEquals(CardColorway.EMERALD, stored[b.id]!!.presentation.colorway)
        repo.select("one", b.id)
        assertEquals(ProfileSyncRunResult.COMPLETE, repo.sync())
        assertEquals(setOf(a.id, b.id), remote.writes.map { it.profileId }.toSet())
        assertEquals(b.id, repo.selection("one"))
        assertTrue(remote.profiles.first { it.id == a.id }.isDefault)
        assertTrue(repo.list("one").all { it.operationId == null })
    }

    @Test fun anotherAccountCannotReadOrSendTheFirstAccountsQueue() = runBlocking {
        seed(); val a = repo.list("one").first()
        repo.save("one", a.id, a.profile.copy(fullName = "Saját módosítás"), a.presentation)
        remote.profiles += testRemoteProfile("two", "Másik fiók", true)
        remote.owner = "two"; repo.refresh("two")
        assertEquals(1, repo.list("two").size)
        assertEquals(ProfileSyncRunResult.COMPLETE, repo.sync())
        assertTrue(remote.writes.isEmpty())
        assertNotNull(repo.list("one").first { it.id == a.id }.operationId)
        try {
            repo.save("two", a.id, a.profile, a.presentation)
            fail("A foreign profile ID must be rejected")
        } catch (_: IllegalArgumentException) { }
        remote.owner = "one"; repo.sync()
        assertEquals("one", remote.writes.single().userId)
        assertEquals(a.id, remote.writes.single().profileId)
    }

    @Test fun delayedSaveResponseNeverClearsANewerEditOrTouchesAnotherProfile() = runBlocking {
        seed(); val a = repo.list("one")[0]; val b = repo.list("one")[1]
        repo.save("one", a.id, a.profile.copy(fullName = "Első változat"), a.presentation)
        remote.pushGate = CompletableDeferred()
        val job = async(Dispatchers.Default) { repo.sync() }
        remote.pushStarted.await()
        repo.select("one", b.id)
        repo.save("one", a.id, a.profile.copy(fullName = "Legújabb változat"), a.presentation)
        val newest = repo.list("one").first { it.id == a.id }.operationId
        remote.pushGate!!.complete(Unit); job.await()
        val pending = repo.list("one").first { it.id == a.id }
        assertEquals(newest, pending.operationId)
        assertEquals("Legújabb változat", pending.profile.fullName)
        assertEquals(2L, pending.serverVersion)
        assertEquals(b.profile, repo.list("one").first { it.id == b.id }.profile)
        assertEquals(b.id, repo.selection("one"))
        remote.pushGate = null; repo.sync()
        assertEquals("Legújabb változat", repo.list("one").first { it.id == a.id }.profile.fullName)
        assertNull(repo.list("one").first { it.id == a.id }.operationId)
    }

    @Test fun completeCacheAndSelectionSurviveAnAppRestart() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val filename = "profile-restart-${UUID.randomUUID()}.db"
        try {
            val first = Room.databaseBuilder(context, VizitDatabase::class.java, filename).build()
            val original = repository(first, remote)
            remote.owner = "one"
            remote.profiles += listOf(testRemoteProfile("one", "Cég", true), testRemoteProfile("one", "Magán"))
            original.refresh("one")
            val active = original.list("one").last()
            original.select("one", active.id)
            original.save("one", active.id, active.profile.copy(fullName = "Offline mentés"), active.presentation)
            first.close()
            val reopened = Room.databaseBuilder(context, VizitDatabase::class.java, filename).build()
            val restored = repository(reopened, remote)
            assertEquals(active.id, restored.selection("one"))
            assertEquals("Offline mentés", restored.list("one").first { it.id == active.id }.profile.fullName)
            assertNotNull(restored.list("one").first { it.id == active.id }.operationId)
            assertEquals(2, restored.list("one").size)
            reopened.close()
        } finally { context.deleteDatabase(filename) }
    }

    @Test fun upgradeRetainsLegacySnapshotsWithoutSendingAmbiguousQueuedWrites() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val filename = "profile-migration-${UUID.randomUUID()}.db"
        try {
            val old = Room.databaseBuilder(context, VizitDatabase::class.java, filename).build()
            RoomProfileStore(old.profileDao()).save("one", ContactProfile(fullName = "Korábbi helyi adat", phone = "123"), true, "legacy", 1)
            old.openHelper.writableDatabase.execSQL("DROP TABLE account_profile_cache")
            old.openHelper.writableDatabase.execSQL("DROP TABLE account_profile_selection")
            old.openHelper.writableDatabase.version = 3
            old.close()
            val upgraded = Room.databaseBuilder(context, VizitDatabase::class.java, filename)
                .addMigrations(VizitDatabase.MIGRATION_3_4).build()
            val upgradedRepo = repository(upgraded, remote)
            remote.owner = "one"; remote.profiles += testRemoteProfile("one", "Felhőprofil", true)
            upgradedRepo.refresh("one"); upgradedRepo.sync()
            assertEquals("Korábbi helyi adat", upgraded.profileDao().getSnapshot("one")!!.profile.displayName)
            assertEquals("legacy", upgraded.profileDao().getOutbox("one")!!.operationId)
            assertEquals("Felhőprofil", upgradedRepo.list("one").single().profile.fullName)
            assertTrue(remote.writes.isEmpty())
            upgraded.close()
        } finally { context.deleteDatabase(filename) }
    }
}

internal fun testRemoteProfile(owner: String, name: String, default: Boolean = false, photo: String = ""): AccountProfile {
    val id = UUID.randomUUID().toString()
    val payload = ProfileSyncPayload(displayName = name, publicSlug = "profile-${id.take(8)}", photoBase64 = photo,
        isPublic = true, contacts = listOf(ProfileContactPayload("phone-$id", "phone", "Telefon", "123", 0, true),
            ProfileContactPayload("email-$id", "email", "E-mail", "$id@example.test", 1, true)))
    val owned = OwnedProfile(owner, id, payload, 1)
    return AccountProfile(id, name, payload.publicSlug!!, default, owned.profile, owner,
        RemoteProfileSnapshot(1, payload), "2026-10-01T00:00:${if (default) "00" else "01"}Z")
}

internal class ProfileTestRemote : AccountProfileRemote {
    var owner = "one"
    val profiles = mutableListOf<AccountProfile>()
    val writes = mutableListOf<PendingProfileMutation>()
    var pushGate: CompletableDeferred<Unit>? = null
    val pushStarted = CompletableDeferred<Unit>()
    override fun ownerId() = owner
    override suspend fun list(ownerId: String) = profiles.filter { it.ownerId == ownerId }
    override suspend fun create(ownerId: String, profile: ContactProfile, makeDefault: Boolean): AccountProfile {
        val created = testRemoteProfile(ownerId, profile.fullName, makeDefault, profile.photoBase64)
        profiles += created
        return created
    }
    override suspend fun delete(ownerId: String, profileId: String) { profiles.removeAll { it.ownerId == ownerId && it.id == profileId } }
    override suspend fun push(mutation: PendingProfileMutation): RemoteProfileSyncResult {
        check(mutation.userId == owner)
        val old = profiles.first { it.ownerId == owner && it.id == mutation.profileId }
        writes += mutation
        pushStarted.complete(Unit); pushGate?.await()
        val saved = mutation.payload.copy(baseFingerprint = null)
        val index = profiles.indexOfFirst { it.ownerId == mutation.userId && it.id == mutation.profileId }
        profiles[index] = old.copy(snapshot = RemoteProfileSnapshot(old.snapshot.serverVersion + 1, saved))
        return RemoteProfileSyncResult.Applied(old.snapshot.serverVersion + 1, saved)
    }
}
