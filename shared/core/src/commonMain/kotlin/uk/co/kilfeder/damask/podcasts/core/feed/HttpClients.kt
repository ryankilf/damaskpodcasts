package uk.co.kilfeder.damask.podcasts.core.feed

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import kotlinx.coroutines.CancellationException

private const val TIMEOUT_MS = 30_000L
private const val MAX_RETRIES = 2
private const val RETRY_BASE_DELAY_MS = 1_000L
private const val RETRY_MAX_DELAY_MS = 8_000L
private const val RETRY_JITTER_MS = 500L
private const val RETRY_GROWTH = 3.0
private const val TOO_MANY_REQUESTS = 429
private const val FIRST_SERVER_ERROR = 500

/** Sent everywhere: hosts count listeners by app, and Podcast Index asks for an honest one. */
const val DAMASK_USER_AGENT = "Damask/0.1"

/**
 * The app's HTTP clients. One base client (one platform engine, one connection pool) with the
 * shared rules: timeouts, a `Damask` User-Agent, no exceptions for error statuses, and retries
 * for transient failures. Each use is a thin variation on it ([HttpClient.config] shares the
 * engine):
 *
 * - [feeds] follows redirects by hand, so a permanent move can update the stored feed URL.
 * - [images] follows redirects itself; the image loader only wants the bytes.
 * - The search API client will be another variation (JSON, its token).
 *
 * Caching isn't done here: feeds use the ETag / Last-Modified stored with each show, and artwork
 * uses the image loader's disk cache.
 */
object HttpClients {
    private val base: HttpClient by lazy {
        HttpClient {
            expectSuccess = false
            install(UserAgent) { agent = DAMASK_USER_AGENT }
            install(HttpTimeout) {
                requestTimeoutMillis = TIMEOUT_MS
                connectTimeoutMillis = TIMEOUT_MS
            }
            retryTransientFailures()
        }
    }

    val feeds: HttpClient by lazy { base.config { followRedirects = false }.upgradeHttpToHttps() }

    val images: HttpClient by lazy { base.config { followRedirects = true }.upgradeHttpToHttps() }
}

/**
 * Ktor's built-in retry, for the failures that trying again can fix: server errors (5xx), "too
 * busy" (429, honouring Retry-After), and network failures such as timeouts, dropped connections
 * and DNS hiccups. Two retries, about 1 s then 3 s apart. Never retried: other 4xx (the server
 * said no) and certificate or TLS failures (they fail the same way every time).
 */
internal fun HttpClientConfig<*>.retryTransientFailures(baseDelayMs: Long = RETRY_BASE_DELAY_MS) {
    install(HttpRequestRetry) {
        retryIf(MAX_RETRIES) { _, response ->
            response.status.value == TOO_MANY_REQUESTS || response.status.value >= FIRST_SERVER_ERROR
        }
        retryOnExceptionIf(MAX_RETRIES) { _, cause -> isTransientNetworkFailure(cause) }
        exponentialDelay(
            base = RETRY_GROWTH,
            baseDelayMs = baseDelayMs,
            maxDelayMs = RETRY_MAX_DELAY_MS,
            randomizationMs = if (baseDelayMs < RETRY_JITTER_MS) 0 else RETRY_JITTER_MS,
            respectRetryAfterHeader = true,
        )
    }
}

/**
 * Worth another try? Platform exception types differ (JVM, Android, Darwin), so TLS and
 * certificate failures are recognised by name, like [describeNetworkFailure].
 */
internal fun isTransientNetworkFailure(cause: Throwable): Boolean {
    if (cause is CancellationException) return false
    val names = generateSequence(cause) { it.cause }.take(MAX_CAUSE_DEPTH).map { it::class.simpleName.orEmpty() }
    return names.none { name -> PERMANENT_FAILURES.any { it in name } }
}

private const val MAX_CAUSE_DEPTH = 8
private val PERMANENT_FAILURES = listOf("SSL", "Certificate", "CertPath", "Cleartext", "UnknownServiceException")
