package hu.rayworks.vizit.data.account

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.card.CardPresentation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Account lifetime and local profile selection. No swipe makes an API request. */
class AccountProfileSession(private val store: AccountProfileStore, private val scope: CoroutineScope) {
    private val mutableState = MutableStateFlow(AccountProfileState())
    val state: StateFlow<AccountProfileState> = mutableState
    private var epoch = 0L
    private var loadJob: Job? = null
    private var observationJob: Job? = null
    private val selectionWrites = Mutex()

    fun bind(ownerId: String) {
        if (state.value.ownerId == ownerId) return
        unbind()
        mutableState.value = AccountProfileState(ownerId = ownerId)
        val token = epoch
        loadJob = scope.launch {
            try {
                val cached = store.list(ownerId)
                val preferred = store.selection(ownerId)
                if (!current(ownerId, token)) return@launch
                publish(cached, preferred, ready = cached.isNotEmpty())
                observationJob = scope.launch {
                    store.observe(ownerId).collect { profiles ->
                        if (current(ownerId, token)) publish(profiles, state.value.activeId,
                            ready = state.value.catalogVerified || profiles.isNotEmpty())
                    }
                }
                refresh(ownerId, token)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                fail(ownerId, token, failure)
            }
        }
    }

    fun unbind() {
        epoch++
        loadJob?.cancel()
        observationJob?.cancel()
        loadJob = null
        observationJob = null
        mutableState.value = AccountProfileState()
    }

    fun retryLoad() {
        val owner = state.value.ownerId ?: return
        val token = epoch
        loadJob?.cancel()
        mutableState.value = state.value.copy(loadStatus = if (state.value.profiles.isEmpty())
            AccountProfileLoadStatus.LOADING else AccountProfileLoadStatus.READY, message = null)
        loadJob = scope.launch { refresh(owner, token) }
    }

    fun select(profileId: String) {
        val before = state.value
        val owner = before.ownerId ?: return
        if (before.profiles.none { it.ownerId == owner && it.id == profileId }) {
            mutableState.value = before.copy(message = "A profil nem ehhez a fiókhoz tartozik.")
            return
        }
        if (before.activeId == profileId) return
        mutableState.value = before.copy(activeId = profileId)
        val token = epoch
        scope.launch {
            selectionWrites.withLock {
                if (current(owner, token) && state.value.activeId == profileId) {
                    try { store.select(owner, profileId) }
                    catch (cancelled: CancellationException) { throw cancelled }
                    catch (_: Exception) {
                        if (current(owner, token)) mutableState.value = state.value.copy(message = "A kiválasztás nem menthető helyben.")
                    }
                }
            }
        }
    }

    suspend fun save(profileId: String, profile: ContactProfile, presentation: CardPresentation): String? {
        val owner = state.value.ownerId ?: return "Jelentkezz be újra."
        val token = epoch
        if (state.value.profiles.none { it.ownerId == owner && it.id == profileId }) return "A profil nem ehhez a fiókhoz tartozik."
        return try {
            store.save(owner, profileId, profile, presentation)
            if (!current(owner, token)) return "A munkamenet megváltozott."
            publish(store.list(owner), state.value.activeId, ready = true)
            null
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { failure.localizedMessage ?: "A profil nem menthető." }
    }

    suspend fun create(profile: ContactProfile, presentation: CardPresentation): String? {
        val before = state.value
        val owner = before.ownerId ?: return "Jelentkezz be újra."
        if (!before.catalogVerified || before.operationBusy) return "Várd meg a profillista betöltését."
        val token = epoch
        mutableState.value = before.copy(operationBusy = true, message = null)
        return try {
            val created = store.create(owner, profile, presentation, makeDefault = before.profiles.isEmpty())
            if (!current(owner, token)) return "A munkamenet megváltozott."
            check(created.ownerId == owner)
            store.select(owner, created.id)
            if (!current(owner, token)) return "A munkamenet megváltozott."
            publish(store.list(owner), created.id, ready = true)
            null
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { failure.localizedMessage ?: "Az új profil nem hozható létre." }
        finally {
            if (current(owner, token)) mutableState.value = state.value.copy(operationBusy = false)
        }
    }

    fun deleteActive(profileId: String? = state.value.activeId) {
        val before = state.value
        val active = before.profiles.firstOrNull { it.id == profileId && it.ownerId == before.ownerId } ?: return
        if (before.operationBusy) return
        val token = epoch
        mutableState.value = before.copy(operationBusy = true, message = null)
        scope.launch {
            try {
                store.delete(active.ownerId, active.id)
                if (!current(active.ownerId, token)) return@launch
                val remaining = store.list(active.ownerId)
                val next = remaining.firstOrNull()?.id
                if (next != null) store.select(active.ownerId, next)
                if (current(active.ownerId, token)) {
                    publish(remaining, next, ready = true)
                    mutableState.value = state.value.copy(message = "A profil törölve.")
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { fail(active.ownerId, token, failure) }
            finally {
                if (current(active.ownerId, token)) mutableState.value = state.value.copy(operationBusy = false)
            }
        }
    }

    fun present(profileId: String, presentation: CardPresentation) {
        val before = state.value
        val owner = before.ownerId ?: return
        if (before.profiles.none { it.ownerId == owner && it.id == profileId }) return
        mutableState.value = before.copy(profiles = before.profiles.map {
            if (it.id == profileId) it.copy(presentation = presentation) else it
        })
        val token = epoch
        scope.launch {
            try { store.present(owner, profileId, presentation) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { fail(owner, token, failure) }
        }
    }

    fun retrySync() = activeOperation { profile -> store.retry(profile.ownerId, profile.id) }
    fun resolve(keepLocal: Boolean) = activeOperation { profile ->
        if (!store.resolve(profile.ownerId, profile.id, keepLocal)) error("A profil közben megváltozott. Ellenőrizd újra.")
    }
    fun clearMessage() { mutableState.value = state.value.copy(message = null) }

    private fun activeOperation(block: suspend (OwnedProfile) -> Unit) {
        val active = state.value.active ?: return
        val token = epoch
        scope.launch {
            try { block(active) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { fail(active.ownerId, token, failure) }
        }
    }

    private suspend fun refresh(owner: String, token: Long) {
        try {
            store.refresh(owner)
            if (!current(owner, token)) return
            val profiles = store.list(owner)
            val preferred = state.value.activeId ?: store.selection(owner)
            if (!current(owner, token)) return
            publish(profiles, preferred, ready = true, verified = true)
            state.value.activeId?.let { store.select(owner, it) }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { fail(owner, token, failure) }
    }

    private fun publish(profiles: List<OwnedProfile>, preferred: String?, ready: Boolean, verified: Boolean = state.value.catalogVerified) {
        val before = state.value
        check(profiles.all { it.ownerId == before.ownerId })
        val ordered = profiles.sortedWith(compareBy(OwnedProfile::createdAt, OwnedProfile::id))
        val active = preferred?.takeIf { id -> ordered.any { it.id == id } }
            ?: ordered.firstOrNull { it.isDefault }?.id ?: ordered.firstOrNull()?.id
        mutableState.value = before.copy(profiles = ordered, activeId = active,
            catalogVerified = verified,
            loadStatus = if (ready) AccountProfileLoadStatus.READY else before.loadStatus)
    }

    private fun fail(owner: String, token: Long, failure: Exception) {
        if (!current(owner, token)) return
        mutableState.value = state.value.copy(loadStatus = if (state.value.profiles.isEmpty())
            AccountProfileLoadStatus.UNAVAILABLE else AccountProfileLoadStatus.READY,
            message = failure.localizedMessage ?: "A profillista most nem tölthető be.")
    }
    private fun current(owner: String, token: Long) = epoch == token && state.value.ownerId == owner
}
