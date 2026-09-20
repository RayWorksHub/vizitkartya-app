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

    @Test
    fun `generates readable identifier and accepts only hostname custom domains`() {
        assertEquals(
            "csukardi-rajmund",
            PublicProfileUrlFactory.automaticSlug("Csukárdi Rajmund", "6bc9b40d-5317-4f78-a1d6-c44d50f4e4f4"),
        )
        assertTrue(PublicProfileUrlFactory.isValidCustomDomain("https://Nevjegy.Example.HU/"))
        assertTrue(!PublicProfileUrlFactory.isValidCustomDomain("https://example.hu/path"))
    }
}
