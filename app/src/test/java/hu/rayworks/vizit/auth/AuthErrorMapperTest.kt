package hu.rayworks.vizit.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AuthErrorMapperTest {
    @Test fun `known auth errors are translated`() {
        assertEquals(
            "Hibás e-mail-cím vagy jelszó.",
            authErrorMessage("Invalid login credentials"),
        )
        assertEquals(
            "Az e-mail-cím még nincs megerősítve. Ellenőrizd a postafiókodat.",
            authErrorMessage("Email not confirmed"),
        )
        assertEquals(
            "Ezzel az e-mail-címmel már létezik fiók.",
            authErrorMessage("User already registered"),
        )
        assertEquals(
            "A jelszó nem felel meg a biztonsági követelményeknek.",
            authErrorMessage("Weak password"),
        )
    }

    @Test fun `rate limit signup and expired link errors are translated`() {
        assertEquals(
            "Túl sok próbálkozás történt. Várj néhány percet, majd próbáld újra.",
            authErrorMessage("Email rate limit exceeded"),
        )
        assertEquals(
            "A regisztráció jelenleg nem érhető el.",
            authErrorMessage("Signups not allowed for this instance"),
        )
        assertEquals(
            "A hitelesítő link lejárt vagy már felhasználták. Kérj új levelet.",
            authErrorMessage("OTP_expired token link"),
        )
    }

    @Test fun `network failures have retry guidance`() {
        assertEquals(
            "Hálózati hiba történt. Ellenőrizd az internetkapcsolatot, majd próbáld újra.",
            authErrorMessage("Unable to resolve host"),
        )
    }

    @Test fun `unknown server details never reach the user`() {
        val raw = "database failure for private-user@example.com at internal host"
        val mapped = authErrorMessage(raw)
        assertEquals("A művelet nem sikerült. Próbáld újra.", mapped)
        assertFalse(mapped.contains("private-user"))
    }

    @Test fun `disabled confirmation cannot create a silent session`() {
        assertEquals(
            "Az e-mailes megerősítés átmenetileg nem érhető el. Próbáld újra később.",
            authErrorMessage(EmailConfirmationNotEnforcedException()),
        )
    }
}
