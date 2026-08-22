package hu.rayworks.vizit.data.sync

object SyncRetryPolicy {
    const val INITIAL_DELAY_MILLIS = 30_000L
    const val MAX_DELAY_MILLIS = 6 * 60 * 60 * 1_000L

    fun delayMillis(attemptCount: Int): Long {
        val exponent = attemptCount.coerceIn(0, 10)
        val multiplier = 1L shl exponent
        return (INITIAL_DELAY_MILLIS * multiplier).coerceAtMost(MAX_DELAY_MILLIS)
    }
}
