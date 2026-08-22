package hu.rayworks.vizit.data.remote

import hu.rayworks.vizit.BuildConfig
import java.net.URI

enum class BackendConfigurationStatus {
    READY,
    MISSING,
    INVALID,
}

/**
 * Public client configuration only. A service-role or secret key must never be supplied here.
 */
data class BackendConfiguration(
    val environment: String,
    val supabaseUrl: String,
    val supabasePublishableKey: String,
    val publicProfileBaseUrl: String,
    val googleAuthEnabled: Boolean,
) {
    val status: BackendConfigurationStatus
        get() = when {
            supabaseUrl.isBlank() || supabasePublishableKey.isBlank() ->
                BackendConfigurationStatus.MISSING

            validationErrors().isNotEmpty() -> BackendConfigurationStatus.INVALID
            else -> BackendConfigurationStatus.READY
        }

    val isSupabaseReady: Boolean
        get() = status == BackendConfigurationStatus.READY

    fun validationErrors(): List<String> = buildList {
        if (supabaseUrl.isNotBlank() && !supabaseUrl.isValidBackendUrl(environment)) {
            add("A Supabase URL-nek érvényes HTTPS-címnek kell lennie.")
        }
        if (publicProfileBaseUrl.isBlank() || !publicProfileBaseUrl.isValidHttpsUrl()) {
            add("A publikus profil alap URL-je nem érvényes HTTPS-cím.")
        }
        if (supabasePublishableKey.startsWith("sb_secret_", ignoreCase = true)) {
            add("Titkos Supabase-kulcs nem kerülhet mobilalkalmazásba.")
        }
    }

    companion object {
        fun fromBuildConfig(): BackendConfiguration = BackendConfiguration(
            environment = BuildConfig.ENVIRONMENT,
            supabaseUrl = BuildConfig.SUPABASE_URL.trim(),
            supabasePublishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY.trim(),
            publicProfileBaseUrl = BuildConfig.PUBLIC_PROFILE_BASE_URL.trimEnd('/'),
            googleAuthEnabled = BuildConfig.GOOGLE_AUTH_ENABLED,
        )
    }
}

private fun String.isValidHttpsUrl(): Boolean = runCatching { URI(this) }
    .getOrNull()
    ?.let { uri ->
        uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.userInfo == null &&
            uri.fragment == null
    } == true

private fun String.isValidBackendUrl(environment: String): Boolean {
    val uri = runCatching { URI(this) }.getOrNull() ?: return false
    val hasSafeShape = !uri.host.isNullOrBlank() && uri.userInfo == null && uri.fragment == null
    if (!hasSafeShape) return false
    if (uri.scheme.equals("https", ignoreCase = true)) return true

    val localDevelopmentHosts = setOf("localhost", "127.0.0.1", "10.0.2.2")
    return environment.equals("DEV", ignoreCase = true) &&
        uri.scheme.equals("http", ignoreCase = true) &&
        uri.host.lowercase() in localDevelopmentHosts
}
