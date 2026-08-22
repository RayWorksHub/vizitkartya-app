package hu.rayworks.vizit.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class VizitHostApduService : HostApduService() {
    private val payloadStore = HcePayloadStore()
    private var processor: Type4TagApduProcessor? = null
    private var processorSessionId: Long? = null

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (Type4TagApduProcessor.isSelectApplication(commandApdu)) {
            val activePayload = payloadStore.activePayload()
                ?: return Type4TagApduProcessor.STATUS_SECURITY_NOT_SATISFIED
            processorSessionId = activePayload.sessionId
            processor = Type4TagApduProcessor(activePayload.bytes) { progress ->
                if (progress.isComplete && payloadStore.deactivate(activePayload.sessionId)) {
                    NfcShareEvents.emit(
                        NfcShareEvent.PayloadRead(
                            sessionId = activePayload.sessionId,
                            payloadBytes = activePayload.bytes.size,
                        ),
                    )
                }
            }
        } else {
            val activeSessionId = payloadStore.activeSessionId()
                ?: return Type4TagApduProcessor.STATUS_SECURITY_NOT_SATISFIED
            if (processorSessionId != activeSessionId) {
                processor = null
                processorSessionId = null
                return Type4TagApduProcessor.STATUS_SECURITY_NOT_SATISFIED
            }
        }

        return processor?.process(commandApdu)
            ?: Type4TagApduProcessor.STATUS_SECURITY_NOT_SATISFIED
    }

    override fun onDeactivated(reason: Int) {
        processorSessionId?.let { sessionId ->
            NfcShareEvents.emit(NfcShareEvent.Deactivated(sessionId, reason))
        }
        processor = null
        processorSessionId = null
    }
}
