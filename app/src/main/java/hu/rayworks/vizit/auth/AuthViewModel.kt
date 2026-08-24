package hu.rayworks.vizit.auth

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.VizitApplication
import hu.rayworks.vizit.config.AppEnvironment
import hu.rayworks.vizit.data.remote.SupabaseProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val container = (application as VizitApplication).container
    private val client = SupabaseProvider.getOrNull()
    private val settingsStore = container.settingsStore
    private val repository = client?.let { actualClient ->
        SupabaseAuthRepository(
            client = actualClient,
            settingsStore = settingsStore,
            legalDocumentsReady = BuildConfig.LEGAL_DOCUMENTS_READY,
            privacyPolicyVersion = BuildConfig.PRIVACY_POLICY_VERSION,
            termsVersion = BuildConfig.TERMS_VERSION,
        )
    }
    private val googleAdapter = client?.let(::GoogleAuthAdapter)
    private val unavailable = MutableStateFlow<AuthSessionState>(AuthSessionState.BackendUnavailable)

    val sessionState: StateFlow<AuthSessionState> = repository?.sessionState?.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AuthSessionState.Initializing,
    ) ?: unavailable

    var actionState by mutableStateOf<AuthActionState>(AuthActionState.Idle)
        private set
    var debugLocalProfile by mutableStateOf(false)
        private set
    var passwordRecovery by mutableStateOf(false)
        private set

    val canUseDebugLocalProfile: Boolean
        get() = BuildConfig.DEBUG && AppEnvironment.current == AppEnvironment.DEV
    val googleSignInEnabled: Boolean
        get() = googleAdapter?.isEnabled == true && legalDocumentsReady
    val cloudAccountAvailable: Boolean
        get() = repository != null && !debugLocalProfile
    val legalDocumentsReady: Boolean
        get() = BuildConfig.LEGAL_DOCUMENTS_READY
    val privacyPolicyUrl: String
        get() = BuildConfig.PRIVACY_POLICY_URL
    val termsUrl: String
        get() = BuildConfig.TERMS_URL

    fun useDebugLocalProfile() {
        if (canUseDebugLocalProfile) debugLocalProfile = true
    }

    fun register(
        name: String,
        email: String,
        password: String,
        confirmation: String,
        legalAccepted: Boolean,
    ) {
        if (!legalDocumentsReady) {
            return setError("A regisztráció jogi dokumentumai még nincsenek konfigurálva.")
        }
        val validation = AuthValidator.registration(name, email, password, confirmation, legalAccepted)
        if (validation != null) return setError(validation)
        runAction(
            operation = AuthOperation.REGISTER,
            successMessage = "Megerősítő e-mailt küldtünk. Ellenőrizd a postafiókodat.",
        ) {
            repositoryOrThrow().register(
                name = name,
                email = email,
                password = password,
                redirectUrl = "${BuildConfig.AUTH_SCHEME}://auth-callback",
                privacyPolicyVersion = BuildConfig.PRIVACY_POLICY_VERSION,
                termsVersion = BuildConfig.TERMS_VERSION,
            )
        }
    }

    fun login(email: String, password: String) {
        val validation = AuthValidator.login(email, password)
        if (validation != null) return setError(validation)
        runAction(AuthOperation.LOGIN, "Sikeres bejelentkezés.") {
            repositoryOrThrow().login(email, password)
        }
    }

    fun requestPasswordReset(email: String) {
        val validation = AuthValidator.email(email)
        if (validation != null) return setError(validation)
        runAction(
            operation = AuthOperation.PASSWORD_RESET_REQUEST,
            successMessage = "Ha az e-mail-címhez tartozik fiók, elküldtük a jelszó-helyreállítási levelet.",
        ) {
            repositoryOrThrow().requestPasswordReset(
                email = email,
                redirectUrl = "${BuildConfig.AUTH_SCHEME}://auth-callback?flow=recovery",
            )
        }
    }

    fun updatePassword(password: String, confirmation: String) {
        val validation = AuthValidator.passwordChange(password, confirmation)
        if (validation != null) return setError(validation)
        runAction(AuthOperation.PASSWORD_UPDATE, "Az új jelszó mentve.") {
            repositoryOrThrow().updatePassword(password)
            passwordRecovery = false
        }
    }

    fun acceptLegalDocuments(accepted: Boolean) {
        if (!legalDocumentsReady) return setError("A jogi dokumentumok még nincsenek konfigurálva.")
        if (!accepted) {
            return setError("A folytatáshoz fogadd el az adatkezelési tájékoztatót és az ÁSZF-et.")
        }
        runAction(AuthOperation.LEGAL_ACCEPTANCE, "A jogi elfogadás rögzítve.") {
            repositoryOrThrow().acceptLegalDocuments()
        }
    }

    fun retryLegalAcceptanceCheck() {
        clearActionState()
        repository?.refreshLegalAcceptance()
    }

    fun logout() = runAction(AuthOperation.LOGOUT, "Kijelentkeztél.") {
        repositoryOrThrow().logout()
        settingsStore.clearAuthenticationCache()
    }

    fun deleteAccount(confirmation: String) {
        val validation = AuthValidator.accountDeletionConfirmation(confirmation)
        if (validation != null) return setError(validation, AuthOperation.DELETE_ACCOUNT)
        runAction(AuthOperation.DELETE_ACCOUNT, "A fiók és a helyi profil törlése befejeződött.") {
            val authRepository = repositoryOrThrow()
            val userId = authRepository.authenticatedUserId() ?: error("Authenticated session required")
            authRepository.deleteAccount()
            container.profileRepository.deleteLocalProfile(userId)
            runCatching { authRepository.logout() }
            settingsStore.clearAuthenticationCache()
        }
    }

    fun signInWithGoogle(activity: Activity) {
        val adapter = googleAdapter ?: return setError("A Google-belépés még nincs konfigurálva.")
        runAction(AuthOperation.GOOGLE_SIGN_IN, "Sikeres Google-belépés.") {
            adapter.signIn(activity)
        }
    }

    fun markPasswordRecovery() { passwordRecovery = true }
    fun clearActionState() { actionState = AuthActionState.Idle }
    fun reportDeepLinkError(error: Throwable) {
        actionState = AuthActionState.Error(operation = null, message = authErrorMessage(error))
    }
    fun reportDeepLinkErrorCode(code: String?) {
        actionState = AuthActionState.Error(operation = null, message = authCallbackErrorMessage(code))
    }
    fun reportUiError(message: String) {
        actionState = AuthActionState.Error(operation = null, message = message)
    }

    private fun repositoryOrThrow(): SupabaseAuthRepository = repository ?: error("Supabase is not configured")
    private fun setError(message: String, operation: AuthOperation? = null) {
        actionState = AuthActionState.Error(operation = operation, message = message)
    }

    private fun runAction(
        operation: AuthOperation,
        successMessage: String,
        block: suspend () -> Unit,
    ) {
        if (actionState is AuthActionState.Loading) return
        viewModelScope.launch {
            actionState = AuthActionState.Loading(operation)
            actionState = try {
                block()
                AuthActionState.Success(operation, successMessage)
            } catch (error: Throwable) {
                AuthActionState.Error(operation, authErrorMessage(error))
            }
        }
    }
}
