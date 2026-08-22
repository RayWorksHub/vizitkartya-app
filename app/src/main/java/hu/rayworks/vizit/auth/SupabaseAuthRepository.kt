package hu.rayworks.vizit.auth

import hu.rayworks.vizit.data.settings.AppSettingsStore
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseAuthRepository(
    private val client: SupabaseClient,
    private val settingsStore: AppSettingsStore,
    private val legalDocumentsReady: Boolean,
    private val privacyPolicyVersion: String,
    private val termsVersion: String,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val legalAcceptanceRefresh = MutableStateFlow(0L)

    val sessionState: Flow<AuthSessionState> = combine(
        client.auth.sessionStatus,
        legalAcceptanceRefresh,
    ) { status, _ ->
        when (status) {
            SessionStatus.Initializing -> AuthSessionState.Initializing
            is SessionStatus.Authenticated -> status.session.user?.id?.let { userId ->
                authenticatedState(userId)
            } ?: AuthSessionState.SignedOut
            is SessionStatus.NotAuthenticated -> AuthSessionState.SignedOut
            is SessionStatus.RefreshFailure -> refreshFailureState()
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

    suspend fun requestPasswordReset(email: String, redirectUrl: String) {
        client.auth.resetPasswordForEmail(email.trim(), redirectUrl = redirectUrl)
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

    suspend fun acceptLegalDocuments() {
        val userId = authenticatedUserId() ?: error("Authenticated session required")
        client.postgrest.rpc(
            function = "accept_legal_documents",
            parameters = legalParameters(),
        )
        settingsStore.rememberLegalAcceptance(
            userId = userId,
            privacyPolicyVersion = privacyPolicyVersion,
            termsVersion = termsVersion,
        )
        legalAcceptanceRefresh.update { it + 1L }
    }

    fun refreshLegalAcceptance() {
        legalAcceptanceRefresh.update { it + 1L }
    }

    fun authenticatedUserId(): String? = client.auth.currentSessionOrNull()?.user?.id

    private suspend fun authenticatedState(userId: String): AuthSessionState {
        if (!legalDocumentsReady) return AuthSessionState.Authenticated(userId)
        return runCatching {
            if (hasLegalAcceptance()) {
                settingsStore.rememberLegalAcceptance(
                    userId = userId,
                    privacyPolicyVersion = privacyPolicyVersion,
                    termsVersion = termsVersion,
                )
                AuthSessionState.Authenticated(userId)
            } else {
                AuthSessionState.LegalAcceptanceRequired(userId)
            }
        }.getOrElse {
            if (hasCachedLegalAcceptance(userId)) {
                AuthSessionState.Authenticated(userId)
            } else {
                AuthSessionState.LegalAcceptanceCheckFailed(
                    userId = userId,
                    message = "A jogi elfogadás ellenőrzéséhez internetkapcsolat szükséges.",
                )
            }
        }
    }

    private suspend fun refreshFailureState(): AuthSessionState {
        val userId = authenticatedUserId()
        if (userId == null) return AuthSessionState.SignedOut
        return if (!legalDocumentsReady || hasCachedLegalAcceptance(userId)) {
            AuthSessionState.RefreshFailed(
                message = "A munkamenet megújítása nem sikerült. A helyi profil továbbra is használható.",
                cachedUserId = userId,
            )
        } else {
            AuthSessionState.LegalAcceptanceCheckFailed(
                userId = userId,
                message = "A munkamenet és a jogi elfogadás ellenőrzéséhez internetkapcsolat szükséges.",
            )
        }
    }

    private suspend fun hasCachedLegalAcceptance(userId: String): Boolean =
        settingsStore.hasRememberedLegalAcceptance(
            userId = userId,
            privacyPolicyVersion = privacyPolicyVersion,
            termsVersion = termsVersion,
        )

    private suspend fun hasLegalAcceptance(): Boolean = client.postgrest.rpc(
        function = "has_legal_acceptance",
        parameters = legalParameters(),
    ).data.let { json.decodeFromString<Boolean>(it) }

    private fun legalParameters() = buildJsonObject {
        put("p_privacy_policy_version", JsonPrimitive(privacyPolicyVersion))
        put("p_terms_version", JsonPrimitive(termsVersion))
    }
}
