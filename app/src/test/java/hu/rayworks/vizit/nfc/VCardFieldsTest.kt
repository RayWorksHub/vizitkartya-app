package hu.rayworks.vizit.nfc

import hu.rayworks.vizit.data.ContactProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VCardFieldsTest {
    @Test
    fun `reads back the card this app generates`() {
        val profile = ContactProfile(
            fullName = "Teszt Elek",
            firstName = "Elek",
            lastName = "Teszt",
            jobTitle = "Ügyvezető",
            company = "Minta; Kft.",
            phone = "+36201234567",
            email = "teszt@example.com",
            address = "Budapest, Fő utca 1.",
        )
        val fields = VCardFields.parse(VCardBuilder.build(profile, includePhoto = false))

        assertEquals("Teszt Elek", fields["FN"])
        // The escaped semicolon is data, not a structural separator.
        assertEquals("Minta; Kft.", fields["ORG"])
        assertEquals("Ügyvezető", fields["TITLE"])
        assertEquals("+36201234567", fields["TEL"])
        assertEquals("teszt@example.com", fields["EMAIL"])
        assertEquals("Budapest, Fő utca 1.", fields["ADR"])
    }

    @Test
    fun `survives folded lines and keeps only the first value of a property`() {
        val folded = "BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Nagyon Hosszú Ne\r\n vű Ember\r\n" +
            "TEL;TYPE=CELL:+36201111111\r\nTEL;TYPE=WORK:+36202222222\r\nEND:VCARD\r\n"
        val fields = VCardFields.parse(folded)

        assertEquals("Nagyon Hosszú Nevű Ember", fields["FN"])
        assertEquals("+36201111111", fields["TEL"])
    }

    @Test
    fun `ignores properties the contact editor cannot take`() {
        val fields = VCardFields.parse(
            "BEGIN:VCARD\r\nFN:Teszt Elek\r\nPHOTO;ENCODING=b;TYPE=JPEG:/9j/4AAQ\r\n" +
                "X-SOCIALPROFILE;TYPE=linkedin:https://linkedin.com/in/teszt\r\nEND:VCARD\r\n",
        )
        assertEquals(setOf("FN"), fields.keys)
        assertFalse(fields.containsKey("PHOTO"))
    }

    @Test
    fun `refuses an oversized payload instead of parsing it`() {
        assertTrue(VCardFields.parse("FN:" + "a".repeat(70_000)).isEmpty())
    }

    @Test
    fun `drops a single value that is too long to be a real field`() {
        assertTrue(VCardFields.parse("FN:" + "a".repeat(600)).isEmpty())
    }
}
