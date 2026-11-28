package uk.co.kilfeder.damask.server.search

import com.auth0.jwk.Jwk
import com.auth0.jwk.JwkProvider
import com.auth0.jwk.SigningKeyNotFoundException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration

/**
 * Supabase's signing keys (spec › Search › Auth), fetched from
 * `$SUPABASE_URL/auth/v1/.well-known/jwks.json` and cached. The cache is refreshed after
 * [maxAgeMs] (an hour) or when a token names a key ID we don't have, but never more often than
 * once per [minRefreshIntervalMs], so tokens with made-up key IDs can't hammer Supabase.
 * Verification itself is local: Supabase isn't called per request.
 */
class JwksKeyProvider(
    private val fetchJwks: () -> String,
    private val maxAgeMs: Long = DEFAULT_MAX_AGE_MS,
    private val minRefreshIntervalMs: Long = DEFAULT_MIN_REFRESH_MS,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : JwkProvider {
    private val log = LoggerFactory.getLogger(JwksKeyProvider::class.java)

    @Volatile private var keys: Map<String, Jwk> = emptyMap()
    @Volatile private var fetchedAt = Long.MIN_VALUE
    @Volatile private var lastAttempt = Long.MIN_VALUE

    /** How many times the key set has been fetched (for tests and metrics). */
    @Volatile var fetchCount = 0
        private set

    override fun get(keyId: String?): Jwk {
        val id = keyId ?: throw SigningKeyNotFoundException("Token has no kid", null)
        val stale = nowMs() - fetchedAt > maxAgeMs
        if (stale || id !in keys) refresh()
        return keys[id] ?: throw SigningKeyNotFoundException("Unknown kid", null)
    }

    @Synchronized
    private fun refresh() {
        val now = nowMs()
        if (lastAttempt != Long.MIN_VALUE && now - lastAttempt < minRefreshIntervalMs) return
        lastAttempt = now
        runCatching { parse(fetchJwks()) }
            .onSuccess {
                keys = it
                fetchedAt = now
                fetchCount++
            }
            .onFailure { log.warn("JWKS refresh failed: {}", it::class.simpleName) }
    }

    companion object {
        const val DEFAULT_MAX_AGE_MS = 60 * 60 * 1000L
        const val DEFAULT_MIN_REFRESH_MS = 30 * 1000L
        private const val FETCH_TIMEOUT_S = 5L

        fun jwksUrl(supabaseUrl: String) = supabaseUrl.trimEnd('/') + "/auth/v1/.well-known/jwks.json"

        /** Fetches over HTTPS with a short timeout. */
        fun httpFetcher(url: String): () -> String {
            val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(FETCH_TIMEOUT_S)).build()
            return {
                val request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(FETCH_TIMEOUT_S))
                    .GET()
                    .build()
                val response = client.send(request, HttpResponse.BodyHandlers.ofString())
                check(response.statusCode() in HTTP_OK_RANGE) { "JWKS HTTP ${response.statusCode()}" }
                response.body()
            }
        }

        private val HTTP_OK_RANGE = 200..299

        internal fun parse(jwks: String): Map<String, Jwk> =
            Json.parseToJsonElement(jwks).jsonObject["keys"]!!.jsonArray
                .map { Jwk.fromValues(it.jsonObject.toPlainMap()) }
                .filter { it.id != null }
                .associateBy { it.id }

        private fun JsonObject.toPlainMap(): Map<String, Any?> = mapValues { (_, v) -> v.toPlain() }

        private fun JsonElement.toPlain(): Any? = when (this) {
            is JsonObject -> toPlainMap()
            is JsonArray -> map { it.toPlain() }
            is JsonPrimitive -> if (isString) content else booleanOrNull ?: longOrNull ?: content
        }
    }
}

/** The private build's caller: it has no Supabase account, so it presents a shared token. */
data class PrivateCaller(val name: String = KEY) {
    companion object {
        const val KEY = "private"
    }
}

/**
 * Compares the presented token with the configured one in constant time: both are hashed first,
 * so neither content nor length leaks through timing.
 */
fun constantTimeEquals(presented: String, expected: String): Boolean {
    val digest = { s: String -> MessageDigest.getInstance("SHA-256").digest(s.toByteArray()) }
    return MessageDigest.isEqual(digest(presented), digest(expected))
}
