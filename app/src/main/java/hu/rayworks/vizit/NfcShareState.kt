package hu.rayworks.vizit

sealed interface NfcShareState {
    data object Idle : NfcShareState

    data object Preparing : NfcShareState

    data class Active(
        val sessionId: Long,
        val payloadBytes: Int,
        val embeddedPhotoBytes: Int,
    ) : NfcShareState

    data class PayloadRead(
        val sessionId: Long,
        val payloadBytes: Int,
    ) : NfcShareState

    data object TimedOut : NfcShareState

    data class Error(val message: String) : NfcShareState
}
