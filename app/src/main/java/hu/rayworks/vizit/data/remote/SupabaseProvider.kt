package hu.rayworks.vizit.data.remote

import hu.rayworks.vizit.BuildConfig
import hu.rayworks.vizit.config.SupabaseEnvironmentConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseProvider {
    @Volatile private var client: SupabaseClient? = null

    fun getOrNull(): SupabaseClient? {
        val config = SupabaseEnvironmentConfig.current
        if (!config.enabled || config.url.isBlank() || config.publishableKey.isBlank()) return null
        return client ?: synchronized(this) {
            client ?: createSupabaseClient(config.url, config.publishableKey) {
                install(Auth) {
                    scheme = BuildConfig.AUTH_SCHEME
                    host = "auth-callback"
                    flowType = FlowType.PKCE
                }
                install(Postgrest)
                install(Storage)
                install(Functions)
            }.also { client = it }
        }
    }
}
