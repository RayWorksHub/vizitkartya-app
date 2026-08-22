package hu.rayworks.vizit.auth

sealed interface AuthSessionState {
    data object Initializing : AuthSessionState
    data object SignedOut : AuthSessionState
    data class Authenticated(val userId: String) : AuthSessionState
    data class LegalAcceptanceRequired(val userId: String) : AuthSessionState
    data class LegalAcceptanceCheckFailed(val userId: String, val message: String) : AuthSessionState
    data class RefreshFailed(val message: String, val cachedUserId: String?) : AuthSessionState
    data object BackendUnavailable : AuthSessionState
}

sealed interface AuthActionState {
    data object Idle : AuthActionState
    data class Loading(val operation: AuthOperation) : AuthActionState
    data class Success(val operation: AuthOperation, val message: String) : AuthActionState
    data class Error(val operation: AuthOperation?, val message: String) : AuthActionState
}

enum class AuthOperation {
    LOGIN,
    REGISTER,
    PASSWORD_RESET_REQUEST,
    PASSWORD_UPDATE,
    GOOGLE_SIGN_IN,
    LEGAL_ACCEPTANCE,
    LOGOUT,
    DELETE_ACCOUNT,
}

enum class AuthScreenMode {
    LOGIN,
    REGISTER,
    EMAIL_VERIFICATION_SENT,
    FORGOT_PASSWORD,
    PASSWORD_RESET_SENT,
    NEW_PASSWORD,
    LEGAL_ACCEPTANCE,
}
