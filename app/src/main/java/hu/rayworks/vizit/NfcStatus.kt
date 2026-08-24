package hu.rayworks.vizit

data class NfcStatus(
    val isAvailable: Boolean,
    val isEnabled: Boolean,
    val hasHostCardEmulation: Boolean,
) {
    val isReady: Boolean
        get() = isAvailable && isEnabled && hasHostCardEmulation
}

enum class NfcSharePhase {
    IDLE,
    WAITING,
    PAYLOAD_READ,
    TIMED_OUT,
    ROUTING_FAILED,
}
