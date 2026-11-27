package uk.co.kilfeder.damask.podcasts.api

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Golden-JSON contract tests for `/v1`. If one of these fails, the change is breaking: add a new
 * field with a default instead, or start `/v2`. Never "fix" a golden string to match new output.
 */
class ContractTest {

    private fun json(text: String): JsonElement = Json.parseToJsonElement(text)

    private val hit = ShowHit(
        podcastIndexId = 920_666,
        podcastGuid = "917393e3-1b1e-5cef-ace4-edaa54e1f810",
        title = "Podcasting 2.0",
        author = "Podcast Index LLC",
        imageUrl = "https://example.com/art.jpg",
        feedUrl = "https://example.com/feed.xml",
        language = "en-us",
        categories = listOf("Technology", "News"),
        itunesType = "episodic",
        explicit = false,
        episodeCount = 250,
        newestItemPubdate = 1_795_780_800,
    )

    private val hitGolden = """
        {
          "podcastIndexId": 920666,
          "podcastGuid": "917393e3-1b1e-5cef-ace4-edaa54e1f810",
          "title": "Podcasting 2.0",
          "author": "Podcast Index LLC",
          "imageUrl": "https://example.com/art.jpg",
          "feedUrl": "https://example.com/feed.xml",
          "language": "en-us",
          "categories": ["Technology", "News"],
          "itunesType": "episodic",
          "explicit": false,
          "episodeCount": 250,
          "newestItemPubdate": 1795780800
        }
    """

    @Test
    fun showHitMatchesGolden() {
        assertEquals(json(hitGolden), json(ApiJson.encodeToString(ShowHit.serializer(), hit)))
        assertEquals(hit, ApiJson.decodeFromString(ShowHit.serializer(), hitGolden))
    }

    @Test
    fun searchResponseMatchesGolden() {
        val golden = """{ "hits": [$hitGolden], "estimatedTotal": 1 }"""
        val response = SearchResponse(hits = listOf(hit), estimatedTotal = 1)
        assertEquals(json(golden), json(ApiJson.encodeToString(SearchResponse.serializer(), response)))
        assertEquals(response, ApiJson.decodeFromString(SearchResponse.serializer(), golden))
    }

    @Test
    fun nullsAreExplicitOnTheWire() {
        val sparse = hit.copy(podcastGuid = null, imageUrl = null, newestItemPubdate = null)
        val encoded = json(ApiJson.encodeToString(ShowHit.serializer(), sparse)).toString()
        listOf("\"podcastGuid\":null", "\"imageUrl\":null", "\"newestItemPubdate\":null").forEach {
            kotlin.test.assertTrue(it in encoded, "expected $it in $encoded")
        }
    }

    @Test
    fun clientsIgnoreFieldsAddedLater() {
        val future = """{ "hits": [], "estimatedTotal": 0, "facets": { "language": {} }, "took": 3 }"""
        assertEquals(SearchResponse(), ApiJson.decodeFromString(SearchResponse.serializer(), future))
    }

    @Test
    fun minimalHitDecodesWithDefaults() {
        // Only the fields without defaults: anything else may be absent and must not break decoding.
        val minimal = """{ "podcastIndexId": 1, "title": "T", "feedUrl": "https://e.com/f" }"""
        val decoded = ApiJson.decodeFromString(ShowHit.serializer(), minimal)
        assertEquals(ShowHit(podcastIndexId = 1, title = "T", feedUrl = "https://e.com/f"), decoded)
    }

    @Test
    fun errorAndHealthMatchGolden() {
        val error = ApiError(error = ApiErrorCode.RATE_LIMITED, message = "Too many requests")
        assertEquals(
            json("""{ "error": "rate_limited", "message": "Too many requests" }"""),
            json(ApiJson.encodeToString(ApiError.serializer(), error)),
        )
        assertEquals(
            json("""{ "status": "ok", "meilisearch": true }"""),
            json(ApiJson.encodeToString(HealthResponse.serializer(), HealthResponse("ok", meilisearch = true))),
        )
    }

    @Test
    fun pathsAndParametersAreStable() {
        assertEquals("/v1/search", SearchApi.SEARCH_PATH)
        assertEquals("/health", SearchApi.HEALTH_PATH)
        assertEquals(
            listOf("q", "lang", "hideExplicit", "limit", "offset"),
            listOf(
                SearchApi.PARAM_QUERY,
                SearchApi.PARAM_LANG,
                SearchApi.PARAM_HIDE_EXPLICIT,
                SearchApi.PARAM_LIMIT,
                SearchApi.PARAM_OFFSET,
            ),
        )
        assertEquals(
            listOf("bad_request", "unauthorized", "rate_limited", "unavailable", "not_found", "internal"),
            listOf(
                ApiErrorCode.BAD_REQUEST,
                ApiErrorCode.UNAUTHORIZED,
                ApiErrorCode.RATE_LIMITED,
                ApiErrorCode.UNAVAILABLE,
                ApiErrorCode.NOT_FOUND,
                ApiErrorCode.INTERNAL,
            ),
        )
    }
}
