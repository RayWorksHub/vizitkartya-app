package hu.rayworks.vizit.data.remote

import hu.rayworks.vizit.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI

/** Profile/account traffic goes only to the configured Node API, never PostgREST. */
class NodeBackendApi(private val client: SupabaseClient?) {
    private val json = Json { ignoreUnknownKeys = true }
    fun userId(): String? = client?.auth?.currentSessionOrNull()?.user?.id

    suspend fun request(method: String, path: String, body: JsonObject? = null,
                        authenticated: Boolean = true): JsonObject {
        require(method in setOf("GET", "POST", "PUT", "DELETE"))
        require(path in setOf("/api/profile", "/api/account", "/api/auth/sign-in"))
        val owner = if (authenticated) userId() ?: throw NodeBackendException(401, "session_unavailable", "Jelentkezz be újra.") else null
        suspend fun send(): Pair<Int, JsonObject> {
            val token = if (authenticated) {
                check(userId() == owner) { "A munkamenet megváltozott." }
                client?.auth?.currentSessionOrNull()?.accessToken?.takeIf(String::isNotBlank)
                    ?: throw NodeBackendException(401, "session_unavailable", "Jelentkezz be újra.")
            } else null
            return withContext(Dispatchers.IO) {
                val base = URI(BuildConfig.NODE_BACKEND_BASE_URL)
                require(base.scheme == "https" && !base.host.isNullOrBlank() && base.userInfo == null &&
                    base.query == null && base.fragment == null && base.path in listOf("", "/"))
                val connection = base.resolve(path).toURL().openConnection() as HttpURLConnection
                try {
                    connection.requestMethod = method
                    connection.instanceFollowRedirects = false
                    connection.connectTimeout = 15000
                    connection.readTimeout = 20000
                    connection.useCaches = false
                    connection.setRequestProperty("Accept", "application/json")
                    connection.setRequestProperty("Cache-Control", "no-store")
                    token?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
                    body?.let {
                        val bytes = it.toString().toByteArray(Charsets.UTF_8)
                        require(bytes.size <= 768 * 1024) { "A profil túl nagy." }
                        connection.doOutput = true
                        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        connection.setFixedLengthStreamingMode(bytes.size)
                        connection.outputStream.use { stream -> stream.write(bytes) }
                    }
                    val status = connection.responseCode
                    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                    val text = stream?.use { input ->
                        val out = ByteArrayOutputStream()
                        val buffer = ByteArray(8192)
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            require(out.size() + count <= 2 * 1024 * 1024) { "A szerver válasza túl nagy." }
                            out.write(buffer, 0, count)
                        }
                        out.toString(Charsets.UTF_8.name())
                    }.orEmpty()
                    val data = if (text.isBlank()) buildJsonObject {} else
                        runCatching { json.parseToJsonElement(text).jsonObject }.getOrElse {
                            throw NodeBackendException(status, "invalid_response", "A szerver válasza nem feldolgozható.")
                        }
                    status to data
                } finally { connection.disconnect() }
            }
        }
        var result = send()
        if (authenticated && result.first == 401 && userId() == owner) {
            requireNotNull(client).auth.refreshCurrentSession()
            result = send()
        }
        if (authenticated && userId() != owner) throw NodeBackendException(401, "session_changed", "A munkamenet megváltozott.")
        if (result.first !in 200..299) throw NodeBackendException(result.first,
            result.second["code"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            result.second["error"]?.jsonPrimitive?.contentOrNull?.take(500) ?: "A szerver jelenleg nem érhető el.",
            result.second["conflict"]?.jsonPrimitive?.booleanOrNull == true)
        return result.second
    }
}

class NodeBackendException(val status: Int, val code: String, message: String,
                           val conflict: Boolean = false) : IllegalStateException(message)
