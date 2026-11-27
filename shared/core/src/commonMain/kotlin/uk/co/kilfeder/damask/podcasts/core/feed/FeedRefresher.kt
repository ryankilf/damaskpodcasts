package uk.co.kilfeder.damask.podcasts.core.feed

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import uk.co.kilfeder.damask.podcasts.core.data.FeedStatus
import uk.co.kilfeder.damask.podcasts.core.data.FeedStore
import uk.co.kilfeder.damask.podcasts.core.data.FetchedFeed
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.sync.FeedUrl

enum class RefreshOutcome { UPDATED, NOT_MODIFIED, UNAVAILABLE, FAILED, SKIPPED }

/**
 * Refreshes subscribed feeds on the device (feeds are never fetched by the server). Callers
 * schedule it (WorkManager on Android) and serialise its database writes.
 */
class FeedRefresher(
    private val fetcher: FeedFetcher,
    private val store: FeedStore,
) {
    suspend fun refreshAll(concurrency: Int = DEFAULT_CONCURRENCY): Map<ShowId, RefreshOutcome> = coroutineScope {
        val permits = Semaphore(concurrency)
        store.subscribedShowIds()
            .map { id -> async { id to permits.withPermit { refresh(id) } } }
            .awaitAll()
            .toMap()
    }

    suspend fun refresh(id: ShowId): RefreshOutcome {
        val show = store.show(id)?.takeIf { it.subscribed == 1L } ?: return RefreshOutcome.SKIPPED
        val cache = store.fetchState(id)
        return when (val result = fetcher.fetch(show.feed_url, cache?.etag, cache?.last_modified)) {
            is FetchResult.Fetched -> apply(id, show.feed_url, result)
            FetchResult.NotModified -> RefreshOutcome.NOT_MODIFIED.also { store.recordNotModified(id) }
            is FetchResult.Unavailable -> RefreshOutcome.UNAVAILABLE.also {
                // A lapsed private feed: keep everything, prompt to replace the URL.
                store.recordProblem(id, FeedStatus.UNAVAILABLE, "HTTP ${result.status}")
            }
            is FetchResult.Failed -> RefreshOutcome.FAILED.also {
                store.recordProblem(id, FeedStatus.FAILED, result.reason)
            }
        }
    }

    private fun apply(id: ShowId, currentUrl: String, result: FetchResult.Fetched): RefreshOutcome {
        val document = try {
            FeedParser.parse(result.body)
        } catch (e: FeedParseException) {
            store.recordProblem(id, FeedStatus.FAILED, e.message)
            return RefreshOutcome.FAILED
        }
        result.movedPermanentlyTo?.let { store.moveFeed(id, it) }
        val announced = document.newFeedUrl?.takeIf { FeedUrl.normalise(it) != FeedUrl.normalise(currentUrl) }
        announced?.let { store.moveFeed(id, it) }
        // After an announced move, drop the old URL's cache headers so the next fetch is unconditional.
        val fetched = when (announced) {
            null -> FetchedFeed(document, result.etag, result.lastModified)
            else -> FetchedFeed(document)
        }
        store.applyFeed(id, fetched)
        return RefreshOutcome.UPDATED
    }

    private companion object {
        const val DEFAULT_CONCURRENCY = 6
    }
}
