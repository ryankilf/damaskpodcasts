package uk.co.kilfeder.damask.podcasts.core.feed

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.URLProtocol
import kotlinx.coroutines.CancellationException

/**
 * Sends `http://` requests over HTTPS first, like an interceptor: stored URLs are never rewritten.
 * If the HTTPS attempt can't connect or returns an error status, the original `http://` request
 * is sent instead. (Android blocks plain HTTP by default, so on Android that fallback fails with
 * a clear reason; on desktop an HTTP-only feed still works.)
 */
fun HttpClient.upgradeHttpToHttps(): HttpClient = apply {
    plugin(HttpSend).intercept { request ->
        if (request.url.protocol != URLProtocol.HTTP) return@intercept execute(request)
        val call = attempt { execute(secureCopyOf(request)) }
        if (call != null && call.response.status.value < FIRST_ERROR_STATUS) call else execute(request)
    }
}

private fun secureCopyOf(request: HttpRequestBuilder) = HttpRequestBuilder().takeFrom(request).apply {
    url.protocol = URLProtocol.HTTPS
    if (url.port == URLProtocol.HTTP.defaultPort) url.port = URLProtocol.HTTPS.defaultPort
}

/** Runs [block], turning any failure except cancellation into null. */
private suspend fun <T> attempt(block: suspend () -> T): T? = try {
    block()
} catch (e: CancellationException) {
    throw e
} catch (@Suppress("TooGenericExceptionCaught", "SwallowedException") e: Exception) {
    null
}

private const val FIRST_ERROR_STATUS = 400

/**
 * A short, plain-English reason for a failed request, shown to users and logged. Never includes
 * the URL. Exception types differ by platform, so they're matched by name.
 */
internal fun describeNetworkFailure(e: Throwable): String = when (e::class.simpleName) {
    // Android refusing plain HTTP ("CLEARTEXT communication not permitted").
    "UnknownServiceException" -> "No secure connection available"
    "HttpRequestTimeoutException", "SocketTimeoutException", "ConnectTimeoutException" -> "Took too long"
    "SSLHandshakeException", "SSLException", "SSLPeerUnverifiedException" -> "Secure connection failed"
    else -> "Couldn’t connect"
}

/** A plain-English reason for an HTTP error status (never the number itself). */
internal fun describeStatus(status: Int): String = when (status) {
    HTTP_NOT_FOUND, HTTP_GONE -> "This show has gone"
    HTTP_TOO_MANY_REQUESTS -> "The show’s server is busy"
    in HTTP_SERVER_ERRORS -> "The show’s server is having problems"
    else -> "Couldn’t load it"
}

/** 401 / 403: a private feed whose subscription has lapsed, or one that needs a login. */
const val NEEDS_LOGIN = "Needs a login or subscription"

private const val HTTP_NOT_FOUND = 404
private const val HTTP_GONE = 410
private const val HTTP_TOO_MANY_REQUESTS = 429
private val HTTP_SERVER_ERRORS = 500..599
