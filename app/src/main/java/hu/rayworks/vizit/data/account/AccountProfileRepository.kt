package hu.rayworks.vizit.data.account

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.ContactProfileValidator
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.remote.AccountProfile
import hu.rayworks.vizit.data.remote.LegacyProfileCodec
import hu.rayworks.vizit.data.remote.NodeProfileCatalog
import hu.rayworks.vizit.data.remote.NodeProfileRemoteDataSource
import hu.rayworks.vizit.data.sync.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

interface AccountProfileStore {
    fun observe(ownerId: String): Flow<List<OwnedProfile>>
    suspend fun list(ownerId: String): List<OwnedProfile>
    suspend fun selection(ownerId: String): String?
    suspend fun select(ownerId: String, profileId: String)
    suspend fun refresh(ownerId: String)
    suspend fun save(ownerId: String, profileId: String, profile: ContactProfile, presentation: CardPresentation)
    suspend fun create(ownerId: String, profile: ContactProfile, presentation: CardPresentation, makeDefault: Boolean): OwnedProfile
    suspend fun delete(ownerId: String, profileId: String)
    suspend fun present(ownerId: String, profileId: String, presentation: CardPresentation)
    suspend fun retry(ownerId: String, profileId: String)
    suspend fun resolve(ownerId: String, profileId: String, keepLocal: Boolean): Boolean
}

interface AccountProfileRemote {
    fun ownerId(): String?
    suspend fun list(ownerId: String): List<AccountProfile>
    suspend fun create(ownerId: String, profile: ContactProfile, makeDefault: Boolean): AccountProfile
    suspend fun delete(ownerId: String, profileId: String)
    suspend fun push(mutation: PendingProfileMutation): RemoteProfileSyncResult
}

class NodeAccountProfileRemote(
    private val catalog: NodeProfileCatalog,
    private val content: NodeProfileRemoteDataSource,
) : AccountProfileRemote {
    override fun ownerId() = content.authenticatedUserId()
    override suspend fun list(ownerId: String) = catalog.list(ownerId)
    override suspend fun create(ownerId: String, profile: ContactProfile, makeDefault: Boolean) =
        catalog.create(ownerId, profile, makeDefault)
    override suspend fun delete(ownerId: String, profileId: String) = catalog.delete(ownerId, profileId)
    override suspend fun push(mutation: PendingProfileMutation) = content.push(mutation)
}

/** Every cache entry and queued mutation is addressed by BOTH the authenticated owner and profile ID. */
class AccountProfileRepository(
    private val dao: AccountProfileDao,
    private val remote: AccountProfileRemote,
    private val scheduler: ProfileSyncScheduler,
    private val automaticSync: suspend () -> Boolean,
    private val clock: EpochClock = EpochClock(System::currentTimeMillis),
) : AccountProfileStore {
    private val writes = Mutex()
    private val syncRuns = Mutex()
    override fun observe(ownerId: String) = dao.observe(ownerId).map { rows -> rows.map { it.toOwned() } }
    override suspend fun list(ownerId: String) = dao.list(ownerId).map { it.toOwned() }
    override suspend fun selection(ownerId: String) = dao.selection(ownerId)?.profileId
    override suspend fun select(ownerId: String, profileId: String) {
        requireNotNull(dao.get(ownerId, profileId)) { "A profil nem ehhez a fiókhoz tartozik." }
        dao.select(AccountProfileSelection(ownerId, profileId))
    }

    override suspend fun refresh(ownerId: String) {
        checkOwner(ownerId)
        val profiles = remote.list(ownerId)
        checkOwner(ownerId)
        require(profiles.all { it.ownerId == ownerId }) { "A profillista másik fiók rekordját tartalmazza." }
        require(profiles.map { it.id }.distinct().size == profiles.size)
        writes.withLock {
            for (profile in profiles) {
                val previous = dao.get(ownerId, profile.id)?.toOwned()
                if (previous?.operationId != null || profile.snapshot.serverVersion < (previous?.serverVersion ?: 0)) continue
                val saved = profile.owned(previous?.presentation ?: CardPresentation())
                dao.put(saved.copy(payload = saved.payload.copy(
                    photoBase64 = saved.payload.photoBase64 ?: previous?.payload?.photoBase64,
                    logoPath = saved.payload.logoPath ?: previous?.payload?.logoPath,
                )).toEntity())
            }
            val liveIds = profiles.map { it.id }.toSet()
            for (cached in list(ownerId).filter { it.id !in liveIds }) {
                if (cached.operationId == null) dao.delete(ownerId, cached.id)
                else dao.put(cached.copy(status = ProfileSyncStatus.CONFLICT,
                    conflict = RemoteProfileSnapshot(0, ProfileSyncPayload()),
                    lastError = "Ezt a profilt időközben törölték.").toEntity())
            }
        }
        if (automaticSync() && list(ownerId).any { it.operationId != null }) scheduler.enqueue()
    }

    override suspend fun save(ownerId: String, profileId: String, profile: ContactProfile, presentation: CardPresentation) {
        require(ContactProfileValidator.validate(profile) == null) { ContactProfileValidator.validate(profile).orEmpty() }
        writes.withLock {
            val previous = requireNotNull(dao.get(ownerId, profileId)) { "A profil nem ehhez a fiókhoz tartozik." }.toOwned()
            check(previous.status != ProfileSyncStatus.CONFLICT) { "Előbb oldd fel a profil szinkronütközését." }
            val local = ProfileSnapshotMapper.toLocalSnapshot("$ownerId/$profileId", previous.payload, null, 0)
            val edited = ProfileSnapshotMapper.toLocalSnapshot("$ownerId/$profileId", profile, local, clock.nowEpochMs(), true)
            val payload = ProfileSnapshotMapper.toPayload(edited).copy(
                baseFingerprint = previous.payload.baseFingerprint.takeIf { previous.operationId != null }
                    ?: LegacyProfileCodec.fingerprint(previous.payload),
            )
            dao.put(previous.copy(payload = payload, presentation = presentation,
                status = ProfileSyncStatus.PENDING, operationId = UUID.randomUUID().toString(),
                nextAttemptAtEpochMs = clock.nowEpochMs(), attemptCount = 0, lastError = null).toEntity())
        }
        if (automaticSync()) scheduler.enqueue()
    }

    override suspend fun create(ownerId: String, profile: ContactProfile, presentation: CardPresentation, makeDefault: Boolean): OwnedProfile {
        checkOwner(ownerId)
        val result = remote.create(ownerId, profile, makeDefault)
        checkOwner(ownerId)
        check(result.ownerId == ownerId)
        val saved = result.owned(presentation)
        writes.withLock { dao.put(saved.toEntity()) }
        return saved
    }

    override suspend fun delete(ownerId: String, profileId: String) {
        checkOwner(ownerId)
        requireNotNull(dao.get(ownerId, profileId))
        remote.delete(ownerId, profileId)
        checkOwner(ownerId)
        writes.withLock { dao.delete(ownerId, profileId) }
    }

    override suspend fun present(ownerId: String, profileId: String, presentation: CardPresentation) = writes.withLock {
        val current = requireNotNull(dao.get(ownerId, profileId)).toOwned()
        dao.put(current.copy(presentation = presentation).toEntity())
    }

    override suspend fun retry(ownerId: String, profileId: String) {
        writes.withLock {
            val current = dao.get(ownerId, profileId)?.toOwned() ?: return@withLock
            if (current.operationId == null || current.status == ProfileSyncStatus.CONFLICT) return@withLock
            dao.put(current.copy(status = ProfileSyncStatus.PENDING, nextAttemptAtEpochMs = clock.nowEpochMs(), lastError = null).toEntity())
        }
        scheduler.enqueue()
    }

    override suspend fun resolve(ownerId: String, profileId: String, keepLocal: Boolean): Boolean {
        val changed = writes.withLock {
            val current = dao.get(ownerId, profileId)?.toOwned() ?: return@withLock false
            val conflict = current.conflict ?: return@withLock false
            // A deleted profile can only be recreated by an explicit new-profile operation.
            if (conflict.serverVersion == 0L) {
                if (keepLocal) return@withLock false
                dao.delete(ownerId, profileId)
                return@withLock true
            }
            val resolved = if (keepLocal) current.copy(
                serverVersion = conflict.serverVersion,
                payload = current.payload.copy(baseFingerprint = LegacyProfileCodec.fingerprint(conflict.payload)),
                operationId = UUID.randomUUID().toString(), status = ProfileSyncStatus.PENDING,
                nextAttemptAtEpochMs = clock.nowEpochMs(), attemptCount = 0, conflict = null, lastError = null,
            ) else current.copy(serverVersion = conflict.serverVersion, payload = conflict.payload,
                operationId = null, status = ProfileSyncStatus.SYNCED, conflict = null, lastError = null)
            dao.put(resolved.toEntity())
            true
        }
        if (changed && keepLocal) scheduler.enqueue()
        return changed
    }

    suspend fun deleteAccount(ownerId: String) = writes.withLock {
        dao.deleteAccount(ownerId)
        dao.deleteSelection(ownerId)
    }

    /** Background work never reads or changes the UI's selected profile. */
    suspend fun sync(): ProfileSyncRunResult = syncRuns.withLock {
        val ownerId = remote.ownerId() ?: return@withLock ProfileSyncRunResult.WAITING_FOR_SESSION
        var outcome = ProfileSyncRunResult.COMPLETE
        for (queued in list(ownerId).filter { it.operationId != null && it.status != ProfileSyncStatus.CONFLICT && it.nextAttemptAtEpochMs <= clock.nowEpochMs() }) {
            if (remote.ownerId() != ownerId) return@withLock ProfileSyncRunResult.WAITING_FOR_SESSION
            val operation = requireNotNull(queued.operationId)
            val mutation = PendingProfileMutation("$ownerId/${queued.id}", operation, ownerId,
                queued.payload, queued.serverVersion, queued.attemptCount, queued.id)
            if (!updateCurrent(queued, operation) { it.copy(status = ProfileSyncStatus.SYNCING) }) continue
            try {
                val result = remote.push(mutation)
                if (remote.ownerId() != ownerId) {
                    updateCurrent(queued, operation) { it.copy(status = ProfileSyncStatus.PENDING) }
                    return@withLock ProfileSyncRunResult.WAITING_FOR_SESSION
                }
                when (result) {
                    is RemoteProfileSyncResult.Applied -> complete(queued, result)
                    is RemoteProfileSyncResult.Conflict -> {
                        updateCurrent(queued, operation) { it.copy(status = ProfileSyncStatus.CONFLICT,
                            conflict = RemoteProfileSnapshot(result.serverVersion, result.serverSnapshot),
                            lastError = "A profil a felhőben is módosult. Válassz a két változat közül.") }
                        outcome = ProfileSyncRunResult.CONFLICT
                    }
                    RemoteProfileSyncResult.SessionUnavailable -> {
                        updateCurrent(queued, operation) { it.copy(status = ProfileSyncStatus.PENDING) }
                        return@withLock ProfileSyncRunResult.WAITING_FOR_SESSION
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                updateCurrent(queued, operation) { it.copy(status = ProfileSyncStatus.RETRY_SCHEDULED,
                    attemptCount = it.attemptCount + 1,
                    nextAttemptAtEpochMs = clock.nowEpochMs() + SyncRetryPolicy.delayMillis(it.attemptCount),
                    lastError = "A mentés helyben megmaradt. A szinkron később újrapróbálja.") }
                outcome = ProfileSyncRunResult.RETRY
            }
        }
        if (list(ownerId).any { it.operationId != null && it.status != ProfileSyncStatus.CONFLICT })
            ProfileSyncRunResult.RETRY else outcome
    }

    private suspend fun updateCurrent(profile: OwnedProfile, operation: String, change: (OwnedProfile) -> OwnedProfile): Boolean = writes.withLock {
        val current = dao.get(profile.ownerId, profile.id)?.toOwned() ?: return@withLock false
        if (current.operationId != operation) return@withLock false
        dao.put(change(current).toEntity())
        true
    }
    private suspend fun complete(sent: OwnedProfile, result: RemoteProfileSyncResult.Applied) = writes.withLock {
        val current = dao.get(sent.ownerId, sent.id)?.toOwned() ?: return@withLock
        if (current.operationId == sent.operationId) {
            dao.put(current.copy(payload = result.snapshot, serverVersion = result.serverVersion,
                status = ProfileSyncStatus.SYNCED, operationId = null, lastError = null, conflict = null,
                lastSyncedAtEpochMs = clock.nowEpochMs()).toEntity())
        } else if (current.operationId != null && current.conflict == null &&
            current.payload.baseFingerprint == sent.payload.baseFingerprint) {
            // A newer local edit keeps its content and rebases only onto our own confirmed write.
            dao.put(current.copy(serverVersion = result.serverVersion,
                payload = current.payload.copy(baseFingerprint = LegacyProfileCodec.fingerprint(result.snapshot))).toEntity())
        }
    }
    private fun checkOwner(ownerId: String) {
        check(remote.ownerId() == ownerId) { "A munkamenet megváltozott. Jelentkezz be újra." }
    }
    private fun AccountProfile.owned(presentation: CardPresentation) = OwnedProfile(
        ownerId, id, snapshot.payload, snapshot.serverVersion, isDefault, createdAt, presentation,
        lastSyncedAtEpochMs = clock.nowEpochMs(),
    )
}
