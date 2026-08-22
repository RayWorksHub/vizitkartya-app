package hu.rayworks.vizit.nfc

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

sealed interface NfcShareEvent {
    data object PayloadRead : NfcShareEvent
    data class Deactivated(val reason: Int) : NfcShareEvent
}

object NfcShareEvents {
    private val mutableEvents = MutableSharedFlow<NfcShareEvent>(extraBufferCapacity = 8)
    val events = mutableEvents.asSharedFlow()

    fun emit(event: NfcShareEvent) {
        mutableEvents.tryEmit(event)
    }
}
