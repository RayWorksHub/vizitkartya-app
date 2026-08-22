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
import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.data.ContactProfileRepository
import hu.rayworks.vizit.data.ContactProfileValidator
import hu.rayworks.vizit.nfc.HcePayloadStore
import hu.rayworks.vizit.nfc.NdefVCardEncoder
import hu.rayworks.vizit.nfc.VCardBuilder

class VizitViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ContactProfileRepository(application)
    private val hcePayloadStore = HcePayloadStore(application)

    var profile by mutableStateOf(repository.load())
        private set

    var nfcStatus by mutableStateOf(readNfcStatus(application))
        private set

    var isNfcShareActive by mutableStateOf(false)
        private set

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

        val vCard = VCardBuilder.build(profile)
        hcePayloadStore.activate(NdefVCardEncoder.encode(vCard))
        isNfcShareActive = true
        return null
    }

    fun stopNfcShare() {
        hcePayloadStore.deactivate()
        isNfcShareActive = false
    }

    fun refreshNfcStatus() {
        nfcStatus = readNfcStatus(getApplication())
    }

    fun shareAsText(context: Context) {
        val contactText = buildString {
            appendLine(profile.fullName)
            if (profile.jobTitle.isNotBlank()) appendLine(profile.jobTitle)
            if (profile.company.isNotBlank()) appendLine(profile.company)
            if (profile.phone.isNotBlank()) appendLine(profile.phone)
            if (profile.email.isNotBlank()) appendLine(profile.email)
            if (profile.website.isNotBlank()) appendLine(profile.website)
        }.trim()

        val intent = Intent.createChooser(
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "${profile.fullName} – VIZIT")
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
}
