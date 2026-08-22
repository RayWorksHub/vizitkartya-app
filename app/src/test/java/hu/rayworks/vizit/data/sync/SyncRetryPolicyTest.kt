package hu.rayworks.vizit.data.sync

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncRetryPolicyTest {
    @Test
    fun `uses capped exponential backoff`() {
        assertEquals(30_000L, SyncRetryPolicy.delayMillis(0))
        assertEquals(60_000L, SyncRetryPolicy.delayMillis(1))
        assertEquals(120_000L, SyncRetryPolicy.delayMillis(2))
        assertEquals(SyncRetryPolicy.MAX_DELAY_MILLIS, SyncRetryPolicy.delayMillis(100))
        assertEquals(30_000L, SyncRetryPolicy.delayMillis(-1))
    }
}
