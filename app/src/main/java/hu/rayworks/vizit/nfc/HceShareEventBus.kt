package hu.rayworks.vizit.nfc

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface HceShareEvent {
    val sessionId: Long

    data class PayloadRead(
        override val sessionId: Long,
        val payloadBytes: Int,
    ) : HceShareEvent

    data class LinkDeactivated(
        override val sessionId: Long,
        val reason: Int,
    ) : HceShareEvent
}

object HceShareEventBus {
    private val mutableEvents = MutableSharedFlow<HceShareEvent>(extraBufferCapacity = 8)

    val events = mutableEvents.asSharedFlow()

    fun publish(event: HceShareEvent) {
        mutableEvents.tryEmit(event)
    }
}
