package hu.rayworks.vizit.data.card

import hu.rayworks.vizit.data.ContactProfile
import hu.rayworks.vizit.nfc.VCardBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CardPresentationTest {
    private fun profile() = ContactProfile(
        fullName = "Teszt Elek",
        firstName = "Elek",
        lastName = "Teszt",
        jobTitle = "CEO",
        company = "Minta Kft.",
        phone = "+36201234567",
        email = "teszt@example.com",
        website = "https://pelda.hu",
        address = "Budapest, Fő utca 1.",
        linkedIn = "https://linkedin.com/in/teszt",
    )

    @Test
    fun `defaults share every optional field`() {
        val defaults = CardPresentation()
        assertEquals(CardPresentation.OPTIONAL_FIELD_COUNT, defaults.sharedFieldCount)
        assertEquals(profile(), profile().visibleThrough(defaults))
    }

    @Test
    fun `hidden fields never reach the shared vCard`() {
        val presentation = CardPresentation(
            sharesEmail = false,
            sharesAddress = false,
            sharesSocial = false,
        )
        val shared = profile().visibleThrough(presentation)

        assertEquals("", shared.email)
        assertEquals("", shared.address)
        assertEquals("", shared.linkedIn)
        // Everything left switched on survives untouched.
        assertEquals(profile().phone, shared.phone)
        assertEquals(profile().company, shared.company)
        assertEquals(profile().website, shared.website)
        assertEquals(CardPresentation.OPTIONAL_FIELD_COUNT - 3, presentation.sharedFieldCount)

        val payload = VCardBuilder.build(shared, includePhoto = false)
        assertFalse(payload.contains("teszt@example.com"))
        assertFalse(payload.contains("linkedin.com"))
        assertFalse(payload.contains("ADR;"))
        assertTrue(payload.contains("+36201234567"))
    }

    @Test
    fun `hiding the company hides the job title with it`() {
        val shared = profile().visibleThrough(CardPresentation(sharesCompany = false))
        assertEquals("", shared.company)
        assertEquals("", shared.jobTitle)
        assertFalse(VCardBuilder.build(shared, includePhoto = false).contains("TITLE:"))
    }
}
