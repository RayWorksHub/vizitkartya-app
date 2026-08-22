package hu.rayworks.vizit.auth

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

sealed interface AuthCallback {
    data object Generic : AuthCallback
    data object PasswordRecovery : AuthCallback
    data class Error(val code: String?) : AuthCallback
}

object AuthCallbackParser {
    fun parse(rawUrl: String, expectedScheme: String): AuthCallback? {
        if (rawUrl.length > MAX_CALLBACK_LENGTH || expectedScheme.isBlank()) return null
        val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return null
        if (!uri.scheme.equals(expectedScheme, ignoreCase = true)) return null
        if (!uri.host.equals(AUTH_CALLBACK_HOST, ignoreCase = true)) return null
        if (uri.userInfo != null || uri.port != -1) return null
        if (!uri.path.isNullOrEmpty() && uri.path != "/") return null

        val parameters = parseParameters(uri.rawQuery) + parseParameters(uri.rawFragment)
        val errorCode = parameters["error_code"] ?: parameters["error"]
        if (!errorCode.isNullOrBlank()) return AuthCallback.Error(errorCode)

        val flow = parameters["flow"] ?: parameters["type"]
        return if (flow.equals("recovery", ignoreCase = true)) {
            AuthCallback.PasswordRecovery
        } else {
            AuthCallback.Generic
        }
    }

    private fun parseParameters(raw: String?): Map<String, String> = raw
        ?.split('&')
        ?.mapNotNull { part ->
            val separator = part.indexOf('=')
            if (separator < 1) return@mapNotNull null
            decode(part.substring(0, separator)) to decode(part.substring(separator + 1))
        }
        ?.toMap()
        .orEmpty()

    private fun decode(value: String): String = runCatching {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }.getOrDefault(value)

    const val AUTH_CALLBACK_HOST = "auth-callback"
    private const val MAX_CALLBACK_LENGTH = 4096
}

fun authCallbackErrorMessage(code: String?): String = when (code?.lowercase()) {
    "otp_expired", "expired_token" ->
        "A hitelesítő link lejárt vagy már felhasználták. Kérj új levelet."

    "access_denied" -> "A hitelesítés megszakadt."
    else -> "A hitelesítő link feldolgozása nem sikerült. Kérj új levelet."
}
