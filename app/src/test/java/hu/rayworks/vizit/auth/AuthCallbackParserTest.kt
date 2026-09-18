package hu.rayworks.vizit.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthCallbackParserTest {
    @Test fun `recovery query is accepted on the configured callback`() {
        assertEquals(
            AuthCallback.PasswordRecovery,
            AuthCallbackParser.parse(
                "vizit-dev://auth-callback?flow=recovery&code=opaque",
                "vizit-dev",
            ),
        )
    }

    @Test fun `recovery fragment is accepted for implicit callback compatibility`() {
        assertEquals(
            AuthCallback.PasswordRecovery,
            AuthCallbackParser.parse(
                "vizit-dev://auth-callback#type=recovery&access_token=opaque",
                "vizit-dev",
            ),
        )
    }

    @Test fun `provider error is returned without exposing its description`() {
        assertEquals(
            AuthCallback.Error("otp_expired"),
            AuthCallbackParser.parse(
                "vizit-dev://auth-callback?error_code=otp_expired&error_description=sensitive",
                "vizit-dev",
            ),
        )
    }

    @Test fun `generic callback is accepted`() {
        assertEquals(
            AuthCallback.Generic,
            AuthCallbackParser.parse("vizit-dev://auth-callback?code=opaque", "vizit-dev"),
        )
    }

    @Test fun `wrong scheme is rejected`() {
        assertNull(
            AuthCallbackParser.parse(
                "vizit://auth-callback?flow=recovery",
                "vizit-dev",
            ),
        )
    }

    @Test fun `host substring spoof is rejected`() {
        assertNull(
            AuthCallbackParser.parse(
                "vizit-dev://auth-callback.attacker.invalid?flow=recovery",
                "vizit-dev",
            ),
        )
    }

    @Test fun `userinfo port and unexpected path are rejected`() {
        assertNull(AuthCallbackParser.parse("vizit-dev://user@auth-callback?code=x", "vizit-dev"))
        assertNull(AuthCallbackParser.parse("vizit-dev://auth-callback:443?code=x", "vizit-dev"))
        assertNull(AuthCallbackParser.parse("vizit-dev://auth-callback/other?code=x", "vizit-dev"))
    }

    @Test fun `malformed and oversized callbacks are rejected`() {
        assertNull(AuthCallbackParser.parse("not a URI", "vizit-dev"))
        assertNull(AuthCallbackParser.parse("x".repeat(4097), "vizit-dev"))
    }

    @Test fun `callback errors have safe Hungarian messages`() {
        assertEquals(
            "A hitelesítő link lejárt vagy már felhasználták. Kérj új levelet.",
            authCallbackErrorMessage("otp_expired"),
        )
        assertEquals("A hitelesítés megszakadt.", authCallbackErrorMessage("access_denied"))
        assertEquals(
            "A hitelesítő link feldolgozása nem sikerült. Kérj új levelet.",
            authCallbackErrorMessage("unexpected_internal_detail"),
        )
    }
}
