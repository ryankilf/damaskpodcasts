package uk.co.kilfeder.damask.server.search

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import uk.co.kilfeder.damask.podcasts.api.SearchResponse
import uk.co.kilfeder.damask.podcasts.api.ShowHit
import uk.co.kilfeder.damask.server.meili.MeiliClient
import uk.co.kilfeder.damask.server.meili.MeiliQuery
import uk.co.kilfeder.damask.server.meili.PodcastDocument
import uk.co.kilfeder.damask.server.meili.PodcastIndex

/**
 * The default source: our Meilisearch index, queried with the search-only key.
 *
 * - `lang=en`: shows in English or with no language listed (one query).
 * - `lang=en-GB`: the same set, with the region's shows nudged up: two queries (the region; the
 *   rest of the language and blank) merged in the gateway by [RegionMerge], with the region's
 *   `rankScore` multiplied by [regionBoost]. Federated search can't do this: its merge uses
 *   `_rankingScore`, which ignores custom rules (docs/decisions.md › Server).
 * - `category`: ORed top-level categories; shows with none are excluded only when one is given.
 *
 * All filter values are validated (language tags, the 19 category keys) before they get here.
 */
class MeiliSource(
    private val meili: MeiliClient,
    private val regionBoost: Double = DEFAULT_REGION_BOOST,
) : SearchSource {

    override suspend fun search(request: SearchRequest): SearchResponse = try {
        val tag = request.langTag
        if (tag == null) plain(request) else regionBoosted(request, tag)
    } catch (e: CancellationException) {
        throw e
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        throw SourceUnavailableException("Meilisearch search failed (${e::class.simpleName})", e)
    }

    private suspend fun plain(request: SearchRequest): SearchResponse {
        val query = MeiliQuery(request.query, request.limit, request.offset, filters(request))
        val result = meili.search(PodcastIndex.LIVE, query)
        return SearchResponse(hits = result.hits.map { it.toHit() }, estimatedTotal = result.estimatedTotalHits)
    }

    /**
     * Each list is fetched from the top, `offset + limit` plus [OVERFETCH] deep, because dropping
     * cross-list duplicates can otherwise leave a page short. At most 40 + 20 + 5 = 65, within the
     * index's 100-hit cap.
     */
    private suspend fun regionBoosted(request: SearchRequest, tag: String): SearchResponse = coroutineScope {
        val shared = filters(request)
        val depth = request.offset + request.limit + OVERFETCH
        val inRegion = MeiliQuery(request.query, depth, 0, shared + "languageTag = \"$tag\"")
        val elsewhere = MeiliQuery(request.query, depth, 0, shared + "NOT languageTag = \"$tag\"")
        val region = async { meili.searchScored(PodcastIndex.LIVE, inRegion) }
        val rest = async { meili.searchScored(PodcastIndex.LIVE, elsewhere) }
        val a = region.await()
        val b = rest.await()
        val merged = RegionMerge.merge(
            a.hits,
            b.hits,
            keyA = { RegionMerge.key(it, regionBoost) },
            keyB = { RegionMerge.key(it, 1.0) },
            dedupeKey = { it.document.dedupeKey },
        )
        SearchResponse(
            hits = merged.drop(request.offset).take(request.limit).map { it.document.toHit() },
            estimatedTotal = a.estimatedTotalHits + b.estimatedTotalHits,
        )
    }

    private fun filters(request: SearchRequest): List<String> = buildList {
        request.langPrefix?.let {
            add("(languagePrefix = \"$it\" OR languagePrefix IS EMPTY OR languagePrefix NOT EXISTS)")
        }
        if (request.hideExplicit) add("explicit = false")
        if (request.categories.isNotEmpty()) {
            add("topCategories IN [${request.categories.joinToString(", ") { "\"$it\"" }}]")
        }
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
        topCategories = topCategories,
    )

    companion object {
        /** Multiplies the region's `rankScore` in the merge. Env: REGION_BOOST. 1.0 = no boost. */
        const val DEFAULT_REGION_BOOST = 1.25

        /** Extra hits fetched per list so cross-list duplicates don't leave a page short. */
        const val OVERFETCH = 5
    }
}
