package hu.rayworks.vizit.nfc

import android.content.Context
import android.util.Base64
import androidx.core.content.edit

class HcePayloadStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun activate(payload: ByteArray, ttlMillis: Long = DEFAULT_TTL_MILLIS) {
        require(ttlMillis in 1..MAX_TTL_MILLIS)
        preferences.edit {
            putString(KEY_PAYLOAD, Base64.encodeToString(payload, Base64.NO_WRAP))
            putLong(KEY_EXPIRES_AT, System.currentTimeMillis() + ttlMillis)
            putBoolean(KEY_ENABLED, true)
        }
    }

    fun deactivate() {
        preferences.edit {
            putBoolean(KEY_ENABLED, false)
            remove(KEY_PAYLOAD)
            remove(KEY_EXPIRES_AT)
        }
    }

    fun isActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        if (!preferences.getBoolean(KEY_ENABLED, false)) return false
        val expiresAt = preferences.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAt <= nowMillis) {
            deactivate()
            return false
        }
        return true
    }

    fun payload(): ByteArray? {
        if (!isActive()) return null
        return preferences.getString(KEY_PAYLOAD, null)
            ?.let { runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull() }
    }

    companion object {
        const val DEFAULT_TTL_MILLIS = 60_000L
        private const val MAX_TTL_MILLIS = 5 * 60_000L
        private const val PREFERENCES_NAME = "vizit_hce_payload"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PAYLOAD = "ndef_payload"
        private const val KEY_EXPIRES_AT = "expires_at"
    }
}
