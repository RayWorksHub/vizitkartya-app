package hu.rayworks.vizit.nfc

import hu.rayworks.vizit.data.ContactProfile
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardBuilderTest {
    @Test
    fun `build creates compatible vCard with contact fields`() {
        val profile = ContactProfile(
            fullName = "Teszt Elek",
            jobTitle = "Fejlesztő",
            company = "VIZIT",
            phone = "+36 30 123 4567",
            email = "teszt@example.com",
            website = "https://vizit.hu",
        )

        val result = VCardBuilder.build(profile)

        assertTrue(result.startsWith("BEGIN:VCARD\r\nVERSION:3.0\r\n"))
        assertTrue(result.contains("FN:Teszt Elek\r\n"))
        assertTrue(result.contains("TEL;TYPE=CELL:+36 30 123 4567\r\n"))
        assertTrue(result.contains("EMAIL;TYPE=INTERNET:teszt@example.com\r\n"))
        assertTrue(result.endsWith("END:VCARD\r\n"))
    }

    @Test
    fun `build escapes reserved characters and omits empty photo`() {
        val result = VCardBuilder.build(
            ContactProfile(
                fullName = "Minta, Mária",
                phone = "+36 1 111 1111",
                company = "Példa; Kft.",
            ),
        )

        assertTrue(result.contains("FN:Minta\\, Mária"))
        assertTrue(result.contains("ORG:Példa\\; Kft."))
        assertFalse(result.contains("PHOTO"))
    }

    @Test
    fun `build can omit photo for size constrained transports and add VIZIT URL`() {
        val result = VCardBuilder.build(
            profile = ContactProfile(
                fullName = "Árvíztűrő Tükörfúrógép",
                phone = "+36 30 123 4567",
                photoBase64 = "AAABBBCCC",
            ),
            includePhoto = false,
            vizitProfileUrl = "https://vizit.hu/p/arvizturo",
        )

        assertFalse(result.contains("PHOTO"))
        assertTrue(result.contains("FN:Árvíztűrő Tükörfúrógép"))
        assertTrue(result.contains("URL;TYPE=VIZIT:https://vizit.hu/p/arvizturo"))
    }
}
