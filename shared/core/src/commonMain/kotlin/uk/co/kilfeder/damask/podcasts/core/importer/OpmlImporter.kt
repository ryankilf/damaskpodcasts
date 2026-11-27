package uk.co.kilfeder.damask.podcasts.core.importer

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import uk.co.kilfeder.damask.podcasts.core.data.CategoryPrefill
import uk.co.kilfeder.damask.podcasts.core.data.FeedStore
import uk.co.kilfeder.damask.podcasts.core.data.FetchedFeed
import uk.co.kilfeder.damask.podcasts.core.feed.FeedFetcher
import uk.co.kilfeder.damask.podcasts.core.feed.FeedParseException
import uk.co.kilfeder.damask.podcasts.core.feed.FeedParser
import uk.co.kilfeder.damask.podcasts.core.feed.FetchResult
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.ShowId

/** One row of the confirm-or-change category list. */
data class ImportCandidate(
    val entry: OpmlEntry,
    /** The feed's own title when it could be fetched, else the OPML's. */
    val title: String,
    val imageUrl: String?,
    val suggestedCategory: CategoryId,
    /** null when the feed couldn't be fetched or read; it can still be subscribed and retried later. */
    val feed: FetchedFeed?,
    val problem: String?,
)

/** Progress from [OpmlImporter.prepare]. */
interface ImportListener {
    /** The file was parsed: these feeds will be read, in this order. */
    fun onStarted(entries: List<OpmlEntry>) = Unit

    /** The feed at [index] in the parsed entries has been read (or failed: see [ImportCandidate.problem]). */
    fun onFeedRead(index: Int, candidate: ImportCandidate) = Unit

    companion object {
        val NONE = object : ImportListener {}
    }
}

/**
 * Import: OPML file -> confirm-or-change category list (one row per show, alphabetical) ->
 * subscribe. Nothing is written until [confirm]; the user must confirm or change every category.
 */
class OpmlImporter(
    private val fetcher: FeedFetcher,
    private val store: FeedStore,
    private val categories: CategoryPrefill,
) {
    /**
     * Fetches every feed in [opml] and prefills its category, reporting progress to [listener]:
     * the parsed entries first, then each feed as it's read (in completion order, one at a time).
     */
    suspend fun prepare(
        opml: String,
        concurrency: Int = DEFAULT_CONCURRENCY,
        listener: ImportListener = ImportListener.NONE,
    ): List<ImportCandidate> {
        val entries = Opml.parse(opml)
        categories.seedAppleCategories()
        val permits = Semaphore(concurrency)
        val reporting = Mutex()
        listener.onStarted(entries)
        val candidates = coroutineScope {
            entries.mapIndexed { index, entry ->
                async {
                    val candidate = permits.withPermit { candidate(entry) }
                    reporting.withLock { listener.onFeedRead(index, candidate) }
                    candidate
                }
            }.awaitAll()
        }
        return candidates.sortedWith(compareBy<ImportCandidate> { it.title.lowercase() }.thenBy { it.entry.feedUrl })
    }

    /**
     * Subscribes each candidate to the category the user confirmed. [onProgress] is called with
     * (shows saved, total) after each one.
     */
    fun confirm(
        choices: List<Pair<ImportCandidate, CategoryId>>,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): List<ShowId> = choices.mapIndexed { index, (candidate, category) ->
        store.subscribe(candidate.entry.feedUrl, category, candidate.feed)
            .also { onProgress(index + 1, choices.size) }
    }

    private suspend fun candidate(entry: OpmlEntry): ImportCandidate {
        val (feed, problem) = fetch(entry.feedUrl)
        val document = feed?.document
        return ImportCandidate(
            entry = entry,
            title = document?.title?.takeIf { it.isNotBlank() } ?: entry.title,
            imageUrl = document?.imageUrl,
            suggestedCategory = categories.prefillCategory(document),
            feed = feed,
            problem = problem,
        )
    }

    private suspend fun fetch(url: String): Pair<FetchedFeed?, String?> =
        when (val result = fetcher.fetch(url)) {
            is FetchResult.Fetched -> try {
                FetchedFeed(FeedParser.parse(result.body), result.etag, result.lastModified) to null
            } catch (e: FeedParseException) {
                null to e.message
            }
            FetchResult.NotModified -> null to "Not modified"
            is FetchResult.Unavailable -> null to "Feed unavailable (HTTP ${result.status})"
            is FetchResult.Failed -> null to result.reason
        }

    private companion object {
        const val DEFAULT_CONCURRENCY = 8
    }
}
