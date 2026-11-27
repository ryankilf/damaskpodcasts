package uk.co.kilfeder.damask.podcasts.core.feed

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/** A scripted web of feeds for tests. Unknown URLs return 404. Records every request. */
class MockFeeds {
    val requests = mutableListOf<HttpRequestData>()
    private val routes = mutableMapOf<String, MockRequestHandleScope.(HttpRequestData) -> HttpResponseData>()

    fun serve(url: String, handler: MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
        routes[url] = handler
    }

    fun feed(url: String, body: String, etag: String? = null) = serve(url) {
        val headers = listOfNotNull(
            HttpHeaders.ContentType to listOf("application/rss+xml; charset=utf-8"),
            etag?.let { HttpHeaders.ETag to listOf(it) },
        )
        respond(body, HttpStatusCode.OK, headersOf(*headers.toTypedArray()))
    }

    fun redirect(from: String, to: String, status: HttpStatusCode) = serve(from) {
        respond("", status, headersOf(HttpHeaders.Location, to))
    }

    fun status(url: String, status: HttpStatusCode) = serve(url) { respond("", status) }

    val client = HttpClient(
        MockEngine { request ->
            requests += request
            val handler = routes[request.url.toString()] ?: return@MockEngine respond("", HttpStatusCode.NotFound)
            handler(request)
        },
    ) {
        followRedirects = false
        expectSuccess = false
    }

    val fetcher = FeedFetcher(client)
}
