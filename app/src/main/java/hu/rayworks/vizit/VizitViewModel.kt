package hu.rayworks.vizit

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.NfcAdapter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.ContactProfileRepository
import hu.rayworks.vizit.data.ContactProfileValidator
import hu.rayworks.vizit.nfc.HcePayloadStore
import hu.rayworks.vizit.nfc.NfcPayloadFactory
import hu.rayworks.vizit.nfc.NfcShareEvent
import hu.rayworks.vizit.nfc.NfcShareEvents
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class VizitViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContactProfileRepository(application)
    private val hcePayloadStore = HcePayloadStore()
    private var nfcTimeoutJob: Job? = null
    private var activeNfcSessionId: Long? = null

    var profile by mutableStateOf(repository.load())
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

    fun saveProfile(updatedProfile: ContactProfile): String? {
        val error = ContactProfileValidator.validate(updatedProfile)
        if (error != null) return error

        repository.save(updatedProfile)
        profile = repository.load()
        stopNfcShare()
        return null
    }

    fun startNfcShare(): String? {
        val validationError = ContactProfileValidator.validate(profile)
        if (validationError != null) return validationError

        refreshNfcStatus()
        if (!nfcStatus.isAvailable) return "Ez a telefon nem rendelkezik NFC-vel."
        if (!nfcStatus.hasHostCardEmulation) return "A telefon nem támogatja az NFC-kártyaemulációt."
        if (!nfcStatus.isEnabled) return "Kapcsold be az NFC-t a telefon beállításaiban."

        val fallbackUrl = profile.publicSlug
            .takeIf { profile.isPublic && it.isNotBlank() }
            ?.let { "${BuildConfig.PUBLIC_PROFILE_BASE_URL}/${Uri.encode(it)}" }

        val prepared = runCatching { NfcPayloadFactory.create(profile, fallbackUrl) }
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

    fun refreshNfcStatus() {
        nfcStatus = readNfcStatus(getApplication())
    }

    fun shareAsText(context: Context) {
        val contactText = buildString {
            appendLine(profile.resolvedDisplayName)
            if (profile.jobTitle.isNotBlank()) appendLine(profile.jobTitle)
            if (profile.company.isNotBlank()) appendLine(profile.company)
            if (profile.phone.isNotBlank()) appendLine(profile.phone)
            if (profile.email.isNotBlank()) appendLine(profile.email)
            if (profile.website.isNotBlank()) appendLine(profile.website)
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
