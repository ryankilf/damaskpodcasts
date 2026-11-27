package uk.co.kilfeder.damask.podcasts.core.feed

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.takeFrom
import kotlinx.coroutines.CancellationException

sealed interface FetchResult {
    data class Fetched(
        val body: String,
        /** Set when every redirect hop was permanent (301/308): the feed has moved for good. */
        val movedPermanentlyTo: String?,
        val etag: String?,
        val lastModified: String?,
    ) : FetchResult

    /** 304: the cached copy is current. */
    data object NotModified : FetchResult

    /** 401 / 403: a lapsed or revoked private feed. */
    data class Unavailable(val status: Int) : FetchResult

    /** Anything else. [reason] never contains the URL (private feed URLs are secrets). */
    data class Failed(val reason: String) : FetchResult
}

/**
 * Fetches feeds with conditional GETs. Redirects are followed by hand so a permanent move can
 * update the stored URL (spec › Premium feeds › Replace feed URL).
 */
class FeedFetcher(private val client: HttpClient) {

    suspend fun fetch(url: String, etag: String? = null, lastModified: String? = null): FetchResult = try {
        follow(url, etag, lastModified)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        FetchResult.Failed(e::class.simpleName ?: "Network error")
    }

    private suspend fun follow(url: String, etag: String?, lastModified: String?): FetchResult {
        var current = url
        var allPermanent = true
        repeat(MAX_REDIRECTS + 1) {
            val response = request(current, etag, lastModified)
            val location = response.headers[HttpHeaders.Location]
            if (response.status.value !in REDIRECTS || location == null) {
                return response.toResult(movedTo = current.takeIf { it != url && allPermanent })
            }
            allPermanent = allPermanent && response.status.value in PERMANENT_REDIRECTS
            current = URLBuilder(current).takeFrom(location).buildString()
        }
        return FetchResult.Failed("Too many redirects")
    }

    private suspend fun request(url: String, etag: String?, lastModified: String?): HttpResponse =
        client.get(url) {
            header(HttpHeaders.UserAgent, USER_AGENT)
            header(HttpHeaders.Accept, ACCEPT)
            etag?.let { header(HttpHeaders.IfNoneMatch, it) }
            lastModified?.let { header(HttpHeaders.IfModifiedSince, it) }
        }

    private suspend fun HttpResponse.toResult(movedTo: String?): FetchResult = when (status) {
        HttpStatusCode.OK -> FetchResult.Fetched(
            body = bodyAsText(),
            movedPermanentlyTo = movedTo,
            etag = headers[HttpHeaders.ETag],
            lastModified = headers[HttpHeaders.LastModified],
        )
        HttpStatusCode.NotModified -> FetchResult.NotModified
        HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> FetchResult.Unavailable(status.value)
        else -> FetchResult.Failed("HTTP ${status.value}")
    }

    companion object {
        const val USER_AGENT = "Damask/0.1"
        private const val ACCEPT = "application/rss+xml, application/xml;q=0.9, text/xml;q=0.8, */*;q=0.5"
        private const val MAX_REDIRECTS = 5
        private val REDIRECTS = setOf(301, 302, 303, 307, 308)
        private val PERMANENT_REDIRECTS = setOf(301, 308)
        private const val TIMEOUT_MS = 30_000L

        /** The client the app uses: platform engine, no automatic redirects, sane timeouts. */
        fun createClient(): HttpClient = HttpClient {
            followRedirects = false
            expectSuccess = false
            install(HttpTimeout) {
                requestTimeoutMillis = TIMEOUT_MS
                connectTimeoutMillis = TIMEOUT_MS
            }
        }
    }
}
