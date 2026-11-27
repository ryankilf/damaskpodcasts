package uk.co.kilfeder.damask.server.search

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class PodcastIndexSourceTest {
    private val requests = mutableListOf<io.ktor.client.request.HttpRequestData>()

    private fun source(status: HttpStatusCode = HttpStatusCode.OK) = PodcastIndexSource(
        http = HttpClient(
            MockEngine { request ->
                requests += request
                respond(BODY, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ) { install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) } },
        apiKey = "KEY",
        apiSecret = "SECRET",
        userAgent = "Damask/1.2.3",
        nowEpochSeconds = { 1_795_780_800 },
    )

    @Test
    fun signsRequestsAndSendsAnHonestUserAgent(): Unit = runBlocking {
        source().search(SearchRequest("history", null, hideExplicit = true, limit = 20, offset = 0))
        val request = requests.single()
        assertEquals("Damask/1.2.3", request.headers[HttpHeaders.UserAgent])
        assertEquals("KEY", request.headers["X-Auth-Key"])
        assertEquals("1795780800", request.headers["X-Auth-Date"])
        val expected = MessageDigest.getInstance("SHA-1").digest("KEYSECRET1795780800".toByteArray())
            .joinToString("") { "%02x".format(it) }
        assertEquals(expected, request.headers[HttpHeaders.Authorization])
        assertTrue("clean" in request.url.parameters.names())
        assertEquals("api.podcastindex.org", request.url.host)
    }

    @Test
    fun mapsFiltersAndPages(): Unit = runBlocking {
        val response = source().search(SearchRequest("history", "en", hideExplicit = true, limit = 1, offset = 1))
        // Of the three feeds, one is French and one explicit; the remaining English clean ones are 1 and 4.
        assertEquals(listOf(4L), response.hits.map { it.podcastIndexId })
        assertEquals(2, response.estimatedTotal)
        val hit = source().search(SearchRequest("history", null, false, 20, 0)).hits.first()
        assertEquals(listOf("History", "Society & Culture"), hit.categories, "ordered by category ID")
        assertEquals(null, hit.podcastGuid, "blank becomes null")
    }

    @Test
    fun upstreamErrorsAreUnavailable() {
        assertFailsWith<SourceUnavailableException> {
            runBlocking { source(HttpStatusCode.Unauthorized).search(SearchRequest("x", null, false, 20, 0)) }
        }
    }

    private companion object {
        val BODY = """
            {"status":"true","feeds":[
              {"id":1,"podcastGuid":"","title":"English History","author":"A","image":"https://i/1.jpg","url":"https://f/1",
               "language":"en","categories":{"102":"Society & Culture","77":"History"},"itunesType":"episodic",
               "explicit":false,"episodeCount":10,"newestItemPubdate":1795000000,"unexpected":"ignored"},
              {"id":2,"title":"Histoire","url":"https://f/2","language":"fr","explicit":false},
              {"id":3,"title":"Explicit History","url":"https://f/3","language":"en-GB","explicit":true},
              {"id":4,"title":"More History","url":"https://f/4","language":"en-US","explicit":false}
            ],"count":4}
        """.trimIndent()
    }
}
