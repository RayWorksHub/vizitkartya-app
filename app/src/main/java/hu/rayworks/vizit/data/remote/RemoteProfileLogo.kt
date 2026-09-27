package hu.rayworks.vizit.data.remote

import hu.rayworks.vizit.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.json.*
import java.util.Base64
import java.util.UUID

/** A webkártya logója az appearance mezőben, a saját Storage-mappában él. */
internal object RemoteProfileLogo {
    private val presets = mapOf(
        "midnight" to ("#0c2c63" to "#05163a"),
        "ivory" to ("#1668f0" to "#0742a8"),
        "forest" to ("#0e6b4a" to "#063526"),
        "plum" to ("#5b2e9e" to "#2c1550"),
    )

    fun url(appearance: JsonObject?): String? = appearance?.get("logo_url")?.jsonPrimitive?.contentOrNull

    suspend fun prepare(
        client: SupabaseClient, owner: String, appearance: JsonObject?,
        remoteLogoBase64: String, requested: String?, theme: String,
    ): JsonObject? {
        if (requested == null) return appearance // Old offline mutations keep the existing logo.
        val existingUrl = url(appearance)
        val current = if (remoteLogoBase64.isEmpty()) "" else LegacyProfileCodec.INLINE_PREFIX + remoteLogoBase64
        if (requested == current || (requested.isEmpty() && existingUrl == null)) return appearance

        val nextUrl = if (requested.isEmpty()) null else {
            require(requested.startsWith(LegacyProfileCodec.INLINE_PREFIX)) { "Érvénytelen logó." }
            val encoded = requested.removePrefix(LegacyProfileCodec.INLINE_PREFIX)
            val bytes = Base64.getDecoder().decode(encoded)
            require(bytes.size in 3..262144 && bytes[0] == 0xff.toByte() &&
                bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte() &&
                Base64.getEncoder().encodeToString(bytes) == encoded) { "Érvénytelen logó." }
            val path = "$owner/logo-${UUID.randomUUID()}.jpg"
            client.storage.from("avatars").upload(path, bytes) { upsert = false }
            client.storage.from("avatars").publicUrl(path).also { publicUrl ->
                val expected = BuildConfig.SUPABASE_URL.trimEnd('/') + "/storage/v1/object/public/avatars/$path"
                require(publicUrl == expected) { "A logó címe nem a saját tárhelyre mutat." }
            }
        }
        val preset = presets[theme] ?: presets.getValue("midnight")
        return buildJsonObject {
            if (appearance == null) {
                put("version", 1); put("mode", "preset"); put("start", preset.first)
                put("end", preset.second); put("angle", 145); put("text", "auto")
                put("logo_scale", 88); put("logo_surface", "white"); put("media_layout", "overlay")
            } else for ((key, value) in appearance) if (key != "logo_url") put(key, value)
            put("logo_url", nextUrl)
        }
    }
}
