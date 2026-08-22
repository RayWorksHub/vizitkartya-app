package hu.rayworks.vizit.nfc

import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HcePayloadStoreTest {
    private var now = 1_000L
    private val store = HcePayloadStore(clock = { now })

    @After
    fun cleanUp() {
        store.deactivate()
    }

    @Test
    fun `payload is copied and expires at TTL`() {
        val source = byteArrayOf(1, 2, 3)
        store.activate(source, ttlMillis = 500)
        source[0] = 9

        assertArrayEquals(byteArrayOf(1, 2, 3), store.activePayload()?.bytes)
        now += 500
        assertNull(store.activePayload())
        assertFalse(store.isActive())
    }

    @Test
    fun `stale session cannot deactivate newer share`() {
        val first = store.activate(byteArrayOf(1))
        val second = store.activate(byteArrayOf(2))

        assertNotEquals(first, second)
        assertFalse(store.deactivate(first))
        assertTrue(store.isActive())
        assertTrue(store.deactivate(second))
    }
}
