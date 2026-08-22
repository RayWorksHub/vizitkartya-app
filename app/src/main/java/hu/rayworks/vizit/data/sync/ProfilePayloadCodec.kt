package hu.rayworks.vizit.data.sync

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object ProfilePayloadCodec {
    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(payload: ProfileSyncPayload): String = json.encodeToString(payload)

    fun decode(raw: String): ProfileSyncPayload = json.decodeFromString(raw)

    fun encodeFieldOrder(value: List<String>): String = json.encodeToString(value)

    fun decodeFieldOrder(raw: String): List<String> = json.decodeFromString(raw)

    fun encodeFieldVisibility(value: Map<String, Boolean>): String = json.encodeToString(value)

    fun decodeFieldVisibility(raw: String): Map<String, Boolean> = json.decodeFromString(raw)
}
