package hu.rayworks.vizit.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class VizitHostApduService : HostApduService() {
    private val payloadStore = HcePayloadStore()
    private var processor: Type4TagApduProcessor? = null
    private var processorSessionId: Long? = null

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        val activePayload = payloadStore.activePayload()
            ?: return Type4TagApduProcessor.STATUS_SECURITY_NOT_SATISFIED

        if (Type4TagApduProcessor.isSelectApplication(commandApdu)) {
            processorSessionId = activePayload.sessionId
            processor = Type4TagApduProcessor(activePayload.bytes) { progress ->
                if (progress.isComplete && payloadStore.deactivate(activePayload.sessionId)) {
                    HceShareEventBus.publish(
                        HceShareEvent.PayloadRead(
                            sessionId = activePayload.sessionId,
                            payloadBytes = activePayload.bytes.size,
                        ),
                    )
                }
            }
        } else if (processorSessionId != activePayload.sessionId) {
            processor = null
            processorSessionId = null
            return Type4TagApduProcessor.STATUS_SECURITY_NOT_SATISFIED
        }

        return processor?.process(commandApdu)
            ?: Type4TagApduProcessor.STATUS_SECURITY_NOT_SATISFIED
    }

    override fun onDeactivated(reason: Int) {
        processorSessionId?.let { sessionId ->
            HceShareEventBus.publish(HceShareEvent.LinkDeactivated(sessionId, reason))
        }
        processor = null
        processorSessionId = null
    }
}
