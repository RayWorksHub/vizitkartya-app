package hu.rayworks.vizit.data.card

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.vizitCardDataStore by preferencesDataStore(name = "vizit_card_presentation")

/**
 * The owner's card styling and per-field visibility, kept in their own
 * preference file so clearing the local profile never takes them with it.
 *
 * Every value falls back to the permissive default, so a file written by an
 * older build can never start hiding data the owner is sharing today.
 */
class CardPresentationStore(private val context: Context) {
    val presentation: Flow<CardPresentation> = context.vizitCardDataStore.data
        .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
        .map { preferences ->
            val defaults = CardPresentation()
            CardPresentation(
                colorway = preferences[COLORWAY]
                    ?.let { name -> CardColorway.entries.firstOrNull { it.name == name } }
                    ?: defaults.colorway,
                layout = preferences[LAYOUT]
                    ?.let { name -> CardLayout.entries.firstOrNull { it.name == name } }
                    ?: defaults.layout,
                showsPhoto = preferences[SHOWS_PHOTO] ?: defaults.showsPhoto,
                showsQr = preferences[SHOWS_QR] ?: defaults.showsQr,
                showsSocial = preferences[SHOWS_SOCIAL] ?: defaults.showsSocial,
                sharesCompany = preferences[SHARES_COMPANY] ?: defaults.sharesCompany,
                sharesEmail = preferences[SHARES_EMAIL] ?: defaults.sharesEmail,
                sharesPhone = preferences[SHARES_PHONE] ?: defaults.sharesPhone,
                sharesWebsite = preferences[SHARES_WEBSITE] ?: defaults.sharesWebsite,
                sharesSocial = preferences[SHARES_SOCIAL] ?: defaults.sharesSocial,
                sharesAddress = preferences[SHARES_ADDRESS] ?: defaults.sharesAddress,
            )
        }

    suspend fun save(value: CardPresentation) {
        context.vizitCardDataStore.edit { preferences ->
            preferences[COLORWAY] = value.colorway.name
            preferences[LAYOUT] = value.layout.name
            preferences[SHOWS_PHOTO] = value.showsPhoto
            preferences[SHOWS_QR] = value.showsQr
            preferences[SHOWS_SOCIAL] = value.showsSocial
            preferences[SHARES_COMPANY] = value.sharesCompany
            preferences[SHARES_EMAIL] = value.sharesEmail
            preferences[SHARES_PHONE] = value.sharesPhone
            preferences[SHARES_WEBSITE] = value.sharesWebsite
            preferences[SHARES_SOCIAL] = value.sharesSocial
            preferences[SHARES_ADDRESS] = value.sharesAddress
        }
    }

    private companion object {
        val COLORWAY = stringPreferencesKey("card_colorway")
        val LAYOUT = stringPreferencesKey("card_layout")
        val SHOWS_PHOTO = booleanPreferencesKey("card_shows_photo")
        val SHOWS_QR = booleanPreferencesKey("card_shows_qr")
        val SHOWS_SOCIAL = booleanPreferencesKey("card_shows_social")
        val SHARES_COMPANY = booleanPreferencesKey("shares_company")
        val SHARES_EMAIL = booleanPreferencesKey("shares_email")
        val SHARES_PHONE = booleanPreferencesKey("shares_phone")
        val SHARES_WEBSITE = booleanPreferencesKey("shares_website")
        val SHARES_SOCIAL = booleanPreferencesKey("shares_social")
        val SHARES_ADDRESS = booleanPreferencesKey("shares_address")
    }
}
