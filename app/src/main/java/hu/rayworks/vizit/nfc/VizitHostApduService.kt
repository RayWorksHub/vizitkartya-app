package hu.rayworks.vizit.nfc

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import android.os.Handler
import android.os.Looper

class VizitHostApduService : HostApduService() {
    private val payloadStore = HcePayloadStore()
    private var processor: Type4TagApduProcessor? = null
    private var processorSessionId: Long? = null
    private var completionPendingSessionId: Long? = null
    private var completionRunnable: Runnable? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        if (Type4TagApduProcessor.isSelectApplication(commandApdu)) {
            val activePayload = payloadStore.activePayload()
                ?: return Type4TagApduProcessor.STATUS_SECURITY_NOT_SATISFIED

            // Android/OEM NDEF readers are allowed to select and read the same emulated
            // Type 4 tag more than once during one physical tap. A new SELECT therefore
            // cancels a pending "read complete" shutdown instead of letting an older
            // delayed callback cut the second read in half.
            cancelPendingCompletion()
            processorSessionId = activePayload.sessionId
            processor = Type4TagApduProcessor(activePayload.bytes) { progress ->
                if (progress.isComplete && completionPendingSessionId != activePayload.sessionId) {
                    scheduleCompletion(
                        sessionId = activePayload.sessionId,
                        payloadBytes = activePayload.bytes.size,
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

    private fun scheduleCompletion(sessionId: Long, payloadBytes: Int) {
        cancelPendingCompletion()
        completionPendingSessionId = sessionId

        val runnable = Runnable {
            if (completionPendingSessionId != sessionId) return@Runnable
            completionPendingSessionId = null
            completionRunnable = null
            if (payloadStore.deactivate(sessionId)) {
                NfcShareEvents.emit(
                    NfcShareEvent.PayloadRead(
                        sessionId = sessionId,
                        payloadBytes = payloadBytes,
                    ),
                )
            }
        }
        completionRunnable = runnable

        // Do not tear down the emulated tag immediately after the last READ BINARY.
        // Samsung/Xiaomi/other readers can re-select or re-read NDEF during the same
        // tap. Keeping the route alive briefly makes those second-pass reads succeed.
        mainHandler.postDelayed(runnable, READ_SETTLE_DELAY_MILLIS)
    }

    private fun cancelPendingCompletion() {
        completionRunnable?.let(mainHandler::removeCallbacks)
        completionRunnable = null
        completionPendingSessionId = null
    }

    override fun onDeactivated(reason: Int) {
        processorSessionId?.let { sessionId ->
            NfcShareEvents.emit(NfcShareEvent.Deactivated(sessionId, reason))
        }
        processor = null
        processorSessionId = null
    }

    override fun onDestroy() {
        cancelPendingCompletion()
        super.onDestroy()
    }

    private companion object {
        const val READ_SETTLE_DELAY_MILLIS = 1_500L
    }
}
