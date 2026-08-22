package hu.rayworks.vizit.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.vizitSettingsDataStore by preferencesDataStore(name = "vizit_settings")

data class AppSettings(
    val appearance: String = "SYSTEM",
    val publicProfileEnabled: Boolean = false,
    val analyticsEnabled: Boolean = true,
    val automaticSyncEnabled: Boolean = true,
    val lastActiveProfileOwnerId: String? = null,
    val legalAcceptanceOwnerId: String? = null,
    val legalAcceptanceVersion: String? = null,
    val legacyProfileMigrated: Boolean = false,
)

class AppSettingsStore(private val context: Context) {
    val settings: Flow<AppSettings> = context.vizitSettingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences ->
            AppSettings(
                appearance = preferences[APPEARANCE] ?: "SYSTEM",
                publicProfileEnabled = preferences[PUBLIC_PROFILE_ENABLED] ?: false,
                analyticsEnabled = preferences[ANALYTICS_ENABLED] ?: true,
                automaticSyncEnabled = preferences[AUTOMATIC_SYNC_ENABLED] ?: true,
                lastActiveProfileOwnerId = preferences[LAST_ACTIVE_PROFILE_OWNER_ID],
                legalAcceptanceOwnerId = preferences[LEGAL_ACCEPTANCE_OWNER_ID],
                legalAcceptanceVersion = preferences[LEGAL_ACCEPTANCE_VERSION],
                legacyProfileMigrated = preferences[LEGACY_PROFILE_MIGRATED] ?: false,
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

    suspend fun setAutomaticSyncEnabled(enabled: Boolean) {
        context.vizitSettingsDataStore.edit { it[AUTOMATIC_SYNC_ENABLED] = enabled }
    }

    suspend fun setActiveProfileOwnerId(userId: String?) {
        context.vizitSettingsDataStore.edit { preferences ->
            if (userId.isNullOrBlank()) {
                preferences.remove(LAST_ACTIVE_PROFILE_OWNER_ID)
            } else {
                preferences[LAST_ACTIVE_PROFILE_OWNER_ID] = userId
            }
        }
    }

    suspend fun rememberLegalAcceptance(
        userId: String,
        privacyPolicyVersion: String,
        termsVersion: String,
    ) {
        context.vizitSettingsDataStore.edit { preferences ->
            preferences[LEGAL_ACCEPTANCE_OWNER_ID] = userId
            preferences[LEGAL_ACCEPTANCE_VERSION] = legalVersion(
                privacyPolicyVersion = privacyPolicyVersion,
                termsVersion = termsVersion,
            )
        }
    }

    suspend fun hasRememberedLegalAcceptance(
        userId: String,
        privacyPolicyVersion: String,
        termsVersion: String,
    ): Boolean = current().let { settings ->
        settings.legalAcceptanceOwnerId == userId &&
            settings.legalAcceptanceVersion == legalVersion(privacyPolicyVersion, termsVersion)
    }

    suspend fun clearAuthenticationCache() {
        context.vizitSettingsDataStore.edit { preferences ->
            preferences.remove(LAST_ACTIVE_PROFILE_OWNER_ID)
            preferences.remove(LEGAL_ACCEPTANCE_OWNER_ID)
            preferences.remove(LEGAL_ACCEPTANCE_VERSION)
        }
    }

    suspend fun markLegacyProfileMigrated() {
        context.vizitSettingsDataStore.edit { it[LEGACY_PROFILE_MIGRATED] = true }
    }

    suspend fun current(): AppSettings = settings.first()

    private companion object {
        val APPEARANCE = stringPreferencesKey("appearance")
        val PUBLIC_PROFILE_ENABLED = booleanPreferencesKey("public_profile_enabled")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        val AUTOMATIC_SYNC_ENABLED = booleanPreferencesKey("automatic_sync_enabled")
        val LAST_ACTIVE_PROFILE_OWNER_ID = stringPreferencesKey("last_active_profile_owner_id")
        val LEGAL_ACCEPTANCE_OWNER_ID = stringPreferencesKey("legal_acceptance_owner_id")
        val LEGAL_ACCEPTANCE_VERSION = stringPreferencesKey("legal_acceptance_version")
        val LEGACY_PROFILE_MIGRATED = booleanPreferencesKey("legacy_profile_migrated")

        fun legalVersion(privacyPolicyVersion: String, termsVersion: String): String =
            "${privacyPolicyVersion.length}:$privacyPolicyVersion${termsVersion.length}:$termsVersion"
    }
}
