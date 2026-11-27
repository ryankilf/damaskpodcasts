package uk.co.kilfeder.damask.podcasts.core.feed

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class FeedFetcherTest {
    private val web = MockFeeds()

    @Test
    fun fetchesWithCacheHeaders() = runTest {
        web.feed("https://example.com/feed", "<rss/>", etag = "\"v1\"")
        val result = assertIs<FetchResult.Fetched>(web.fetcher.fetch("https://example.com/feed"))
        assertEquals("<rss/>", result.body)
        assertEquals("\"v1\"", result.etag)
        assertNull(result.movedPermanentlyTo)
    }

    @Test
    fun sendsConditionalHeadersAndUnderstands304() = runTest {
        web.serve("https://example.com/feed") { request ->
            if (request.headers[HttpHeaders.IfNoneMatch] == "\"v1\"") respond("", HttpStatusCode.NotModified)
            else respond("<rss/>", HttpStatusCode.OK)
        }
        assertEquals(FetchResult.NotModified, web.fetcher.fetch("https://example.com/feed", etag = "\"v1\""))
        assertEquals(FeedFetcher.USER_AGENT, web.requests.single().headers[HttpHeaders.UserAgent])
    }

    @Test
    fun permanentRedirectsReportTheNewHome() = runTest {
        web.redirect("http://old.example.com/feed", "https://old.example.com/feed", HttpStatusCode.MovedPermanently)
        web.redirect("https://old.example.com/feed", "/v2/feed", HttpStatusCode.PermanentRedirect)
        web.feed("https://old.example.com/v2/feed", "<rss/>")
        val result = assertIs<FetchResult.Fetched>(web.fetcher.fetch("http://old.example.com/feed"))
        assertEquals("https://old.example.com/v2/feed", result.movedPermanentlyTo)
    }

    @Test
    fun aTemporaryHopMeansTheFeedHasntMoved() = runTest {
        web.redirect("https://example.com/feed", "https://cdn.example.com/feed", HttpStatusCode.Found)
        web.feed("https://cdn.example.com/feed", "<rss/>")
        val result = assertIs<FetchResult.Fetched>(web.fetcher.fetch("https://example.com/feed"))
        assertNull(result.movedPermanentlyTo)
    }

    @Test
    fun authFailuresAreUnavailableAndOtherErrorsFail() = runTest {
        web.status("https://example.com/private", HttpStatusCode.Forbidden)
        web.status("https://example.com/broken", HttpStatusCode.InternalServerError)
        assertEquals(FetchResult.Unavailable(403), web.fetcher.fetch("https://example.com/private"))
        assertEquals(FetchResult.Failed("HTTP 500"), web.fetcher.fetch("https://example.com/broken"))
    }

    @Test
    fun httpFeedsAreFetchedOverHttpsWithoutCountingAsAMove() = runTest {
        val secure = MockFeeds(upgrade = true)
        secure.feed("https://example.com/feed", "<rss/>")
        val result = assertIs<FetchResult.Fetched>(secure.fetcher.fetch("http://example.com/feed"))
        assertNull(result.movedPermanentlyTo)
        assertEquals(listOf("https://example.com/feed"), secure.requests.map { it.url.toString() })
    }

    @Test
    fun httpFallsBackWhenHttpsFails() = runTest {
        val secure = MockFeeds(upgrade = true)
        secure.status("https://legacy.example.com/feed", HttpStatusCode.NotFound)
        secure.feed("http://legacy.example.com/feed", "<rss/>")
        assertIs<FetchResult.Fetched>(secure.fetcher.fetch("http://legacy.example.com/feed"))
        assertEquals(
            listOf("https://legacy.example.com/feed", "http://legacy.example.com/feed"),
            secure.requests.map { it.url.toString() },
        )
    }

    @Test
    fun failuresAreDescribedReadably() {
        assertEquals("Not available over a secure connection", describeNetworkFailure(UnknownServiceException()))
        assertEquals("Network error", describeNetworkFailure(IllegalStateException()))
    }

    private class UnknownServiceException : Exception()

    @Test
    fun redirectLoopsGiveUp() = runTest {
        web.redirect("https://a.example.com/", "https://b.example.com/", HttpStatusCode.Found)
        web.redirect("https://b.example.com/", "https://a.example.com/", HttpStatusCode.Found)
        assertEquals(FetchResult.Failed("Too many redirects"), web.fetcher.fetch("https://a.example.com/"))
    }
}
