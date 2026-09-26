package hu.rayworks.vizit.auth

import hu.rayworks.vizit.data.settings.AppSettingsStore
import hu.rayworks.vizit.data.remote.NodeBackendApi
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.*

class SupabaseAuthRepository(
    private val client: SupabaseClient,
    private val settingsStore: AppSettingsStore,
    private val legalDocumentsReady: Boolean,
    private val privacyPolicyVersion: String,
    private val termsVersion: String,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val backend = NodeBackendApi(client)
    private val legalAcceptanceRefresh = MutableStateFlow(0L)

    val sessionState: Flow<AuthSessionState> = combine(client.auth.sessionStatus, legalAcceptanceRefresh) { status, _ ->
        when (status) {
            SessionStatus.Initializing -> AuthSessionState.Initializing
            is SessionStatus.Authenticated -> status.session.user?.id?.let { authenticatedState(it) } ?: AuthSessionState.SignedOut
            is SessionStatus.NotAuthenticated -> AuthSessionState.SignedOut
            is SessionStatus.RefreshFailure -> refreshFailureState()
        }
    }

    // Keep the existing native confirmation and recovery deep-link flow.
    // Supabase is the Node backend's identity provider, not the profile data source.
    suspend fun register(name: String, email: String, password: String, redirectUrl: String,
                         privacyPolicyVersion: String, termsVersion: String) {
        client.auth.signUpWith(Email, redirectUrl = redirectUrl) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject {
                put("display_name", name.trim())
                put("privacy_policy_version", privacyPolicyVersion)
                put("privacy_version", privacyPolicyVersion)
                put("terms_version", termsVersion)
            }
        }
        if (client.auth.currentSessionOrNull() != null) {
            client.auth.signOut()
            throw EmailConfirmationNotEnforcedException()
        }
    }

    suspend fun login(email: String, password: String) {
        val response = backend.request("POST", "/api/auth/sign-in", buildJsonObject {
            put("email", email.trim()); put("password", password)
        }, authenticated = false)
        val expiresAt = requireNotNull(response["expires_at"]?.jsonPrimitive?.longOrNull)
        require(response["user"] is JsonObject && !response["access_token"]?.jsonPrimitive?.contentOrNull.isNullOrBlank())
        val session = buildJsonObject {
            response.forEach { (key, value) -> put(key, value) }
            put("expires_in", (expiresAt - System.currentTimeMillis() / 1000L).coerceAtLeast(1L))
        }
        client.auth.importSession(json.decodeFromJsonElement<UserSession>(session))
    }

    suspend fun requestPasswordReset(email: String, redirectUrl: String) {
        client.auth.resetPasswordForEmail(email.trim(), redirectUrl = redirectUrl)
    }
    suspend fun updatePassword(newPassword: String) { client.auth.updateUser { password = newPassword } }
    suspend fun logout() { client.auth.signOut() }
    suspend fun deleteAccount() { backend.request("DELETE", "/api/account") }

    suspend fun acceptLegalDocuments() {
        val userId = authenticatedUserId() ?: error("Authenticated session required")
        client.auth.updateUser {
            data = buildJsonObject {
                client.auth.currentSessionOrNull()?.user?.userMetadata?.forEach { (key, value) -> put(key, value) }
                put("privacy_version", privacyPolicyVersion)
                put("privacy_policy_version", privacyPolicyVersion)
                put("terms_version", termsVersion)
            }
        }
        settingsStore.rememberLegalAcceptance(userId, privacyPolicyVersion, termsVersion)
        legalAcceptanceRefresh.update { it + 1L }
    }
    fun refreshLegalAcceptance() { legalAcceptanceRefresh.update { it + 1L } }
    fun authenticatedUserId(): String? = client.auth.currentSessionOrNull()?.user?.id

    private suspend fun authenticatedState(userId: String): AuthSessionState {
        if (!legalDocumentsReady) return AuthSessionState.Authenticated(userId)
        return runCatching {
            if (hasLegalAcceptance()) {
                settingsStore.rememberLegalAcceptance(userId, privacyPolicyVersion, termsVersion)
                AuthSessionState.Authenticated(userId)
            } else AuthSessionState.LegalAcceptanceRequired(userId)
        }.getOrElse {
            if (hasCachedLegalAcceptance(userId)) AuthSessionState.Authenticated(userId)
            else AuthSessionState.LegalAcceptanceCheckFailed(userId,
                "A jogi elfogadás ellenőrzéséhez internetkapcsolat szükséges.")
        }
    }
    private suspend fun refreshFailureState(): AuthSessionState {
        val userId = authenticatedUserId() ?: return AuthSessionState.SignedOut
        return if (!legalDocumentsReady || hasCachedLegalAcceptance(userId)) AuthSessionState.RefreshFailed(
            message = "A munkamenet megújítása nem sikerült. A helyi profil továbbra is használható.", cachedUserId = userId)
        else AuthSessionState.LegalAcceptanceCheckFailed(userId,
            "A munkamenet és a jogi elfogadás ellenőrzéséhez internetkapcsolat szükséges.")
    }
    private suspend fun hasCachedLegalAcceptance(userId: String): Boolean =
        settingsStore.hasRememberedLegalAcceptance(userId, privacyPolicyVersion, termsVersion)
    private suspend fun hasLegalAcceptance(): Boolean {
        val metadata = client.auth.retrieveUserForCurrentSession().userMetadata ?: return false
        return metadata["privacy_version"]?.jsonPrimitive?.contentOrNull == privacyPolicyVersion &&
            metadata["terms_version"]?.jsonPrimitive?.contentOrNull == termsVersion
    }
}
internal class EmailConfirmationNotEnforcedException : IllegalStateException()
