package hu.rayworks.vizit.data.cards

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.card.CardPresentation
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

data class OwnedBusinessCard(
    val ownerId: String,
    val profileId: String,
    val profile: ContactProfile,
    val fingerprint: String,
    val isPrimary: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val presentation: CardPresentation = CardPresentation(),
)

data class BusinessCardCatalogState(
    val ownerId: String,
    val cards: List<OwnedBusinessCard>,
    val activeProfileId: String?,
) {
    val activeCard: OwnedBusinessCard?
        get() = cards.firstOrNull { it.profileId == activeProfileId }
            ?: cards.firstOrNull { it.isPrimary }
            ?: cards.firstOrNull()
}

@Serializable
internal data class CachedBusinessCardPayload(
    val profile: ContactProfile,
    val presentation: CardPresentation,
)

internal object BusinessCardCacheCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(profile: ContactProfile, presentation: CardPresentation): String =
        json.encodeToString(CachedBusinessCardPayload(profile, presentation))

    fun decode(value: String): CachedBusinessCardPayload =
        json.decodeFromString<CachedBusinessCardPayload>(value)
}
