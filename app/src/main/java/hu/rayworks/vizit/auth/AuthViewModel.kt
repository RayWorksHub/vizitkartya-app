package hu.rayworks.vizit.auth

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.config.AppEnvironment
import hu.rayworks.vizit.data.remote.SupabaseProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val client = SupabaseProvider.getOrNull()
    private val repository = client?.let(::SupabaseAuthRepository)
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
        get() = googleAdapter?.isEnabled == true

    fun useDebugLocalProfile() {
        if (canUseDebugLocalProfile) debugLocalProfile = true
    }

    fun register(email: String, password: String, confirmation: String) {
        val validation = AuthValidator.registration(email, password, confirmation)
        if (validation != null) return setError(validation)
        runAction("Megerősítő e-mail elküldve. A belépéshez erősítsd meg az e-mail-címedet.") {
            repositoryOrThrow().register(email, password)
        }
    }

    fun login(email: String, password: String) {
        val validation = AuthValidator.email(email) ?: if (password.isBlank()) "Add meg a jelszavadat." else null
        if (validation != null) return setError(validation)
        runAction("Sikeres bejelentkezés.") { repositoryOrThrow().login(email, password) }
    }

    fun requestPasswordReset(email: String) {
        val validation = AuthValidator.email(email)
        if (validation != null) return setError(validation)
        runAction("Ha az e-mail-címhez tartozik fiók, elküldtük a jelszó-helyreállítási levelet.") {
            repositoryOrThrow().requestPasswordReset(email)
        }
    }

    fun updatePassword(password: String, confirmation: String) {
        val validation = AuthValidator.password(password) ?: if (password != confirmation) "A két jelszó nem egyezik." else null
        if (validation != null) return setError(validation)
        runAction("Az új jelszó mentve.") {
            repositoryOrThrow().updatePassword(password)
            passwordRecovery = false
        }
    }

    fun logout() = runAction("Kijelentkeztél.") { repositoryOrThrow().logout() }

    fun deleteAccount() = runAction("A fiók törlése befejeződött.") {
        repositoryOrThrow().deleteAccount()
        runCatching { repositoryOrThrow().logout() }
    }

    fun signInWithGoogle(activity: Activity) {
        val adapter = googleAdapter ?: return setError("A Google-belépés még nincs konfigurálva.")
        runAction("Sikeres Google-belépés.") { adapter.signIn(activity) }
    }

    fun markPasswordRecovery() { passwordRecovery = true }
    fun clearActionState() { actionState = AuthActionState.Idle }
    fun reportDeepLinkError(error: Throwable) { actionState = AuthActionState.Error(authErrorMessage(error)) }

    private fun repositoryOrThrow(): SupabaseAuthRepository = repository ?: error("Supabase is not configured")
    private fun setError(message: String) { actionState = AuthActionState.Error(message) }

    private fun runAction(successMessage: String, block: suspend () -> Unit) {
        if (actionState == AuthActionState.Loading) return
        viewModelScope.launch {
            actionState = AuthActionState.Loading
            actionState = try {
                block()
                AuthActionState.Success(successMessage)
            } catch (error: Throwable) {
                AuthActionState.Error(authErrorMessage(error))
            }
        }
    }
}
