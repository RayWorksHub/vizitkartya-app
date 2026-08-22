package hu.rayworks.vizit.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthValidatorTest {
    @Test fun `valid registration passes`() {
        assertNull(AuthValidator.registration("teszt@vizit.hu", "Titkos123", "Titkos123", true))
    }

    @Test fun `invalid email is rejected`() {
        assertEquals("Az e-mail-cím formátuma nem megfelelő.", AuthValidator.email("nem-email"))
    }

    @Test fun `weak password is rejected`() {
        assertEquals("A jelszó legalább 8 karakter hosszú legyen.", AuthValidator.password("Aa1"))
    }

    @Test fun `blank password has explicit feedback`() {
        assertEquals("Add meg a jelszavadat.", AuthValidator.password(""))
    }

    @Test fun `login requires a password`() {
        assertEquals(
            "Add meg a jelszavadat.",
            AuthValidator.login("teszt@vizit.hu", ""),
        )
    }

    @Test fun `different confirmation is rejected`() {
        assertEquals(
            "A két jelszó nem egyezik.",
            AuthValidator.registration("teszt@vizit.hu", "Titkos123", "Titkos124", true),
        )
    }

    @Test fun `missing legal acceptance is rejected with visible message`() {
        assertEquals(
            "A regisztrációhoz fogadd el az adatkezelési tájékoztatót és az ÁSZF-et.",
            AuthValidator.registration("teszt@vizit.hu", "Titkos123", "Titkos123", false),
        )
    }

    @Test fun `password change requires confirmation`() {
        assertEquals(
            "Ismételd meg az új jelszavadat.",
            AuthValidator.passwordChange("Titkos123", ""),
        )
    }

    @Test fun `password change rejects different confirmation`() {
        assertEquals(
            "A két jelszó nem egyezik.",
            AuthValidator.passwordChange("Titkos123", "Titkos124"),
        )
    }

    @Test fun `account deletion accepts exact phrase case insensitively`() {
        assertNull(AuthValidator.accountDeletionConfirmation("  törlés  "))
    }

    @Test fun `account deletion rejects missing confirmation`() {
        assertEquals(
            "A törlés megerősítéséhez írd be: TÖRLÉS",
            AuthValidator.accountDeletionConfirmation("töröl"),
        )
    }
}
