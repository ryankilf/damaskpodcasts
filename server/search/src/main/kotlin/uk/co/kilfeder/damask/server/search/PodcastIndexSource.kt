package uk.co.kilfeder.damask.server.search

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import uk.co.kilfeder.damask.podcasts.api.SearchResponse
import uk.co.kilfeder.damask.podcasts.api.ShowHit
import java.security.MessageDigest

/**
 * Private-build passthrough (spec › Search): the Podcast Index API, called from the server with
 * the key and secret held only in its environment. Enabled only by `PI_PASSTHROUGH=true`, for the
 * private build before the first import exists. Never for public users unless Podcast Index agree
 * in writing.
 */
class PodcastIndexSource(
    private val http: HttpClient,
    private val apiKey: String,
    private val apiSecret: String,
    private val userAgent: String,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / MS_PER_SECOND },
) : SearchSource {

    override suspend fun search(request: SearchRequest): SearchResponse {
        val response = request(request)
        if (!response.status.isSuccess()) {
            throw SourceUnavailableException("Podcast Index answered HTTP ${response.status.value}")
        }
        val feeds = response.body<PiSearchResult>().feeds
            .filter { request.langPrefix == null || it.language.lowercase().startsWith(request.langPrefix) }
            .filter { !request.hideExplicit || !it.explicit }
        return SearchResponse(
            hits = feeds.drop(request.offset).take(request.limit).map { it.toHit() },
            estimatedTotal = feeds.size,
        )
    }

    override suspend fun isHealthy(): Boolean = true

    /** The signed `search/byterm` call. The API has no offset, so it asks for offset + limit. */
    private suspend fun request(request: SearchRequest): HttpResponse = try {
        val now = nowEpochSeconds().toString()
        http.get("$BASE/search/byterm") {
            header(HttpHeaders.UserAgent, userAgent)
            header("X-Auth-Key", apiKey)
            header("X-Auth-Date", now)
            header(HttpHeaders.Authorization, sha1(apiKey + apiSecret + now))
            parameter("q", request.query)
            parameter("max", request.offset + request.limit)
            if (request.hideExplicit) parameter("clean", "")
        }
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        throw SourceUnavailableException("Podcast Index request failed (${e::class.simpleName})", e)
    }

    private fun sha1(text: String): String =
        MessageDigest.getInstance("SHA-1").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    @Serializable
    internal data class PiSearchResult(val feeds: List<PiFeed> = emptyList())

    @Serializable
    internal data class PiFeed(
        val id: Long,
        val podcastGuid: String? = null,
        val title: String = "",
        val author: String = "",
        val image: String? = null,
        val url: String = "",
        val language: String = "",
        val categories: Map<String, String>? = null,
        val itunesType: String? = null,
        val explicit: Boolean = false,
        val episodeCount: Int = 0,
        val newestItemPubdate: Long? = null,
    ) {
        fun toHit() = ShowHit(
            podcastIndexId = id,
            podcastGuid = podcastGuid?.ifBlank { null },
            title = title,
            author = author,
            imageUrl = image?.ifBlank { null },
            feedUrl = url,
            language = language,
            // The API keys categories by ID; keep them in ID order, matching category1..10 roughly.
            categories = categories.orEmpty().entries
                .sortedBy { it.key.toIntOrNull() ?: Int.MAX_VALUE }
                .map { it.value },
            itunesType = itunesType.orEmpty(),
            explicit = explicit,
            episodeCount = episodeCount,
            newestItemPubdate = newestItemPubdate,
        )
    }

    private companion object {
        const val BASE = "https://api.podcastindex.org/api/1.0"
        const val MS_PER_SECOND = 1_000L
    }
}
