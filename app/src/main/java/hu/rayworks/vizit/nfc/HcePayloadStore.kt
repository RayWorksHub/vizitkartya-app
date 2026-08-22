package hu.rayworks.vizit.nfc

import android.content.Context
import android.util.Base64
import androidx.core.content.edit

class HcePayloadStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun activate(payload: ByteArray) {
        preferences.edit {
            putString(KEY_PAYLOAD, Base64.encodeToString(payload, Base64.NO_WRAP))
            putBoolean(KEY_ENABLED, true)
        }
    }

    fun deactivate() {
        preferences.edit {
            putBoolean(KEY_ENABLED, false)
            remove(KEY_PAYLOAD)
        }
    }

    fun isActive(): Boolean = preferences.getBoolean(KEY_ENABLED, false)

    fun payload(): ByteArray? = preferences.getString(KEY_PAYLOAD, null)
        ?.let { Base64.decode(it, Base64.NO_WRAP) }

    private companion object {
        const val PREFERENCES_NAME = "vizit_hce_payload"
        const val KEY_ENABLED = "enabled"
        const val KEY_PAYLOAD = "ndef_payload"
    }
}
