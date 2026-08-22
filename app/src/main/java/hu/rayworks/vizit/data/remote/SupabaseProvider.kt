package hu.rayworks.vizit.data.remote

import hu.rayworks.vizit.config.SupabaseEnvironmentConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseProvider {
    @Volatile
    private var client: SupabaseClient? = null

    fun getOrNull(): SupabaseClient? {
        val config = SupabaseEnvironmentConfig.current
        if (!config.enabled || config.url.isBlank() || config.publishableKey.isBlank()) return null

        return client ?: synchronized(this) {
            client ?: createSupabaseClient(
                supabaseUrl = config.url,
                supabaseKey = config.publishableKey,
            ) {
                install(Auth)
                install(Postgrest)
                install(Storage)
            }.also { client = it }
        }
    }
}
