package hu.rayworks.vizit.qr

import hu.rayworks.vizit.data.ContactProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QrPayloadFactoryTest {
    @Test
    fun `contact QR is a vCard and omits embedded photo`() {
        val payload = QrPayloadFactory.contact(
            profile = ContactProfile(
                fullName = "Őrült Árvíztűrő",
                phone = "+36 30 123 4567",
                photoBase64 = "very-large-photo-data",
            ),
            profileUrl = null,
        ).getOrThrow()

        assertTrue(payload.startsWith("BEGIN:VCARD\r\n"))
        assertTrue(payload.contains("FN:Őrült Árvíztűrő"))
        assertFalse(payload.contains("PHOTO"))
        assertTrue(payload.toByteArray(Charsets.UTF_8).size <= QrPayloadFactory.MAX_CONTACT_QR_BYTES)
    }
}
