package hu.rayworks.vizit.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PublicProfileUrlFactoryTest {
    @Test
    fun `normalizes slug into stable HTTPS profile URL`() {
        val result = PublicProfileUrlFactory.create("https://vizit.hu/p/", "  Kovacs-Anna  ")

        assertEquals("https://vizit.hu/p/kovacs-anna", result.getOrThrow())
    }

    @Test
    fun `rejects unsafe slug and non HTTPS base`() {
        assertTrue(PublicProfileUrlFactory.create("https://vizit.hu/p", "../anna").isFailure)
        assertTrue(PublicProfileUrlFactory.create("http://vizit.hu/p", "anna-01").isFailure)
    }
}
