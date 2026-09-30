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
import hu.rayworks.vizit.data.remote.AccountProfile
import hu.rayworks.vizit.data.sync.ProfileSyncState
import hu.rayworks.vizit.data.sync.ProfileSyncRunResult
import hu.rayworks.vizit.data.sync.ProfileSyncStatus
import hu.rayworks.vizit.nfc.HcePayloadStore
import hu.rayworks.vizit.data.card.CardPresentation
import hu.rayworks.vizit.data.card.visibleThrough
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
    private val settingsStore = container.settingsStore
    private val cardPresentationStore = container.cardPresentationStore
    private val featureFlagRepository = container.featureFlagRepository
    private val profileCatalog = container.profileCatalog
    private val hcePayloadStore = HcePayloadStore()
    private var profileObservationJob: Job? = null
    private var nfcTimeoutJob: Job? = null
    private var activeNfcSessionId: Long? = null
    private var activeProfileOwnerId: String? = null
    private var cloudSyncEnabled = false

    var profile by mutableStateOf(ContactProfile())
        private set

    var profileSyncState by mutableStateOf(ProfileSyncState())
        private set

    var automaticSyncEnabled by mutableStateOf(true)
        private set

    var featureFlags by mutableStateOf(AppFeatureFlags())
        private set

    var accountProfiles by mutableStateOf<List<AccountProfile>>(emptyList())
        private set

    var profileCatalogBusy by mutableStateOf(false)
        private set

    var profileCatalogMessage by mutableStateOf<String?>(null)
        private set

    var creatingAdditionalProfile by mutableStateOf(false)
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
            cardPresentationStore.presentation.collect { cardPresentation = it }
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
        if (activeProfileOwnerId == userId && cloudSyncEnabled == enableCloudSync) {
            if (enableCloudSync) {
                viewModelScope.launch {
                    featureFlags = featureFlagRepository.fetch()
                    refreshProfileCatalog()
                }
            }
            viewModelScope.launch {
                repository.prepare(
                    userId = userId,
                    cloudSyncEnabled = enableCloudSync,
                )
            }
            return
        }
        activeProfileOwnerId = userId
        cloudSyncEnabled = enableCloudSync
        accountProfiles = emptyList()
        profileCatalogMessage = null
        creatingAdditionalProfile = false
        profile = ContactProfile()
        profileSyncState = ProfileSyncState()
        hasOfflineProfileSession = false
        profileLoadStatus = ProfileLoadStatus.LOADING
        profileObservationJob?.cancel()
        if (enableCloudSync) {
            viewModelScope.launch {
                featureFlags = featureFlagRepository.fetch()
                refreshProfileCatalog()
            }
        }
        profileObservationJob = viewModelScope.launch {
            val initialResult = repository.prepare(
                userId = userId,
                cloudSyncEnabled = enableCloudSync,
            )
            repository.observe(userId).collect { state ->
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
    }

    fun retryProfileLoad() {
        val userId = activeProfileOwnerId ?: return
        profileLoadStatus = ProfileLoadStatus.LOADING
        viewModelScope.launch {
            val result = repository.prepare(userId = userId, cloudSyncEnabled = cloudSyncEnabled)
            profileLoadStatus = when {
                profile.resolvedDisplayName.isNotBlank() -> ProfileLoadStatus.READY
                result == ProfileSyncRunResult.RETRY ||
                    result == ProfileSyncRunResult.WAITING_FOR_SESSION -> ProfileLoadStatus.UNAVAILABLE
                else -> ProfileLoadStatus.READY
            }
        }
    }

    suspend fun saveProfile(updatedProfile: ContactProfile): String? {
        val error = ContactProfileValidator.validate(updatedProfile)
        if (error != null) return error
        val userId = activeProfileOwnerId ?: return "A profil munkamenete még nem áll készen."
        repository.save(
            userId = userId,
            profile = updatedProfile,
            cloudSyncEnabled = cloudSyncEnabled,
            automaticSyncEnabled = automaticSyncEnabled,
        )
        stopNfcShare()
        return null
    }

    fun beginAdditionalProfile(): String? {
        if (!featureFlags.multiProfile) return "A többprofilos funkció még nincs bekapcsolva."
        if (!cloudSyncEnabled) return "Új profilt csak bejelentkezett, online fiókhoz lehet létrehozni."
        if (profileSyncState.pendingChanges || profileSyncState.status == ProfileSyncStatus.CONFLICT) {
            return "Előbb várd meg a jelenlegi profil szinkronizálását."
        }
        creatingAdditionalProfile = true
        profileCatalogMessage = null
        return null
    }

    fun cancelAdditionalProfile() {
        creatingAdditionalProfile = false
    }

    suspend fun createAdditionalProfile(updatedProfile: ContactProfile): String? {
        val validationError = ContactProfileValidator.validate(updatedProfile)
        if (validationError != null) return validationError
        val userId = activeProfileOwnerId ?: return "A profil munkamenete még nem áll készen."
        if (!featureFlags.multiProfile || !cloudSyncEnabled) return "A többprofilos funkció most nem érhető el."
        if (profileSyncState.pendingChanges || profileSyncState.status == ProfileSyncStatus.CONFLICT) {
            return "Előbb várd meg a jelenlegi profil szinkronizálását."
        }
        profileCatalogBusy = true
        profileCatalogMessage = null
        return try {
            profileCatalog.create(updatedProfile)
            reloadDefaultProfile(userId)
            profileCatalogMessage = "Az új profil elkészült és aktív."
            null
        } catch (failure: Exception) {
            failure.localizedMessage ?: "Az új profil nem hozható létre."
        } finally {
            profileCatalogBusy = false
        }
    }

    fun switchProfile(profileId: String) {
        val userId = activeProfileOwnerId ?: return
        if (profileCatalogBusy || accountProfiles.firstOrNull { it.isDefault }?.id == profileId) return
        if (profileSyncState.pendingChanges || profileSyncState.status == ProfileSyncStatus.CONFLICT) {
            profileCatalogMessage = "Előbb várd meg a jelenlegi profil szinkronizálását."
            return
        }
        viewModelScope.launch {
            profileCatalogBusy = true
            profileCatalogMessage = null
            try {
                profileCatalog.makeDefault(profileId)
                reloadDefaultProfile(userId)
                profileCatalogMessage = "Profil átváltva."
            } catch (failure: Exception) {
                profileCatalogMessage = failure.localizedMessage ?: "A profilváltás nem sikerült."
            } finally {
                profileCatalogBusy = false
            }
        }
    }

    fun deleteActiveProfile() {
        val userId = activeProfileOwnerId ?: return
        val active = accountProfiles.firstOrNull { it.isDefault } ?: return
        if (profileCatalogBusy) return
        if (profileSyncState.pendingChanges || profileSyncState.status == ProfileSyncStatus.CONFLICT) {
            profileCatalogMessage = "Előbb várd meg a jelenlegi profil szinkronizálását."
            return
        }
        viewModelScope.launch {
            profileCatalogBusy = true
            profileCatalogMessage = null
            try {
                profileCatalog.delete(active.id)
                reloadDefaultProfile(userId)
                profileCatalogMessage = "A profil törölve."
            } catch (failure: Exception) {
                profileCatalogMessage = failure.localizedMessage ?: "A profil nem törölhető."
            } finally {
                profileCatalogBusy = false
            }
        }
    }

    fun clearProfileCatalogMessage() {
        profileCatalogMessage = null
    }

    private suspend fun reloadDefaultProfile(userId: String) {
        profileLoadStatus = ProfileLoadStatus.LOADING
        stopNfcShare()
        repository.deleteLocalProfile(userId)
        val result = repository.prepare(userId = userId, cloudSyncEnabled = cloudSyncEnabled)
        refreshProfileCatalog()
        profileLoadStatus = when {
            profile.resolvedDisplayName.isNotBlank() -> ProfileLoadStatus.READY
            result == ProfileSyncRunResult.RETRY || result == ProfileSyncRunResult.WAITING_FOR_SESSION ->
                ProfileLoadStatus.UNAVAILABLE
            else -> ProfileLoadStatus.READY
        }
    }

    private suspend fun refreshProfileCatalog() {
        runCatching { profileCatalog.list() }
            .onSuccess { accountProfiles = it }
            .onFailure {
                if (accountProfiles.isEmpty()) {
                    profileCatalogMessage = "A profillista most nem frissíthető."
                }
            }
    }

    var conflictResolutionMessage by mutableStateOf<String?>(null)
        private set

    fun resolveProfileConflict(keepLocal: Boolean) {
        val userId = activeProfileOwnerId ?: return
        viewModelScope.launch {
            conflictResolutionMessage = try {
                if (repository.resolveConflict(userId, keepLocal)) "A választás mentve."
                else "A profil közben megváltozott. Ellenőrizd újra az állapotot."
            } catch (_: Exception) { "A feloldás nem sikerült. Az adatok megmaradtak." }
        }
    }

    fun retryProfileSync() {
        val userId = activeProfileOwnerId ?: return
        viewModelScope.launch { repository.retrySync(userId) }
    }

    /** The profile as a recipient sees it, with hidden fields already stripped. */
    val sharedProfile: ContactProfile
        get() = profile.visibleThrough(cardPresentation)

    fun updateCardPresentation(value: CardPresentation) {
        // Optimistic: the switch has to move under the finger, the DataStore
        // write follows and the collector confirms it.
        cardPresentation = value
        viewModelScope.launch { cardPresentationStore.save(value) }
    }

    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        viewModelScope.launch { settingsStore.setAppearance(mode.storageValue) }
    }

    fun updateAutomaticSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAutomaticSyncEnabled(enabled)
            if (enabled && cloudSyncEnabled) {
                activeProfileOwnerId?.let { repository.retrySync(it) }
            }
            repository.setAutomaticSyncEnabled(enabled && cloudSyncEnabled)
        }
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
