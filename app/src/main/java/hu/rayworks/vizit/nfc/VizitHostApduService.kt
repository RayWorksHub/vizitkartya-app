package hu.rayworks.vizit.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class VizitHostApduService : HostApduService() {
    private lateinit var payloadStore: HcePayloadStore
    private var processor: Type4TagApduProcessor? = null
    private var readEventEmitted = false

    override fun onCreate() {
        super.onCreate()
        payloadStore = HcePayloadStore(applicationContext)
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (!payloadStore.isActive()) return Type4TagApduProcessor.STATUS_SECURITY_NOT_SATISFIED

        if (Type4TagApduProcessor.isSelectApplication(commandApdu)) {
            val payload = payloadStore.payload()
                ?: return Type4TagApduProcessor.STATUS_SECURITY_NOT_SATISFIED
            processor = Type4TagApduProcessor(payload)
            readEventEmitted = false
        }

        val activeProcessor = processor
            ?: return Type4TagApduProcessor.STATUS_SECURITY_NOT_SATISFIED
        val response = activeProcessor.process(commandApdu)

        if (activeProcessor.isNdefFullyRead && !readEventEmitted) {
            readEventEmitted = true
            NfcShareEvents.emit(NfcShareEvent.PayloadRead)
        }

        return response
    }

    override fun onDeactivated(reason: Int) {
        processor = null
        readEventEmitted = false
        NfcShareEvents.emit(NfcShareEvent.Deactivated(reason))
    }
}
