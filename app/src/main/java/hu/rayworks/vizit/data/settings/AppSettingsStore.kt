package hu.rayworks.vizit.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.vizitSettingsDataStore by preferencesDataStore(name = "vizit_settings")

data class AppSettings(
    val appearance: String = "SYSTEM",
    val publicProfileEnabled: Boolean = false,
    val analyticsEnabled: Boolean = true,
)

class AppSettingsStore(private val context: Context) {
    val settings: Flow<AppSettings> = context.vizitSettingsDataStore.data.map { preferences ->
        AppSettings(
            appearance = preferences[APPEARANCE] ?: "SYSTEM",
            publicProfileEnabled = preferences[PUBLIC_PROFILE_ENABLED] ?: false,
            analyticsEnabled = preferences[ANALYTICS_ENABLED] ?: true,
        )
    }

    suspend fun setAppearance(value: String) {
        context.vizitSettingsDataStore.edit { it[APPEARANCE] = value }
    }

    suspend fun setPublicProfileEnabled(enabled: Boolean) {
        context.vizitSettingsDataStore.edit { it[PUBLIC_PROFILE_ENABLED] = enabled }
    }

    suspend fun setAnalyticsEnabled(enabled: Boolean) {
        context.vizitSettingsDataStore.edit { it[ANALYTICS_ENABLED] = enabled }
    }

    private companion object {
        val APPEARANCE = stringPreferencesKey("appearance")
        val PUBLIC_PROFILE_ENABLED = booleanPreferencesKey("public_profile_enabled")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
    }
}
