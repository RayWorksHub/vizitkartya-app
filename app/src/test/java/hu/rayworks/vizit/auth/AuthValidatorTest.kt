package hu.rayworks.vizit.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {
    @Test fun `valid registration passes`() {
        assertNull(AuthValidator.registration("teszt@vizit.hu", "Titkos123", "Titkos123"))
    }

    @Test fun `invalid email is rejected`() {
        assertEquals("Az e-mail-cím formátuma nem megfelelő.", AuthValidator.email("nem-email"))
    }

    @Test fun `weak password is rejected`() {
        assertEquals("A jelszó legalább 8 karakter hosszú legyen.", AuthValidator.password("Aa1"))
    }

    @Test fun `different confirmation is rejected`() {
        assertEquals(
            "A két jelszó nem egyezik.",
            AuthValidator.registration("teszt@vizit.hu", "Titkos123", "Titkos124"),
        )
    }
}
