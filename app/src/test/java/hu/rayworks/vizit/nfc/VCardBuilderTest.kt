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
    fun `build writes structured Hungarian name and public profile URL`() {
        val result = VCardBuilder.build(
            ContactProfile(
                firstName = "Rajmund",
                lastName = "Csukárdi",
                phone = "+36 30 123 4567",
                publicProfileUrl = "https://vizit.hu/p/rajmund",
            ),
        )

        assertTrue(result.contains("N:Csukárdi;Rajmund;;;\r\n"))
        assertTrue(result.contains("FN:Csukárdi Rajmund\r\n"))
        assertTrue(result.contains("URL;TYPE=VIZIT:https://vizit.hu/p/rajmund\r\n"))
    }

    @Test
    fun `folding respects 75 UTF-8 octets and preserves unicode code points`() {
        val result = VCardBuilder.build(
            ContactProfile(
                fullName = "Árvíztűrő tükörfúrógép ".repeat(8),
                phone = "+36 30 123 4567",
            ),
        )

        result.split("\r\n")
            .filter(String::isNotEmpty)
            .forEach { line ->
                assertTrue("Line exceeds 75 octets: $line", line.toByteArray(Charsets.UTF_8).size <= 75)
            }
        assertFalse(result.contains("�"))
    }
}
