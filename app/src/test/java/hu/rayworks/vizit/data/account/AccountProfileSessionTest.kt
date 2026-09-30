package hu.rayworks.vizit.data.account

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.sync.ProfileSyncPayload
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AccountProfileSessionTest {
    private fun profile(owner: String, id: String, name: String, default: Boolean = false) =
        OwnedProfile(owner, id, ProfileSyncPayload(displayName = name, publicSlug = id), 1, isDefault = default)

    @Test fun swipingIsLocalAndEverySaveKeepsItsTarget() = runTest {
        val store = FakeStore()
        store.rows["account"] = MutableStateFlow(listOf(profile("account", "company", "Céges", true), profile("account", "private", "Magán")))
        val session = AccountProfileSession(store, backgroundScope)
        session.bind("account"); runCurrent()
        repeat(20) { session.select("private"); session.select("company") }
        session.select("private"); runCurrent()
        assertEquals("account", session.state.value.ownerId)
        assertEquals("Magán", session.state.value.active?.profile?.fullName)
        assertEquals(1, store.refreshes)
        assertNull(session.save("company", ContactProfile(fullName = "Céges új név", phone = "123"), CardPresentation()))
        assertEquals("account/company", store.saved.single())
        assertEquals("private", session.state.value.activeId)
        assertNull(session.create(ContactProfile(fullName = "Harmadik", phone = "456"), CardPresentation()))
        assertEquals("account", session.state.value.active?.ownerId)
        assertEquals("Harmadik", session.state.value.active?.profile?.fullName)
        assertFalse(store.createdDefault)
        assertFalse(session.state.value.needsFirstProfile)
    }

    @Test fun foreignIdCannotBeSelectedOrSaved() = runTest {
        val store = FakeStore()
        store.rows["one"] = MutableStateFlow(listOf(profile("one", "own", "Saját")))
        store.rows["two"] = MutableStateFlow(listOf(profile("two", "foreign", "Másik")))
        val session = AccountProfileSession(store, backgroundScope)
        session.bind("one"); runCurrent()
        session.select("foreign")
        assertEquals("own", session.state.value.activeId)
        assertNotNull(session.save("foreign", ContactProfile(fullName = "Tiltott", phone = "123"), CardPresentation()))
        assertTrue(store.saved.isEmpty())
    }

    @Test fun noWizardUntilTheCatalogHasConfirmedAnEmptyAccount() = runTest {
        val store = FakeStore(); val gate = CompletableDeferred<Unit>()
        store.refreshGate = gate
        val session = AccountProfileSession(store, backgroundScope)
        session.bind("empty"); runCurrent()
        assertEquals(AccountProfileLoadStatus.LOADING, session.state.value.loadStatus)
        assertFalse(session.state.value.needsFirstProfile)
        gate.complete(Unit); runCurrent()
        assertTrue(session.state.value.needsFirstProfile)
        assertNull(session.create(ContactProfile(fullName = "Első", phone = "123"), CardPresentation()))
        assertTrue(store.createdDefault)
        assertFalse(session.state.value.needsFirstProfile)
    }

    @Test fun networkFailureCannotOpenTheFirstProfileWizard() = runTest {
        val store = FakeStore(); store.failRefresh = true
        val session = AccountProfileSession(store, backgroundScope)
        session.bind("empty"); runCurrent()
        assertEquals(AccountProfileLoadStatus.UNAVAILABLE, session.state.value.loadStatus)
        assertFalse(session.state.value.needsFirstProfile)
    }

    @Test fun lateOldAccountResponseCannotReplaceTheNewAccount() = runTest {
        val store = FakeStore(); val oldResponse = CompletableDeferred<Unit>()
        store.rows["old"] = MutableStateFlow(listOf(profile("old", "old-profile", "Régi")))
        store.rows["new"] = MutableStateFlow(listOf(profile("new", "new-profile", "Új")))
        store.refreshGate = oldResponse
        val session = AccountProfileSession(store, backgroundScope)
        session.bind("old"); runCurrent()
        store.refreshGate = null
        session.bind("new"); runCurrent()
        oldResponse.complete(Unit); runCurrent()
        assertEquals("new", session.state.value.ownerId)
        assertEquals("Új", session.state.value.active?.profile?.fullName)
        assertTrue(session.state.value.profiles.all { it.ownerId == "new" })
    }

    @Test fun savedSelectionSurvivesRebindWithoutChangingTheServerDefault() = runTest {
        val store = FakeStore()
        store.rows["one"] = MutableStateFlow(listOf(profile("one", "first", "Első", true), profile("one", "second", "Második")))
        val session = AccountProfileSession(store, backgroundScope)
        session.bind("one"); runCurrent(); session.select("second"); runCurrent()
        session.unbind(); session.bind("one"); runCurrent()
        assertEquals("second", session.state.value.activeId)
        assertTrue(session.state.value.profiles.first { it.id == "first" }.isDefault)
    }

    private class FakeStore : AccountProfileStore {
        val rows = mutableMapOf<String, MutableStateFlow<List<OwnedProfile>>>()
        val selected = mutableMapOf<String, String>()
        val saved = mutableListOf<String>()
        var refreshGate: CompletableDeferred<Unit>? = null
        var failRefresh = false
        var refreshes = 0
        var createdDefault = false
        fun flow(owner: String) = rows.getOrPut(owner) { MutableStateFlow(emptyList()) }
        override fun observe(ownerId: String) = flow(ownerId)
        override suspend fun list(ownerId: String) = flow(ownerId).value
        override suspend fun selection(ownerId: String) = selected[ownerId]
        override suspend fun select(ownerId: String, profileId: String) { selected[ownerId] = profileId }
        override suspend fun refresh(ownerId: String) {
            refreshes++
            val gate = refreshGate
            if (gate != null) withContext(NonCancellable) { gate.await() }
            if (failRefresh) error("offline")
        }
        override suspend fun save(ownerId: String, profileId: String, profile: ContactProfile, presentation: CardPresentation) {
            saved += "$ownerId/$profileId"
            flow(ownerId).value = flow(ownerId).value.map { if (it.id == profileId) it.copy(payload = it.payload.copy(displayName = profile.fullName)) else it }
        }
        override suspend fun create(ownerId: String, profile: ContactProfile, presentation: CardPresentation, makeDefault: Boolean): OwnedProfile {
            createdDefault = makeDefault
            val created = OwnedProfile(ownerId, "created", ProfileSyncPayload(displayName = profile.fullName), 1)
            flow(ownerId).value += created
            return created
        }
        override suspend fun delete(ownerId: String, profileId: String) { flow(ownerId).value = flow(ownerId).value.filter { it.id != profileId } }
        override suspend fun present(ownerId: String, profileId: String, presentation: CardPresentation) {}
        override suspend fun retry(ownerId: String, profileId: String) {}
        override suspend fun resolve(ownerId: String, profileId: String, keepLocal: Boolean) = true
    }
}
