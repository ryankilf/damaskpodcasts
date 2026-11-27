package uk.co.kilfeder.damask.podcasts.core.importer

import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import uk.co.kilfeder.damask.podcasts.core.NOW_MS
import uk.co.kilfeder.damask.podcasts.core.data.CategoryPrefill
import uk.co.kilfeder.damask.podcasts.core.data.DeviceClock
import uk.co.kilfeder.damask.podcasts.core.data.FeedStatus
import uk.co.kilfeder.damask.podcasts.core.data.FeedStore
import uk.co.kilfeder.damask.podcasts.core.data.LibraryRepository
import uk.co.kilfeder.damask.podcasts.core.data.inMemoryDriver
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.feed.AppleCategories
import uk.co.kilfeder.damask.podcasts.core.feed.FeedRefresher
import uk.co.kilfeder.damask.podcasts.core.feed.MockFeeds
import uk.co.kilfeder.damask.podcasts.core.feed.RefreshOutcome
import uk.co.kilfeder.damask.podcasts.core.feed.SampleFeeds
import uk.co.kilfeder.damask.podcasts.core.model.Category
import uk.co.kilfeder.damask.podcasts.core.sync.ShowIds
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ImportAndRefreshTest {
    private val driver = inMemoryDriver()
    private val db = DamaskDatabase(driver)
    private val store = FeedStore(db, DeviceClock(db) { NOW_MS })
    private val web = MockFeeds()
    private val importer = OpmlImporter(web.fetcher, store, CategoryPrefill(db))
    private val refresher = FeedRefresher(web.fetcher, store)

    @AfterTest
    fun close() = driver.close()

    private val opml = """
        <opml version="1.0"><body><outline text="feeds">
          <outline type="rss" text="Zeta" xmlUrl="https://zeta.example.com/feed"/>
          <outline type="rss" text="Alpha" xmlUrl="https://alpha.example.com/feed"/>
          <outline type="rss" text="Hobby" xmlUrl="https://hobby.example.com/feed"/>
          <outline type="rss" text="Gone" xmlUrl="https://gone.example.com/feed"/>
        </outline></body></opml>
    """.trimIndent()

    private fun serveAll() {
        val zeta = SampleFeeds.episodic("Zeta", "Technology", count = 12)
        web.feed("https://zeta.example.com/feed", zeta, etag = "\"z1\"")
        web.feed("https://alpha.example.com/feed", SampleFeeds.MESSY)
        web.feed("https://hobby.example.com/feed", SampleFeeds.episodic("Hobby", "Games &amp; Hobbies", count = 3))
        web.status("https://gone.example.com/feed", HttpStatusCode.Gone)
    }

    @Test
    fun prepareListsShowsAlphabeticallyWithPrefilledCategories() = runTest {
        serveAll()
        val candidates = importer.prepare(opml)
        assertEquals(listOf("Gone", "History & Friends", "Hobby", "Zeta"), candidates.map { it.title })
        val byTitle = candidates.associateBy { it.title }
        val history = byTitle.getValue("History & Friends")
        assertEquals(AppleCategories.categoryId("Society & Culture"), history.suggestedCategory)
        assertEquals(AppleCategories.categoryId("Leisure"), byTitle.getValue("Hobby").suggestedCategory, "legacy name")
        assertEquals(AppleCategories.categoryId("Technology"), byTitle.getValue("Zeta").suggestedCategory)
        val gone = byTitle.getValue("Gone")
        assertEquals(Category.UNCATEGORISED_ID, gone.suggestedCategory)
        assertEquals("HTTP 410", gone.problem)
        assertNull(db.showQueries.selectAll().executeAsList().firstOrNull(), "nothing is written before confirm")
    }

    @Test
    fun progressCountsEveryFeedThenEveryShow() = runTest {
        serveAll()
        var started = emptyList<OpmlEntry>()
        val read = mutableMapOf<Int, ImportCandidate>()
        val candidates = importer.prepare(
            opml,
            listener = object : ImportListener {
                override fun onStarted(entries: List<OpmlEntry>) {
                    started = entries
                }

                override fun onFeedRead(index: Int, candidate: ImportCandidate) {
                    read[index] = candidate
                }
            },
        )
        assertEquals(listOf("Zeta", "Alpha", "Hobby", "Gone"), started.map { it.title })
        assertEquals(started, (0..3).map { read.getValue(it).entry }, "each index reports its own feed")
        assertEquals("HTTP 410", read.getValue(3).problem)
        val saving = mutableListOf<Pair<Int, Int>>()
        importer.confirm(candidates.map { it to it.suggestedCategory }) { done, total -> saving += done to total }
        assertEquals((1..4).map { it to 4 }, saving)
    }

    @Test
    fun confirmSubscribesAndAppliesRetention() = runTest {
        serveAll()
        val candidates = importer.prepare(opml)
        val choices = candidates.map { it to it.suggestedCategory }
        val ids = importer.confirm(choices)

        assertEquals(4, ids.size)
        val library = LibraryRepository(db).load()
        val zeta = ShowIds.fromFeedUrl("https://zeta.example.com/feed")
        assertEquals(10, library.episodesOf(zeta).size, "episodic: latest 10 kept")
        assertEquals("Zeta-1", library.episodesOf(zeta).maxBy { it.publishedAt }.key.guid)
        val alpha = ShowIds.fromFeedUrl("https://alpha.example.com/feed")
        assertEquals(2, library.episodesOf(alpha).size, "serial: everything kept")
        val gone = ShowIds.fromFeedUrl("https://gone.example.com/feed")
        assertEquals(0, library.episodesOf(gone).size, "subscribed anyway; fetched on a later refresh")
    }

    @Test
    fun refreshHandlesNotModifiedUnavailableAndMovedFeeds() = runTest {
        serveAll()
        importer.confirm(importer.prepare(opml).map { it to it.suggestedCategory })
        val zeta = ShowIds.fromFeedUrl("https://zeta.example.com/feed")
        val alpha = ShowIds.fromFeedUrl("https://alpha.example.com/feed")

        web.serve("https://zeta.example.com/feed") { request ->
            if (request.headers["If-None-Match"] == "\"z1\"") respond("", HttpStatusCode.NotModified)
            else respond("unexpected unconditional fetch", HttpStatusCode.OK)
        }
        assertEquals(RefreshOutcome.NOT_MODIFIED, refresher.refresh(zeta))

        web.status("https://zeta.example.com/feed", HttpStatusCode.Forbidden)
        assertEquals(RefreshOutcome.UNAVAILABLE, refresher.refresh(zeta))
        assertEquals(FeedStatus.UNAVAILABLE.name, store.fetchState(zeta)?.status)
        assertEquals(10, LibraryRepository(db).load().episodesOf(zeta).size, "nothing deleted when a feed lapses")

        // The messy feed announces itunes:new-feed-url: the show moves but keeps its ID.
        assertEquals(RefreshOutcome.UPDATED, refresher.refresh(alpha))
        val moved = assertNotNull(store.show(alpha))
        assertEquals("https://new.example.com/feed", moved.feed_url)
        assertNull(store.fetchState(alpha)?.etag, "old URL's cache headers dropped")
    }
}
