package hu.rayworks.vizit.auth

sealed interface AuthSessionState {
    data object Initializing : AuthSessionState
    data object SignedOut : AuthSessionState
    data object Authenticated : AuthSessionState
    data class RefreshFailed(val message: String) : AuthSessionState
    data object BackendUnavailable : AuthSessionState
}

sealed interface AuthActionState {
    data object Idle : AuthActionState
    data object Loading : AuthActionState
    data class Success(val message: String) : AuthActionState
    data class Error(val message: String) : AuthActionState
}

enum class AuthScreenMode { LOGIN, REGISTER, FORGOT_PASSWORD, NEW_PASSWORD }
