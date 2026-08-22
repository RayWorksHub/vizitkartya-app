package hu.rayworks.vizit.config

import hu.rayworks.vizit.BuildConfig

enum class AppEnvironment {
    DEV,
    BETA,
    PROD,
    ;

    companion object {
        val current: AppEnvironment
            get() = entries.firstOrNull { it.name == BuildConfig.ENVIRONMENT } ?: DEV
    }
}

data class SupabaseEnvironmentConfig(
    val url: String,
    val publishableKey: String,
    val enabled: Boolean,
) {
    companion object {
        val current: SupabaseEnvironmentConfig
            get() = SupabaseEnvironmentConfig(
                url = BuildConfig.SUPABASE_URL,
                publishableKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY,
                enabled = BuildConfig.SUPABASE_ENABLED,
            )
    }
}
