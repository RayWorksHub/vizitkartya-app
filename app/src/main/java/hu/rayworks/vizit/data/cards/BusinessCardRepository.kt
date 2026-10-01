package hu.rayworks.vizit.data.cards

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.local.BusinessCardCacheEntity
import hu.rayworks.vizit.data.local.BusinessCardDao
import hu.rayworks.vizit.data.local.BusinessCardSelectionEntity
import hu.rayworks.vizit.data.remote.NodeBusinessCardRemoteDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Account-scoped cache and remote coordinator for cloud business cards.
 *
 * Owner id is part of every local lookup and is re-checked against the active
 * Supabase session before remote data is allowed into the cache. This keeps a
 * previous account's cached cards from ever becoming the next account's state.
 */
class BusinessCardRepository(
    private val dao: BusinessCardDao,
    private val remote: NodeBusinessCardRemoteDataSource,
    private val nowEpochMs: () -> Long = System::currentTimeMillis,
) {
    private val mutationMutex = Mutex()

    fun observe(ownerId: String): Flow<BusinessCardCatalogState> = combine(
        dao.observeCards(ownerId),
        dao.observeSelection(ownerId),
    ) { entities, selection ->
        val cards = entities.mapNotNull { entity ->
            runCatching { entity.toOwnedCard(expectedOwnerId = ownerId) }.getOrNull()
        }
        val selected = selection
            ?.takeIf { it.ownerId == ownerId && cards.any { card -> card.profileId == it.profileId } }
            ?.profileId
            ?: cards.firstOrNull { it.isPrimary }?.profileId
            ?: cards.firstOrNull()?.profileId
        BusinessCardCatalogState(ownerId, cards, selected)
    }

    suspend fun cached(ownerId: String): BusinessCardCatalogState {
        val cards = dao.getCards(ownerId).mapNotNull { entity ->
            runCatching { entity.toOwnedCard(expectedOwnerId = ownerId) }.getOrNull()
        }
        val selected = dao.getSelection(ownerId)
            ?.takeIf { it.ownerId == ownerId && cards.any { card -> card.profileId == it.profileId } }
            ?.profileId
            ?: cards.firstOrNull { it.isPrimary }?.profileId
            ?: cards.firstOrNull()?.profileId
        return BusinessCardCatalogState(ownerId, cards, selected)
    }

    suspend fun refresh(ownerId: String): List<OwnedBusinessCard> = mutationMutex.withLock {
        requireActiveOwner(ownerId)
        val cachedPresentations = dao.getCards(ownerId).associate { entity ->
            entity.profileId to runCatching {
                BusinessCardCacheCodec.decode(entity.payloadJson).presentation
            }.getOrDefault(CardPresentation())
        }
        val cards = remote.list(ownerId).map { card ->
            check(card.ownerId == ownerId)
            card.copy(presentation = cachedPresentations[card.profileId] ?: CardPresentation())
        }
        requireActiveOwner(ownerId)
        dao.replaceOwnerCards(ownerId, cards.map { it.toEntity(nowEpochMs()) })
        cards
    }

    suspend fun select(ownerId: String, profileId: String) = mutationMutex.withLock {
        val card = requireNotNull(dao.getCard(ownerId, profileId)) {
            "A névjegy nem tartozik ehhez a fiókhoz."
        }
        check(card.ownerId == ownerId)
        dao.upsertSelection(BusinessCardSelectionEntity(ownerId, profileId))
    }

    suspend fun save(
        ownerId: String,
        profileId: String?,
        profile: ContactProfile,
        presentation: CardPresentation,
    ): OwnedBusinessCard = mutationMutex.withLock {
        requireActiveOwner(ownerId)
        val existing = profileId?.let { id ->
            requireNotNull(dao.getCard(ownerId, id)) {
                "A névjegy nem tartozik ehhez a fiókhoz."
            }.toOwnedCard(expectedOwnerId = ownerId)
        }
        val saved = remote.save(ownerId, existing, profile, presentation)
        requireActiveOwner(ownerId)
        check(saved.ownerId == ownerId)
        check(profileId == null || saved.profileId == profileId)
        dao.upsertCard(saved.toEntity(nowEpochMs()))
        dao.upsertSelection(BusinessCardSelectionEntity(ownerId, saved.profileId))
        saved
    }

    suspend fun delete(ownerId: String, profileId: String) = mutationMutex.withLock {
        requireActiveOwner(ownerId)
        val cached = requireNotNull(dao.getCard(ownerId, profileId)) {
            "A névjegy nem tartozik ehhez a fiókhoz."
        }
        check(cached.ownerId == ownerId)
        remote.delete(ownerId, profileId)
        requireActiveOwner(ownerId)
        dao.deleteOwnedCardAndSelectNext(ownerId, profileId)
    }

    suspend fun updatePresentation(
        ownerId: String,
        profileId: String,
        presentation: CardPresentation,
    ) = mutationMutex.withLock {
        val entity = requireNotNull(dao.getCard(ownerId, profileId)) {
            "A névjegy nem tartozik ehhez a fiókhoz."
        }
        check(entity.ownerId == ownerId)
        val cached = BusinessCardCacheCodec.decode(entity.payloadJson)
        dao.upsertCard(
            entity.copy(
                payloadJson = BusinessCardCacheCodec.encode(cached.profile, presentation),
                cachedAtEpochMs = nowEpochMs(),
            ),
        )
    }

    suspend fun deleteOwnerData(ownerId: String) = mutationMutex.withLock {
        dao.deleteOwnerData(ownerId)
    }

    private fun requireActiveOwner(ownerId: String) {
        check(remote.authenticatedOwnerId() == ownerId) { "A munkamenet megváltozott." }
    }

    private fun BusinessCardCacheEntity.toOwnedCard(expectedOwnerId: String): OwnedBusinessCard {
        check(ownerId == expectedOwnerId)
        val cached = BusinessCardCacheCodec.decode(payloadJson)
        return OwnedBusinessCard(
            ownerId = ownerId,
            profileId = profileId,
            profile = cached.profile,
            fingerprint = fingerprint,
            isPrimary = isPrimary,
            createdAt = createdAt,
            updatedAt = updatedAt,
            presentation = cached.presentation,
        )
    }

    private fun OwnedBusinessCard.toEntity(cachedAtEpochMs: Long) = BusinessCardCacheEntity(
        ownerId = ownerId,
        profileId = profileId,
        payloadJson = BusinessCardCacheCodec.encode(profile, presentation),
        fingerprint = fingerprint,
        isPrimary = isPrimary,
        createdAt = createdAt,
        updatedAt = updatedAt,
        cachedAtEpochMs = cachedAtEpochMs,
    )
}
