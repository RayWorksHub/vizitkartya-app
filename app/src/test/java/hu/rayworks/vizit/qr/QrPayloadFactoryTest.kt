package hu.rayworks.vizit.qr

import hu.rayworks.vizit.data.ContactProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPayloadFactoryTest {
    private val profile = ContactProfile(
        fullName = "Őrsi Ágnes",
        firstName = "Ágnes",
        lastName = "Őrsi",
        phone = "+36 30 123 4567",
        email = "agnes@example.hu",
        company = "VIZIT Kft.",
        publicProfileSlug = "orsi-agnes",
        publicProfileUrl = "https://vizit.hu/p/orsi-agnes",
        photoBase64 = "large-photo-must-not-enter-qr",
    )

    @Test
    fun `profile QR contains only stable HTTPS URL`() {
        val payload = QrPayloadFactory.createProfile(profile, "https://vizit.hu/p").getOrThrow()

        assertEquals(QrMode.PROFILE, payload.mode)
        assertEquals("https://vizit.hu/p/orsi-agnes", payload.content)
    }

    @Test
    fun `contact QR is image free UTF8 vCard within budget`() {
        val payload = QrPayloadFactory.createContact(profile).getOrThrow()

        assertEquals(QrMode.CONTACT, payload.mode)
        assertTrue(payload.content.startsWith("BEGIN:VCARD\r\n"))
        assertTrue(payload.content.contains("FN:Őrsi Ágnes"))
        assertFalse(payload.content.contains("PHOTO"))
        assertTrue(payload.byteCount <= QrPayloadFactory.MAX_CONTACT_QR_BYTES)
    }
}
