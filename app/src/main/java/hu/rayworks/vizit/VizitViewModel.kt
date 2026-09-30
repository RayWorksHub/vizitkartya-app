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
import hu.rayworks.vizit.data.sync.ProfileSyncState
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
    private val repository = container.profileRepository // DEV local profile only.
    private val settingsStore = container.settingsStore
    private val cardPresentationStore = container.cardPresentationStore
    private val featureFlagRepository = container.featureFlagRepository
    private val profiles = hu.rayworks.vizit.data.account.AccountProfileSession(container.accountProfileRepository, viewModelScope)
    private val hcePayloadStore = HcePayloadStore()
    private var profileObservationJob: Job? = null
    private var nfcTimeoutJob: Job? = null
    private var activeNfcSessionId: Long? = null
    private var activeProfileOwnerId: String? = null
    private var cloudSyncEnabled = false

    var accountState by mutableStateOf(hu.rayworks.vizit.data.account.AccountProfileState())
        private set
    private var localProfile by mutableStateOf(ContactProfile())
    private var localSyncState by mutableStateOf(ProfileSyncState())
    private var localLoadStatus by mutableStateOf(ProfileLoadStatus.IDLE)
    private var localPrepared by mutableStateOf(false)
    private var localPresentation by mutableStateOf(CardPresentation())

    val profile: ContactProfile get() = if (cloudSyncEnabled) accountState.active?.profile ?: ContactProfile() else localProfile
    val activeProfileId: String? get() = if (cloudSyncEnabled) accountState.activeId else "local-profile"
    val accountProfiles get() = accountState.profiles
    val profileSyncState get() = if (cloudSyncEnabled) accountState.active?.sync ?: ProfileSyncState() else localSyncState
    val profileCatalogBusy get() = accountState.operationBusy
    val profileCatalogMessage get() = accountState.message
    val needsFirstProfile get() = if (cloudSyncEnabled) accountState.needsFirstProfile else localPrepared && localProfile.resolvedDisplayName.isBlank()
    val hasOfflineProfileSession get() = if (cloudSyncEnabled) accountState.profiles.isNotEmpty() else localPrepared
    val profileLoadStatus get() = if (!cloudSyncEnabled) localLoadStatus else when (accountState.loadStatus) {
        hu.rayworks.vizit.data.account.AccountProfileLoadStatus.LOADING -> ProfileLoadStatus.LOADING
        hu.rayworks.vizit.data.account.AccountProfileLoadStatus.READY -> ProfileLoadStatus.READY
        hu.rayworks.vizit.data.account.AccountProfileLoadStatus.UNAVAILABLE -> ProfileLoadStatus.UNAVAILABLE
    }
    val cardPresentation get() = if (cloudSyncEnabled) accountState.active?.presentation ?: CardPresentation() else localPresentation

    var automaticSyncEnabled by mutableStateOf(true)
        private set
    var featureFlags by mutableStateOf(AppFeatureFlags())
        private set
    var creatingAdditionalProfile by mutableStateOf(false)
        private set
    var themeMode by mutableStateOf(ThemeMode.LIGHT)
        private set
    var conflictResolutionMessage by mutableStateOf<String?>(null)
        private set
    var nfcStatus by mutableStateOf(readNfcStatus(application))
        private set
    var nfcSharePhase by mutableStateOf(NfcSharePhase.IDLE)
        private set
    var nfcPhotoIncluded by mutableStateOf(false)
        private set
    val isNfcShareActive: Boolean get() = nfcSharePhase != NfcSharePhase.IDLE

    init {
        viewModelScope.launch { profiles.state.collect { accountState = it } }
        viewModelScope.launch {
            settingsStore.settings.collect { settings ->
                automaticSyncEnabled = settings.automaticSyncEnabled
                themeMode = ThemeMode.fromStorage(settings.appearance)
            }
        }
        viewModelScope.launch { cardPresentationStore.presentation.collect { localPresentation = it } }
        viewModelScope.launch {
            NfcShareEvents.events.collect { event ->
                if (event is NfcShareEvent.PayloadRead && event.sessionId == activeNfcSessionId &&
                    nfcSharePhase == NfcSharePhase.WAITING) {
                    nfcTimeoutJob?.cancel()
                    activeNfcSessionId = null
                    nfcSharePhase = NfcSharePhase.PAYLOAD_READ
                }
            }
        }
    }

    fun bindProfileOwner(userId: String, enableCloudSync: Boolean) {
        if (activeProfileOwnerId == userId && cloudSyncEnabled == enableCloudSync) return
        unbindProfileOwner()
        activeProfileOwnerId = userId
        cloudSyncEnabled = enableCloudSync
        if (enableCloudSync) {
            accountState = hu.rayworks.vizit.data.account.AccountProfileState(ownerId = userId)
            profiles.bind(userId)
            viewModelScope.launch {
                val flags = featureFlagRepository.fetch()
                if (activeProfileOwnerId == userId && cloudSyncEnabled) featureFlags = flags
            }
        } else {
            localLoadStatus = ProfileLoadStatus.LOADING
            profileObservationJob = viewModelScope.launch {
                repository.prepare(userId, cloudSyncEnabled = false)
                repository.observe(userId).collect { state ->
                    if (activeProfileOwnerId == userId && !cloudSyncEnabled) {
                        localProfile = state.profile
                        localSyncState = state.sync
                        localPrepared = true
                        localLoadStatus = ProfileLoadStatus.READY
                    }
                }
            }
        }
    }

    fun unbindProfileOwner() {
        stopNfcShare()
        profileObservationJob?.cancel()
        profileObservationJob = null
        profiles.unbind()
        accountState = hu.rayworks.vizit.data.account.AccountProfileState()
        activeProfileOwnerId = null
        cloudSyncEnabled = false
        localProfile = ContactProfile()
        localSyncState = ProfileSyncState()
        localPrepared = false
        localLoadStatus = ProfileLoadStatus.IDLE
        creatingAdditionalProfile = false
        conflictResolutionMessage = null
        featureFlags = AppFeatureFlags()
    }

    fun retryProfileLoad() {
        if (cloudSyncEnabled) profiles.retryLoad()
    }

    suspend fun saveProfile(
        updatedProfile: ContactProfile,
        targetProfileId: String? = activeProfileId,
        presentation: CardPresentation = cardPresentation,
    ): String? {
        val error = ContactProfileValidator.validate(updatedProfile)
        if (error != null) return error
        val userId = activeProfileOwnerId ?: return "Jelentkezz be újra."
        val issue = if (cloudSyncEnabled) {
            profiles.save(targetProfileId ?: return "Válassz profilt.", updatedProfile, presentation)
        } else {
            repository.save(userId, updatedProfile, cloudSyncEnabled = false, automaticSyncEnabled = false)
            cardPresentationStore.save(presentation)
            null
        }
        if (issue == null) stopNfcShare()
        return issue
    }

    fun beginAdditionalProfile(): String? {
        if (!cloudSyncEnabled) return "Új profilt csak bejelentkezett, online fiókhoz lehet létrehozni."
        if (!featureFlags.multiProfile && accountProfiles.isNotEmpty()) return "A többprofilos funkció most nem érhető el."
        if (!accountState.catalogVerified || profileCatalogBusy) return "Várd meg a profillista betöltését."
        creatingAdditionalProfile = true
        return null
    }

    fun cancelAdditionalProfile() { creatingAdditionalProfile = false }

    suspend fun createAdditionalProfile(updatedProfile: ContactProfile, presentation: CardPresentation = CardPresentation()): String? {
        val error = ContactProfileValidator.validate(updatedProfile)
        if (error != null) return error
        if (!cloudSyncEnabled) return saveProfile(updatedProfile, presentation = presentation)
        if (accountProfiles.isNotEmpty() && !featureFlags.multiProfile) return "A többprofilos funkció most nem érhető el."
        val result = profiles.create(updatedProfile, presentation)
        if (result == null) {
            creatingAdditionalProfile = false
            stopNfcShare()
        }
        return result
    }

    fun switchProfile(profileId: String) {
        if (profileId != activeProfileId) stopNfcShare()
        profiles.select(profileId)
    }
    fun deleteActiveProfile() { stopNfcShare(); profiles.deleteActive() }
    fun deleteProfile(profileId: String) { stopNfcShare(); profiles.deleteActive(profileId) }
    fun clearProfileCatalogMessage() { profiles.clearMessage() }
    fun resolveProfileConflict(keepLocal: Boolean) { profiles.resolve(keepLocal) }
    fun retryProfileSync() { profiles.retrySync() }

    val sharedProfile: ContactProfile get() = profile.visibleThrough(cardPresentation)

    fun updateCardPresentation(value: CardPresentation) {
        val id = activeProfileId
        if (cloudSyncEnabled && id != null) profiles.present(id, value)
        else {
            localPresentation = value
            viewModelScope.launch { cardPresentationStore.save(value) }
        }
    }
    fun updateCardPresentation(profileId: String, value: CardPresentation) {
        if (cloudSyncEnabled) profiles.present(profileId, value) else updateCardPresentation(value)
    }
    fun updateThemeMode(mode: ThemeMode) {
        themeMode = mode
        viewModelScope.launch { settingsStore.setAppearance(mode.storageValue) }
    }
    fun updateAutomaticSyncEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAutomaticSyncEnabled(enabled)
            if (enabled && cloudSyncEnabled) profiles.retrySync()
            repository.setAutomaticSyncEnabled(enabled && cloudSyncEnabled)
        }
    }

    fun startNfcShare(targetProfileId: String? = activeProfileId): String? {
        val selected = profiles.state.value.profiles.firstOrNull {
            it.ownerId == activeProfileOwnerId && it.id == targetProfileId
        }
        val profile = if (cloudSyncEnabled) selected?.profile ?: return "Válassz profilt." else localProfile
        val sharedProfile = profile.visibleThrough(selected?.presentation ?: localPresentation)
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
