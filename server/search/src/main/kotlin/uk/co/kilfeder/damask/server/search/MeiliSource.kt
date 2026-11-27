package uk.co.kilfeder.damask.server.search

import kotlinx.coroutines.CancellationException
import uk.co.kilfeder.damask.podcasts.api.SearchResponse
import uk.co.kilfeder.damask.podcasts.api.ShowHit
import uk.co.kilfeder.damask.server.meili.MeiliClient
import uk.co.kilfeder.damask.server.meili.MeiliQuery
import uk.co.kilfeder.damask.server.meili.PodcastDocument
import uk.co.kilfeder.damask.server.meili.PodcastIndex

/** The default source: our Meilisearch index, queried with the search-only key. */
class MeiliSource(private val meili: MeiliClient) : SearchSource {

    override suspend fun search(request: SearchRequest): SearchResponse {
        val filters = buildList {
            // The prefix is validated to [a-z]{2,3} before it gets here, so quoting is safe.
            request.langPrefix?.let { add("languagePrefix = \"$it\"") }
            if (request.hideExplicit) add("explicit = false")
        }
        val result = try {
            meili.search(PodcastIndex.LIVE, MeiliQuery(request.query, request.limit, request.offset, filters))
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw SourceUnavailableException("Meilisearch search failed (${e::class.simpleName})", e)
        }
        return SearchResponse(hits = result.hits.map { it.toHit() }, estimatedTotal = result.estimatedTotalHits)
    }

    /**
     * Meilisearch is up and the live index exists. Checked with an empty search, because the
     * gateway's search-only key may not read index metadata.
     */
    override suspend fun isHealthy(): Boolean = runCatching {
        meili.search(PodcastIndex.LIVE, MeiliQuery(text = "", limit = 0, offset = 0))
    }.isSuccess

    private fun PodcastDocument.toHit() = ShowHit(
        podcastIndexId = id,
        podcastGuid = podcastGuid,
        title = title,
        author = author,
        imageUrl = imageUrl,
        feedUrl = feedUrl,
        language = language,
        categories = categories,
        itunesType = itunesType,
        explicit = explicit,
        episodeCount = episodeCount,
        newestItemPubdate = newestItemPubdate,
    )
}
