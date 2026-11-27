package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.db.Feed_fetch_state
import uk.co.kilfeder.damask.podcasts.core.feed.FeedDocument
import uk.co.kilfeder.damask.podcasts.core.model.Category
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.model.ShowType
import uk.co.kilfeder.damask.podcasts.core.retention.Retention
import uk.co.kilfeder.damask.podcasts.core.rules.ListeningRules
import uk.co.kilfeder.damask.podcasts.core.sync.FeedUrl
import uk.co.kilfeder.damask.podcasts.core.sync.ShowIds
import uk.co.kilfeder.damask.podcasts.core.db.Show as ShowRow

/** A successfully fetched and parsed feed, with the cache headers to send next time. */
data class FetchedFeed(
    val document: FeedDocument,
    val etag: String? = null,
    val lastModified: String? = null,
)

enum class FeedStatus { OK, UNAVAILABLE, FAILED }

/**
 * Subscriptions and the local feed cache. Synced writes (the `show` row) get HLCs from the
 * [DeviceClock]; feed data is local-only and simply replaced. Not thread-safe: use from a single
 * database dispatcher.
 */
class FeedStore(
    private val db: DamaskDatabase,
    private val clock: DeviceClock,
    rules: ListeningRules = ListeningRules(),
) {
    private val retention = Retention(rules)

    /** Subscribes (or resubscribes) to [feedUrl] in [categoryId], storing [feed] if already fetched. */
    fun subscribe(feedUrl: String, categoryId: CategoryId, feed: FetchedFeed?): ShowId {
        val id = ShowIds.fromFeedUrl(feedUrl)
        db.transaction {
            val existing = db.showQueries.selectById(id.value).executeAsOneOrNull()
            writeShow(
                (existing ?: newShowRow(id, feedUrl)).copy(
                    feed_url = feedUrl.trim(),
                    feed_url_norm = FeedUrl.normalise(feedUrl),
                    category_id = categoryId.value,
                    subscribed = 1,
                ),
            )
            feed?.let { applyFeed(id, it) }
        }
        return id
    }

    fun show(id: ShowId): ShowRow? = db.showQueries.selectById(id.value).executeAsOneOrNull()

    fun subscribedShowIds(): List<ShowId> = db.showQueries.selectSubscribedIds().executeAsList().map(::ShowId)

    fun fetchState(id: ShowId): Feed_fetch_state? = db.feedFetchStateQueries.selectByShow(id.value).executeAsOneOrNull()

    /**
     * Stores a fetched feed: channel data, categories and episodes, then applies retention. The
     * download path of existing episodes is kept.
     */
    fun applyFeed(id: ShowId, fetched: FetchedFeed) = db.transaction {
        val doc = fetched.document
        db.feedQueries.upsertShowFeed(id.value, doc.title, doc.imageUrl, doc.itunesType)
        db.feedQueries.deleteShowFeedCategories(id.value)
        doc.categories.forEachIndexed { index, category ->
            db.feedQueries.insertShowFeedCategory(id.value, index.toLong(), category.category, category.subcategory)
        }
        doc.episodes.forEach { episode ->
            db.feedQueries.upsertEpisode(
                show_id = id.value,
                guid = episode.guid,
                title = episode.title,
                // An undated item sorts as oldest rather than jumping around on every refresh.
                published_at = episode.publishedAt ?: 0,
                enclosure_url = episode.enclosureUrl,
                image_url = episode.imageUrl,
                duration_ms = episode.durationMs,
            )
        }
        prune(id, ShowType.fromItunesType(doc.itunesType))
        db.feedFetchStateQueries.recordSuccess(id.value, fetched.etag, fetched.lastModified, clock.nowMs())
    }

    fun recordNotModified(id: ShowId) {
        db.feedFetchStateQueries.recordNotModified(fetched_at = clock.nowMs(), show_id = id.value)
    }

    /** [failure] is a short reason for the UI and logs; never pass the URL. */
    fun recordProblem(id: ShowId, status: FeedStatus, failure: String?) {
        db.feedFetchStateQueries.recordProblem(id.value, status.name, clock.nowMs(), failure)
    }

    /**
     * Replace feed URL: a host issued a new URL, or the feed moved (permanent redirect or
     * `itunes:new-feed-url`). The show keeps its ID, category, settings and history.
     */
    fun moveFeed(id: ShowId, newUrl: String) = db.transaction {
        val show = db.showQueries.selectById(id.value).executeAsOneOrNull() ?: return@transaction
        if (show.feed_url == newUrl.trim()) return@transaction
        writeShow(show.copy(feed_url = newUrl.trim(), feed_url_norm = FeedUrl.normalise(newUrl)))
    }

    private fun prune(id: ShowId, type: ShowType) {
        val row = db.showQueries.selectById(id.value).executeAsOneOrNull() ?: return
        val episodes = db.libraryQueries.selectFeedEpisodesForShow(id.value, ::episodeRow).executeAsList()
        retention.episodesToPrune(row.toModel(type), episodes, clock.nowMs()).forEach { key ->
            db.feedQueries.deleteEpisode(key.showId.value, key.guid)
        }
    }

    private fun newShowRow(id: ShowId, feedUrl: String) = ShowRow(
        id = id.value,
        feed_url = feedUrl,
        feed_url_norm = FeedUrl.normalise(feedUrl),
        category_id = Category.UNCATEGORISED_ID.value,
        episode_order = null,
        keep_mode = null,
        keep_count = DEFAULT_KEEP_COUNT,
        subscribed = 1,
        hlc = 0,
    )

    private fun writeShow(row: ShowRow) = db.showQueries.upsert(
        id = row.id,
        feed_url = row.feed_url,
        feed_url_norm = row.feed_url_norm,
        category_id = row.category_id,
        episode_order = row.episode_order,
        keep_mode = row.keep_mode,
        keep_count = row.keep_count,
        subscribed = row.subscribed,
        hlc = clock.issue().value,
    )

    private companion object {
        const val DEFAULT_KEEP_COUNT = 10L
    }
}
