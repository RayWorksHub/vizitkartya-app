package hu.rayworks.vizit.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactProfileValidatorTest {
    @Test
    fun `accepts full HTTPS social profile links`() {
        val profile = ContactProfile(
            fullName = "Teszt Elek",
            phone = "+361234567",
            linkedIn = "https://linkedin.com/in/teszt",
            facebook = "https://facebook.com/teszt",
            instagram = "https://instagram.com/teszt",
            tiktok = "https://tiktok.com/@teszt",
            youtube = "https://youtube.com/@teszt",
        )

        assertNull(ContactProfileValidator.validate(profile))
    }

    @Test
    fun `rejects insecure or partial social profile links`() {
        val message = "A webes és közösségi hivatkozások teljes, https:// kezdetű címek legyenek."
        val base = ContactProfile(fullName = "Teszt Elek", phone = "+361234567")

        assertEquals(message, ContactProfileValidator.validate(base.copy(instagram = "instagram.com/teszt")))
        assertEquals(message, ContactProfileValidator.validate(base.copy(youtube = "http://youtube.com/@teszt")))
    }
}
