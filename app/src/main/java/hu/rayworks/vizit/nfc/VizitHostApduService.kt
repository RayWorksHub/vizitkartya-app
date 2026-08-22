package hu.rayworks.vizit.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle

class VizitHostApduService : HostApduService() {
    private lateinit var payloadStore: HcePayloadStore
    private var processor: Type4TagApduProcessor? = null

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
        }

        return processor?.process(commandApdu)
            ?: Type4TagApduProcessor.STATUS_SECURITY_NOT_SATISFIED
    }

    override fun onDeactivated(reason: Int) {
        processor = null
    }
}
