package hu.rayworks.vizit.nfc

import android.os.SystemClock
import java.util.concurrent.atomic.AtomicLong

data class ActiveHcePayload(
    val sessionId: Long,
    val bytes: ByteArray,
    val expiresAtElapsedRealtime: Long,
)

/**
 * Process-local, one-shot payload store.
 *
 * Contact PII is deliberately not persisted. If Android kills the process, the share is cancelled
 * instead of silently surviving in SharedPreferences or on disk.
 */
class HcePayloadStore(
    private val clock: () -> Long = SystemClock::elapsedRealtime,
) {
    fun activate(
        payload: ByteArray,
        ttlMillis: Long = DEFAULT_TTL_MILLIS,
    ): Long {
        require(payload.isNotEmpty()) { "The HCE payload must not be empty." }
        require(ttlMillis in 1..MAX_TTL_MILLIS) { "The HCE payload TTL is invalid." }

        val sessionId = sessionIds.incrementAndGet()
        synchronized(lock) {
            activePayload = ActiveHcePayload(
                sessionId = sessionId,
                bytes = payload.copyOf(),
                expiresAtElapsedRealtime = clock() + ttlMillis,
            )
        }
        return sessionId
    }

    fun activePayload(): ActiveHcePayload? = synchronized(lock) {
        val current = activePayload ?: return@synchronized null
        if (clock() >= current.expiresAtElapsedRealtime) {
            activePayload = null
            null
        } else {
            current.copy(bytes = current.bytes.copyOf())
        }
    }

    fun isActive(): Boolean = activePayload() != null

    fun deactivate(sessionId: Long? = null): Boolean = synchronized(lock) {
        val current = activePayload ?: return@synchronized false
        if (sessionId != null && current.sessionId != sessionId) return@synchronized false
        activePayload = null
        true
    }

    companion object {
        const val DEFAULT_TTL_MILLIS = 60_000L
        const val MAX_TTL_MILLIS = 120_000L

        private val lock = Any()
        private val sessionIds = AtomicLong(0)

        @Volatile
        private var activePayload: ActiveHcePayload? = null
    }
}
