package hu.rayworks.vizit

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.ContactProfileValidator
import hu.rayworks.vizit.data.AppFeatureFlags
import hu.rayworks.vizit.data.cards.BusinessCardCatalogState
import hu.rayworks.vizit.data.cards.OwnedBusinessCard
import hu.rayworks.vizit.data.sync.ProfileSyncState
import hu.rayworks.vizit.data.sync.ProfileSyncRunResult
import hu.rayworks.vizit.data.sync.ProfileSyncStatus
import hu.rayworks.vizit.nfc.HcePayloadStore
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.card.visibleThrough
import hu.rayworks.vizit.data.remote.NodeBackendException
import hu.rayworks.vizit.nfc.NfcPayloadFactory
import hu.rayworks.vizit.nfc.NfcShareEvent
import hu.rayworks.vizit.nfc.NfcShareEvents
import hu.rayworks.vizit.qr.PublicProfileUrlFactory
import hu.rayworks.vizit.ui.design.ThemeMode
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class VizitViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as VizitApplication).container
    private val repository = container.profileRepository
    private val businessCardRepository = container.businessCardRepository
    private val settingsStore = container.settingsStore
    private val cardPresentationStore = container.cardPresentationStore
    private val featureFlagRepository = container.featureFlagRepository
    private val hcePayloadStore = HcePayloadStore()
    private var profileObservationJob: Job? = null
    private var nfcTimeoutJob: Job? = null
    private var activeNfcSessionId: Long? = null
    private var activeProfileOwnerId: String? = null
    private var cloudSyncEnabled = false

    var profile by mutableStateOf(ContactProfile())
        private set

    var businessCards by mutableStateOf<List<OwnedBusinessCard>>(emptyList())
        private set

    var activeBusinessCardId by mutableStateOf<String?>(null)
        private set

    val usesBusinessCardCatalog: Boolean
        get() = cloudSyncEnabled

    var profileSyncState by mutableStateOf(ProfileSyncState())
        private set

    var automaticSyncEnabled by mutableStateOf(true)
        private set

    var featureFlags by mutableStateOf(AppFeatureFlags())
        private set

    /** User-selected appearance. Light is the product default. */
    var themeMode by mutableStateOf(ThemeMode.LIGHT)
        private set

    /** Card styling and per-field visibility, both local to this device. */
    var cardPresentation by mutableStateOf(CardPresentation())
        private set

    var hasOfflineProfileSession by mutableStateOf(false)
        private set

    var profileLoadStatus by mutableStateOf(ProfileLoadStatus.IDLE)
        private set

    var nfcStatus by mutableStateOf(readNfcStatus(application))
        private set

    var nfcSharePhase by mutableStateOf(NfcSharePhase.IDLE)
        private set

    var nfcPhotoIncluded by mutableStateOf(false)
        private set

    val isNfcShareActive: Boolean
        get() = nfcSharePhase != NfcSharePhase.IDLE

    init {
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                automaticSyncEnabled = settings.automaticSyncEnabled
                themeMode = ThemeMode.fromStorage(settings.appearance)
            }
        }
        viewModelScope.launch {
            cardPresentationStore.presentation.collect {
                if (!cloudSyncEnabled) cardPresentation = it
            }
        }
        viewModelScope.launch {
            NfcShareEvents.events.collect { event ->
                if (
                    event is NfcShareEvent.PayloadRead &&
                    event.sessionId == activeNfcSessionId &&
                    nfcSharePhase == NfcSharePhase.WAITING
                ) {
                    nfcTimeoutJob?.cancel()
                    activeNfcSessionId = null
                    nfcSharePhase = NfcSharePhase.PAYLOAD_READ
                }
            }
        }
    }

    fun bindProfileOwner(userId: String, enableCloudSync: Boolean) {
        if (enableCloudSync) {
            viewModelScope.launch { featureFlags = featureFlagRepository.fetch() }
        }
        if (activeProfileOwnerId == userId && cloudSyncEnabled == enableCloudSync) {
            retryProfileLoad()
            return
        }
        activeProfileOwnerId = userId
        cloudSyncEnabled = enableCloudSync
        profile = ContactProfile()
        businessCards = emptyList()
        activeBusinessCardId = null
        cardPresentation = CardPresentation()
        profileSyncState = ProfileSyncState()
        hasOfflineProfileSession = false
        profileLoadStatus = ProfileLoadStatus.LOADING
        profileObservationJob?.cancel()
        profileObservationJob = if (enableCloudSync) observeCloudCards(userId) else observeLocalProfile(userId)
    }

    fun retryProfileLoad() {
        val userId = activeProfileOwnerId ?: return
        profileLoadStatus = ProfileLoadStatus.LOADING
        viewModelScope.launch {
            if (cloudSyncEnabled) {
                refreshCloudCards(userId)
            } else {
                val result = repository.prepare(userId = userId, cloudSyncEnabled = false)
                if (activeProfileOwnerId == userId && !cloudSyncEnabled) {
                    profileLoadStatus = when {
                        profile.resolvedDisplayName.isNotBlank() -> ProfileLoadStatus.READY
                        result == ProfileSyncRunResult.RETRY ||
                            result == ProfileSyncRunResult.WAITING_FOR_SESSION -> ProfileLoadStatus.UNAVAILABLE
                        else -> ProfileLoadStatus.READY
                    }
                }
            }
        }
    }

    suspend fun saveProfile(updatedProfile: ContactProfile): String? {
        val error = ContactProfileValidator.validate(updatedProfile)
        if (error != null) return error
        val userId = activeProfileOwnerId ?: return "A profil munkamenete még nem áll készen."
        return if (cloudSyncEnabled) {
            saveCloudCard(userId, activeBusinessCardId, updatedProfile)
        } else {
            repository.save(
                userId = userId,
                profile = updatedProfile,
                cloudSyncEnabled = false,
                automaticSyncEnabled = automaticSyncEnabled,
            )
            stopNfcShare()
            null
        }
    }

    suspend fun createBusinessCard(updatedProfile: ContactProfile): String? {
        val error = ContactProfileValidator.validate(updatedProfile)
        if (error != null) return error
        val userId = activeProfileOwnerId ?: return "A profil munkamenete még nem áll készen."
        if (!cloudSyncEnabled) return "További névjegyhez jelentkezz be a fiókodba."
        return saveCloudCard(userId, profileId = null, updatedProfile)
    }

    fun selectBusinessCard(profileId: String) {
        val ownerId = activeProfileOwnerId ?: return
        val selected = businessCards.firstOrNull {
            it.ownerId == ownerId && it.profileId == profileId
        } ?: return
        stopNfcShare()
        applyActiveCard(selected)
        viewModelScope.launch {
            runCatching { businessCardRepository.select(ownerId, profileId) }
                .onFailure {
                    if (activeProfileOwnerId == ownerId && cloudSyncEnabled) {
                        applyCloudCatalog(businessCardRepository.cached(ownerId))
                    }
                }
        }
    }

    suspend fun deleteActiveBusinessCard(): String? {
        val ownerId = activeProfileOwnerId ?: return "A profil munkamenete még nem áll készen."
        val profileId = activeBusinessCardId ?: return "Nincs kiválasztott névjegy."
        if (!cloudSyncEnabled) return "A helyi tesztprofilt itt nem lehet törölni."
        profileSyncState = ProfileSyncState(status = ProfileSyncStatus.SYNCING)
        return runCatching {
            businessCardRepository.delete(ownerId, profileId)
            if (activeProfileOwnerId != ownerId || !cloudSyncEnabled) error("A munkamenet megváltozott.")
            stopNfcShare()
            applyCloudCatalog(businessCardRepository.cached(ownerId))
            hasOfflineProfileSession = true
            profileLoadStatus = ProfileLoadStatus.READY
            profileSyncState = ProfileSyncState(status = ProfileSyncStatus.SYNCED)
            null
        }.getOrElse { failure ->
            if (activeProfileOwnerId == ownerId && cloudSyncEnabled) {
                profileSyncState = ProfileSyncState(
                    status = ProfileSyncStatus.RETRY_SCHEDULED,
                    lastError = failure.localizedMessage,
                )
            }
            failure.localizedMessage ?: "A névjegy törlése nem sikerült."
        }
    }

    var conflictResolutionMessage by mutableStateOf<String?>(null)
        private set

    fun resolveProfileConflict(keepLocal: Boolean) {
        val userId = activeProfileOwnerId ?: return
        viewModelScope.launch {
            conflictResolutionMessage = try {
                if (cloudSyncEnabled) {
                    refreshCloudCards(userId)
                    "A névjegyek frissítve."
                } else if (repository.resolveConflict(userId, keepLocal)) {
                    "A választás mentve."
                } else {
                    "A profil közben megváltozott. Ellenőrizd újra az állapotot."
                }
            } catch (_: Exception) { "A feloldás nem sikerült. Az adatok megmaradtak." }
        }
    }

    fun retryProfileSync() {
        val userId = activeProfileOwnerId ?: return
        viewModelScope.launch {
            if (cloudSyncEnabled) refreshCloudCards(userId) else repository.retrySync(userId)
        }
    }

    /** The profile as a recipient sees it, with hidden fields already stripped. */
    val sharedProfile: ContactProfile
        get() = profile.visibleThrough(cardPresentation)

    fun updateCardPresentation(value: CardPresentation) {
        cardPresentation = value
        val ownerId = activeProfileOwnerId
        val profileId = activeBusinessCardId
        if (cloudSyncEnabled && ownerId != null && profileId != null) {
            businessCards = businessCards.map { card ->
                if (card.ownerId == ownerId && card.profileId == profileId) {
                    card.copy(presentation = value)
                } else {
                    card
                }
            }
            viewModelScope.launch {
                runCatching {
                    businessCardRepository.updatePresentation(ownerId, profileId, value)
                }.onFailure {
                    if (activeProfileOwnerId == ownerId && cloudSyncEnabled) {
                        applyCloudCatalog(businessCardRepository.cached(ownerId))
                    }
                }
            }
        } else {
            // The signed-out DEV profile keeps its existing device-wide setting.
            viewModelScope.launch { cardPresentationStore.save(value) }
        }
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        viewModelScope.launch { settingsStore.setAppearance(mode.storageValue) }
    }

    fun updateAutomaticSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAutomaticSyncEnabled(enabled)
            if (enabled && cloudSyncEnabled) {
                activeProfileOwnerId?.let { refreshCloudCards(it) }
            } else {
                repository.setAutomaticSyncEnabled(enabled && cloudSyncEnabled)
            }
        }
    }

    fun isBoundToCloudOwner(ownerId: String): Boolean =
        cloudSyncEnabled && activeProfileOwnerId == ownerId

    private fun observeLocalProfile(userId: String): Job = viewModelScope.launch {
        val initialResult = repository.prepare(userId = userId, cloudSyncEnabled = false)
        repository.observe(userId).collect { state ->
            if (activeProfileOwnerId != userId || cloudSyncEnabled) return@collect
            profile = state.profile
            profileSyncState = state.sync
            hasOfflineProfileSession = true
            profileLoadStatus = when {
                state.profile.resolvedDisplayName.isNotBlank() -> ProfileLoadStatus.READY
                initialResult == ProfileSyncRunResult.RETRY ||
                    initialResult == ProfileSyncRunResult.WAITING_FOR_SESSION -> ProfileLoadStatus.UNAVAILABLE
                else -> ProfileLoadStatus.READY
            }
        }
    }

    private fun observeCloudCards(ownerId: String): Job = viewModelScope.launch {
        val cached = businessCardRepository.cached(ownerId)
        if (activeProfileOwnerId != ownerId || !cloudSyncEnabled) return@launch
        applyCloudCatalog(cached)
        if (cached.cards.isNotEmpty()) {
            hasOfflineProfileSession = true
            profileLoadStatus = ProfileLoadStatus.READY
        }
        refreshCloudCards(ownerId)
        businessCardRepository.observe(ownerId).collect { state ->
            if (activeProfileOwnerId == ownerId && cloudSyncEnabled) applyCloudCatalog(state)
        }
    }

    private suspend fun refreshCloudCards(ownerId: String) {
        if (activeProfileOwnerId != ownerId || !cloudSyncEnabled) return
        profileSyncState = ProfileSyncState(status = ProfileSyncStatus.SYNCING)
        runCatching { businessCardRepository.refresh(ownerId) }
            .onSuccess {
                if (activeProfileOwnerId != ownerId || !cloudSyncEnabled) return@onSuccess
                applyCloudCatalog(businessCardRepository.cached(ownerId))
                hasOfflineProfileSession = true
                profileLoadStatus = ProfileLoadStatus.READY
                profileSyncState = ProfileSyncState(status = ProfileSyncStatus.SYNCED)
            }
            .onFailure { failure ->
                if (activeProfileOwnerId != ownerId || !cloudSyncEnabled) return@onFailure
                val cached = businessCardRepository.cached(ownerId)
                applyCloudCatalog(cached)
                hasOfflineProfileSession = cached.cards.isNotEmpty()
                profileLoadStatus = if (cached.cards.isNotEmpty()) {
                    ProfileLoadStatus.READY
                } else {
                    ProfileLoadStatus.UNAVAILABLE
                }
                profileSyncState = ProfileSyncState(
                    status = ProfileSyncStatus.RETRY_SCHEDULED,
                    lastError = failure.localizedMessage,
                )
            }
    }

    private suspend fun saveCloudCard(
        ownerId: String,
        profileId: String?,
        updatedProfile: ContactProfile,
    ): String? {
        if (activeProfileOwnerId != ownerId || !cloudSyncEnabled) {
            return "A munkamenet megváltozott."
        }
        profileSyncState = ProfileSyncState(status = ProfileSyncStatus.SYNCING, pendingChanges = true)
        return runCatching {
            businessCardRepository.save(
                ownerId = ownerId,
                profileId = profileId,
                profile = updatedProfile,
                presentation = cardPresentation,
            )
            if (activeProfileOwnerId != ownerId || !cloudSyncEnabled) error("A munkamenet megváltozott.")
            stopNfcShare()
            applyCloudCatalog(businessCardRepository.cached(ownerId))
            hasOfflineProfileSession = true
            profileLoadStatus = ProfileLoadStatus.READY
            profileSyncState = ProfileSyncState(status = ProfileSyncStatus.SYNCED)
            null
        }.getOrElse { failure ->
            if (activeProfileOwnerId == ownerId && cloudSyncEnabled) {
                profileSyncState = ProfileSyncState(
                    status = if (failure is NodeBackendException && failure.conflict) {
                        ProfileSyncStatus.CONFLICT
                    } else {
                        ProfileSyncStatus.RETRY_SCHEDULED
                    },
                    pendingChanges = false,
                    lastError = failure.localizedMessage,
                )
            }
            failure.localizedMessage ?: "A névjegy mentése nem sikerült."
        }
    }

    private fun applyCloudCatalog(state: BusinessCardCatalogState) {
        val ownerId = activeProfileOwnerId ?: return
        if (!cloudSyncEnabled || state.ownerId != ownerId) return
        val ownedCards = state.cards.filter { it.ownerId == ownerId }
        businessCards = ownedCards
        val active = ownedCards.firstOrNull { it.profileId == state.activeProfileId }
            ?: ownedCards.firstOrNull { it.isPrimary }
            ?: ownedCards.firstOrNull()
        if (active == null) {
            activeBusinessCardId = null
            profile = ContactProfile()
            cardPresentation = CardPresentation()
        } else {
            applyActiveCard(active)
        }
    }

    private fun applyActiveCard(card: OwnedBusinessCard) {
        val ownerId = activeProfileOwnerId ?: return
        if (!cloudSyncEnabled || card.ownerId != ownerId) return
        activeBusinessCardId = card.profileId
        profile = card.profile
        cardPresentation = card.presentation
    }

    fun startNfcShare(): String? {
        val validationError = ContactProfileValidator.validate(profile)
        if (validationError != null) return validationError

        // Hiding every reachable field would hand over a card nobody can act on.
        if (sharedProfile.phone.isBlank() && sharedProfile.email.isBlank()) {
            return "Az Adatláthatóságban a telefonszám és az e-mail-cím is ki van kapcsolva, " +
                "így nem marad mit átadni. Kapcsold vissza valamelyiket."
        }

        refreshNfcStatus()
        if (!nfcStatus.isAvailable) return "Ez a telefon nem rendelkezik NFC-vel."
        if (!nfcStatus.hasHostCardEmulation) return "A telefon nem támogatja az NFC-kártyaemulációt."
        if (!nfcStatus.isEnabled) return "Kapcsold be az NFC-t a telefon beállításaiban."

        val fallbackUrl = profile.publicSlug
            .takeIf { profile.isPublic && it.isNotBlank() }
            ?.let {
                PublicProfileUrlFactory.createPreferred(
                    baseUrl = BuildConfig.PUBLIC_PROFILE_BASE_URL,
                    slug = it,
                    customDomain = profile.customDomain,
                    customDomainVerified = profile.customDomainVerified,
                ).getOrNull()
            }

        val prepared = runCatching {
            NfcPayloadFactory.create(sharedProfile.copy(photoBase64 = ""), fallbackUrl)
        }
            .getOrElse { return "A névjegy NFC-adatcsomagja túl nagy. Rövidíts néhány mezőt, majd próbáld újra." }

        val sessionId = hcePayloadStore.activate(prepared.bytes, NFC_SHARE_TIMEOUT_MILLIS)
        activeNfcSessionId = sessionId
        nfcPhotoIncluded = prepared.photoIncluded
        nfcSharePhase = NfcSharePhase.WAITING

        nfcTimeoutJob?.cancel()
        nfcTimeoutJob = viewModelScope.launch {
            delay(NFC_SHARE_TIMEOUT_MILLIS)
            if (
                nfcSharePhase == NfcSharePhase.WAITING &&
                activeNfcSessionId == sessionId
            ) {
                hcePayloadStore.deactivate(sessionId)
                activeNfcSessionId = null
                nfcSharePhase = NfcSharePhase.TIMED_OUT
            }
        }
        return null
    }

    fun stopNfcShare() {
        nfcTimeoutJob?.cancel()
        nfcTimeoutJob = null
        activeNfcSessionId?.let(hcePayloadStore::deactivate)
        activeNfcSessionId = null
        nfcSharePhase = NfcSharePhase.IDLE
        nfcPhotoIncluded = false
    }

    fun reportNfcRoutingFailure() {
        nfcTimeoutJob?.cancel()
        nfcTimeoutJob = null
        activeNfcSessionId?.let(hcePayloadStore::deactivate)
        activeNfcSessionId = null
        nfcSharePhase = NfcSharePhase.ROUTING_FAILED
    }

    fun refreshNfcStatus() {
        nfcStatus = readNfcStatus(getApplication())
    }

    fun shareAsText(context: Context) {
        val profile = sharedProfile
        val contactText = buildString {
            appendLine(profile.resolvedDisplayName)
            if (profile.jobTitle.isNotBlank()) appendLine(profile.jobTitle)
            if (profile.company.isNotBlank()) appendLine(profile.company)
            if (profile.phone.isNotBlank()) appendLine(profile.phone)
            if (profile.email.isNotBlank()) appendLine(profile.email)
            if (profile.website.isNotBlank()) appendLine(profile.website)
            if (profile.linkedIn.isNotBlank()) appendLine("LinkedIn: ${profile.linkedIn}")
            if (profile.facebook.isNotBlank()) appendLine("Facebook: ${profile.facebook}")
            if (profile.instagram.isNotBlank()) appendLine("Instagram: ${profile.instagram}")
            if (profile.tiktok.isNotBlank()) appendLine("TikTok: ${profile.tiktok}")
            if (profile.youtube.isNotBlank()) appendLine("YouTube: ${profile.youtube}")
        }.trim()

        val intent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "${profile.resolvedDisplayName} – VIZIT")
                putExtra(Intent.EXTRA_TEXT, contactText)
            },
            "Névjegy megosztása",
        )
        context.startActivity(intent)
    }

    override fun onCleared() {
        stopNfcShare()
        super.onCleared()
    }

    private fun readNfcStatus(context: Context): NfcStatus {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        return NfcStatus(
            isAvailable = adapter != null,
            isEnabled = adapter?.isEnabled == true,
            hasHostCardEmulation = context.packageManager.hasSystemFeature(
                PackageManager.FEATURE_NFC_HOST_CARD_EMULATION,
            ),
        )
    }

    private companion object {
        const val NFC_SHARE_TIMEOUT_MILLIS = 60_000L
    }
}

enum class ProfileLoadStatus {
    IDLE,
    LOADING,
    READY,
    UNAVAILABLE,
}
