package hu.rayworks.vizit.nfc

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface NfcShareEvent {
    val sessionId: Long

    data class PayloadRead(
        override val sessionId: Long,
        val payloadBytes: Int,
    ) : NfcShareEvent

    data class Deactivated(
        override val sessionId: Long,
        val reason: Int,
    ) : NfcShareEvent
}

object NfcShareEvents {
    private val mutableEvents = MutableSharedFlow<NfcShareEvent>(extraBufferCapacity = 8)
    val events = mutableEvents.asSharedFlow()

    fun emit(event: NfcShareEvent) {
        mutableEvents.tryEmit(event)
    }
}
