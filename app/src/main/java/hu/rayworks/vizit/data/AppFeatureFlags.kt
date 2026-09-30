package hu.rayworks.vizit.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

data class AppFeatureFlags(
    val businessPortal: Boolean = true,
    val analytics: Boolean = true,
    val crm: Boolean = true,
    val qrScanner: Boolean = true,
    val onlineEditor: Boolean = true,
    val multiProfile: Boolean = false,
) {
    companion object {
        fun from(rows: List<RemoteFeatureFlag>): AppFeatureFlags {
            val values = rows.associate { it.key to it.enabled }
            return AppFeatureFlags(
                businessPortal = values["business_portal"] ?: true,
                analytics = values["analytics"] ?: true,
                crm = values["crm"] ?: true,
                qrScanner = values["qr_scanner"] ?: true,
                onlineEditor = values["online_editor"] ?: true,
                multiProfile = values["multi_profile"] ?: false,
            )
        }
    }
}

@Serializable
data class RemoteFeatureFlag(
    val key: String,
    @SerialName("enabled") val enabled: Boolean,
)

class FeatureFlagRepository(private val client: SupabaseClient?) {
    suspend fun fetch(): AppFeatureFlags {
        val actualClient = client ?: return AppFeatureFlags()
        return runCatching {
            AppFeatureFlags.from(
                actualClient.from("app_feature_flags")
                    .select()
                    .decodeList<RemoteFeatureFlag>(),
            )
        }.getOrDefault(AppFeatureFlags())
    }
}
