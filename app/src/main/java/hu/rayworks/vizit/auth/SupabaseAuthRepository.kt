package hu.rayworks.vizit.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.functions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseAuthRepository(private val client: SupabaseClient) {
    val sessionState: Flow<AuthSessionState> = client.auth.sessionStatus.map { status ->
        when (status) {
            SessionStatus.Initializing -> AuthSessionState.Initializing
            is SessionStatus.Authenticated -> status.session.user?.id
                ?.let(AuthSessionState::Authenticated)
                ?: AuthSessionState.SignedOut
            is SessionStatus.NotAuthenticated -> AuthSessionState.SignedOut
            is SessionStatus.RefreshFailure -> AuthSessionState.RefreshFailed(
                "A munkamenet megújítása nem sikerült. Ellenőrizd az internetkapcsolatot, majd jelentkezz be újra.",
                client.auth.currentSessionOrNull()?.user?.id,
            )
        }
    }

    suspend fun register(
        email: String,
        password: String,
        privacyPolicyVersion: String,
        termsVersion: String,
    ) {
        client.auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject {
                put("privacy_policy_version", privacyPolicyVersion)
                put("terms_version", termsVersion)
            }
        }
    }

    suspend fun login(email: String, password: String) {
        client.auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    suspend fun requestPasswordReset(email: String) {
        client.auth.resetPasswordForEmail(email.trim())
    }

    suspend fun updatePassword(newPassword: String) {
        client.auth.updateUser { password = newPassword }
    }

    suspend fun logout() {
        client.auth.signOut()
    }

    suspend fun deleteAccount() {
        client.functions.invoke("delete-account")
    }
}

fun authErrorMessage(error: Throwable): String {
    val raw = error.message.orEmpty().lowercase()
    return when {
        "invalid login credentials" in raw -> "Hibás e-mail-cím vagy jelszó."
        "email not confirmed" in raw -> "Az e-mail-cím még nincs megerősítve. Ellenőrizd a postafiókodat."
        "user already registered" in raw -> "Ezzel az e-mail-címmel már létezik fiók."
        "weak password" in raw || "password" in raw && "weak" in raw -> "A jelszó nem felel meg a biztonsági követelményeknek."
        "timeout" in raw || "network" in raw || "unable to resolve host" in raw -> "Hálózati hiba történt. Ellenőrizd az internetkapcsolatot."
        else -> "A művelet nem sikerült. Próbáld újra."
    }
}
