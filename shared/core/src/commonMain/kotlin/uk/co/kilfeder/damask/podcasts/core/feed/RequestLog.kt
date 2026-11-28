package uk.co.kilfeder.damask.podcasts.core.feed

import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.observer.ResponseObserver
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.util.AttributeKey
import io.ktor.utils.io.discard
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.concurrent.atomics.fetchAndIncrement
import kotlin.time.Clock
import kotlin.time.TimeSource

/** Writes one line to the platform's log (Logcat on Android, the console on desktop and iOS). */
internal expect fun writeHttpLog(line: String)

/**
 * Logs every HTTP request the app makes, when [HttpClients.logRequests] is on (debug builds):
 *
 *     → #12 GET https://example.com/feed.xml at 2026-11-28T13:55:01.123Z
 *     ← #12 200 https://example.com/feed.xml 48213 B in 230 ms
 *
 * The number matches a response to its request. Each attempt is logged (retries and redirects
 * get their own number). The size is the body as received, counted from a copy of it, so it's
 * right even without a Content-Length; the time runs from sending to the last byte. A request that
 * never gets a response logs `✗` with why. URLs are logged without any user:password part, but
 * otherwise whole, private feed tokens included, which is why this is off in release builds.
 */
internal fun HttpClientConfig<*>.logRequests() {
    install(RequestStart)
    install(ResponseObserver) {
        filter { HttpClients.logRequests }
        onResponse { response -> logResponse(response) }
    }
}

private val LogId = AttributeKey<Int>("DamaskRequestId")
private val LogStart = AttributeKey<TimeSource.Monotonic.ValueTimeMark>("DamaskRequestStart")

@OptIn(ExperimentalAtomicApi::class)
private val nextId = AtomicInt(1)

@OptIn(ExperimentalAtomicApi::class)
private val RequestStart = createClientPlugin("DamaskRequestStart") {
    on(Send) { request ->
        if (!HttpClients.logRequests) return@on proceed(request)
        val id = nextId.fetchAndIncrement()
        val start = TimeSource.Monotonic.markNow()
        request.attributes.put(LogId, id)
        request.attributes.put(LogStart, start)
        val url = loggable(request.url.build())
        writeHttpLog("→ #$id ${request.method.value} $url at ${Clock.System.now()}")
        // Logged, then rethrown unchanged: retries and the caller still see the failure.
        runCatching { proceed(request) }
            .onFailure { failure ->
                val why = failure::class.simpleName ?: "error"
                writeHttpLog("✗ #$id $url failed after ${start.elapsedNow().inWholeMilliseconds} ms: $why")
            }
            .getOrThrow()
    }
}

private suspend fun logResponse(response: HttpResponse) {
    val attributes = response.call.attributes
    val id = attributes.getOrNull(LogId) ?: return
    val bytes = response.bodyAsChannel().discard()
    val ms = attributes.getOrNull(LogStart)?.elapsedNow()?.inWholeMilliseconds
    val url = loggable(response.call.request.url)
    writeHttpLog("← #$id ${response.status.value} $url $bytes B in $ms ms")
}

/** The URL without a user:password part. */
private fun loggable(url: Url): String = URLBuilder(url).apply {
    user = null
    password = null
}.buildString()
