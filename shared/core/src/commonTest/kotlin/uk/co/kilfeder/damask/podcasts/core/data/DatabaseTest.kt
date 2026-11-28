package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.HOUR_MS
import uk.co.kilfeder.damask.podcasts.core.NOW_MS
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.guids
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext
import uk.co.kilfeder.damask.podcasts.core.model.PlayedBy
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.model.ShowType
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.core.model.DAY_MS
import uk.co.kilfeder.damask.podcasts.core.rules.ListeningRules
import uk.co.kilfeder.damask.podcasts.core.model.MINUTE_MS
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DatabaseTest {
    private val driver = inMemoryDriver()
    private val db = DamaskDatabase(driver)
    private var now = NOW_MS
    private val clock = DeviceClock(db) { now }
    private val store = UserStateStore(db, clock)
    private val repository = LibraryRepository(db)

    @AfterTest
    fun close() = driver.close()

    private fun category(id: String) = db.categoryQueries.upsert(id, id, is_system = 0, deleted = 0, hlc = 1)

    private fun show(id: String, category: String, feedUrl: String = "https://$id.example.com/feed", hlc: Long = 1) {
        db.showQueries.upsert(
            id = id,
            feed_url = feedUrl,
            feed_url_norm = feedUrl,
            category_id = category,
            episode_order = null,
            keep_mode = null,
            keep_count = 10,
            subscribed = 1,
            hlc = hlc,
        )
    }

    private fun feedEpisode(show: String, guid: String, publishedDaysAgo: Long = 1) {
        db.feedQueries.upsertEpisode(
            show_id = show,
            guid = guid,
            title = "Episode $guid",
            published_at = NOW_MS - publishedDaysAgo * DAY_MS,
            enclosure_url = "https://cdn.example.com/$guid.mp3",
            image_url = null,
            duration_ms = HOUR_MS,
            notes_html = "<p>Notes for $guid</p>",
        )
    }

    private fun key(show: String, guid: String) = EpisodeKey(ShowId(show), guid)

    // --- Schema --------------------------------------------------------------------------------

    @Test
    fun uncategorisedIsSeededWithAFixedId() {
        val system = db.categoryQueries.selectLive().executeAsList().single()
        assertEquals("uncategorised", system.id)
        assertEquals(1, system.is_system)
    }

    @Test
    fun upsertsAreLastWriteWins() {
        db.episodeStateQueries.upsertPosition("s", "e", 5_000, null, null, hlc = 10)
        db.episodeStateQueries.upsertPosition("s", "e", 1_000, null, null, hlc = 5) // older: ignored
        assertEquals(5_000, db.episodeStateQueries.selectPosition("s", "e").executeAsOne().position_ms)
        db.episodeStateQueries.upsertPosition("s", "e", 9_000, null, null, hlc = 11)
        assertEquals(9_000, db.episodeStateQueries.selectPosition("s", "e").executeAsOne().position_ms)
    }

    @Test
    fun applePrefillPrefersTheSubcategoryRow() {
        category("news")
        category("politics")
        db.appleCategoryMapQueries.upsert("News", "", "news", 0, 1)
        db.appleCategoryMapQueries.upsert("News", "Politics", "politics", 0, 1)
        assertEquals("politics", db.appleCategoryMapQueries.selectPrefill("News", "Politics").executeAsOne())
        assertEquals("news", db.appleCategoryMapQueries.selectPrefill("News", "Daily").executeAsOne())
        assertNull(db.appleCategoryMapQueries.selectPrefill("Comedy", "").executeAsOneOrNull())
    }

    @Test
    fun listenStatsAccumulateLocally() {
        db.listenStatsQueries.addLocal("d", "2026-11-27", "news", wall_ms = 60_000, content_ms = 90_000, hlc = 1)
        db.listenStatsQueries.addLocal("d", "2026-11-27", "news", wall_ms = 30_000, content_ms = 60_000, hlc = 2)
        val row = db.listenStatsQueries.selectByDevice("d").executeAsOne()
        assertEquals(90_000, row.wall_ms)
        assertEquals(150_000, row.content_ms)
    }

    @Test
    fun notesAndMediaAreReadOneEpisodeAtATime() {
        show("s", "uncategorised")
        feedEpisode("s", "e")
        val lookup = EpisodeLookup(db)
        assertEquals("<p>Notes for e</p>", lookup.notes(key("s", "e")))
        assertNull(lookup.notes(key("s", "missing")))
        assertEquals(EpisodeMedia("https://cdn.example.com/e.mp3", null), lookup.media(key("s", "e")))
        assertNull(lookup.media(key("s", "missing")))
    }

    // --- User state ----------------------------------------------------------------------------

    @Test
    fun listenTimeAddsUpPerDayAndCategory() {
        store.addListenTime(CategoryId("news"), "2026-11-27", wallMs = 60_000, contentMs = 90_000)
        store.addListenTime(CategoryId("news"), "2026-11-27", wallMs = 30_000, contentMs = 30_000)
        store.addListenTime(CategoryId("news"), "2026-11-27", wallMs = 0, contentMs = 0)
        val row = db.listenStatsQueries.selectByDevice(clock.deviceId).executeAsList().single()
        assertEquals(90_000, row.wall_ms)
        assertEquals(120_000, row.content_ms)
    }

    @Test
    fun savingPositionCreatesTheMinimalRecordForPlayed() {
        show("s", "uncategorised")
        feedEpisode("s", "e")
        store.savePosition(key("s", "e"), 10 * MINUTE_MS, HOUR_MS, PlayContext.UpNext)
        val meta = assertNotNull(db.episodeMetaQueries.selectByKey("s", "e").executeAsOneOrNull())
        assertEquals("Episode e", meta.title)
        assertEquals("UP_NEXT", db.episodeStateQueries.selectPosition("s", "e").executeAsOne().play_context)
    }

    @Test
    fun markUnplayedResetsThePosition() {
        show("s", "uncategorised")
        feedEpisode("s", "e")
        store.savePosition(key("s", "e"), 40 * MINUTE_MS, HOUR_MS, PlayContext.UpNext)
        store.markPlayed(key("s", "e"), PlayedBy.FINISHED)
        store.markUnplayed(key("s", "e"))
        assertEquals(0, db.episodeStateQueries.selectPlayed("s", "e").executeAsOne().played)
        assertEquals(0, db.episodeStateQueries.selectPosition("s", "e").executeAsOne().position_ms)
    }

    @Test
    fun clockKeepsIncreasingAcrossRestartsEvenIfTheWallClockGoesBack() {
        show("s", "uncategorised")
        feedEpisode("s", "e")
        store.savePosition(key("s", "e"), MINUTE_MS, null, PlayContext.None)
        val first = db.episodeStateQueries.selectPosition("s", "e").executeAsOne().hlc

        now -= DAY_MS // the device clock is wound back a day, then the app restarts
        val restartedClock = DeviceClock(db) { now }
        val restarted = UserStateStore(db, restartedClock)
        restarted.savePosition(key("s", "e"), 2 * MINUTE_MS, null, PlayContext.None)
        val second = db.episodeStateQueries.selectPosition("s", "e").executeAsOne()

        assertEquals(clock.deviceId, restartedClock.deviceId)
        assertTrue(second.hlc > first, "the newer write must win")
        assertEquals(2 * MINUTE_MS, second.position_ms)
    }

    @Test
    fun playerDurationReplacesTheFeedDuration() {
        show("s", "uncategorised")
        feedEpisode("s", "e")
        store.reportPlayerDuration(key("s", "e"), 2 * HOUR_MS)
        val episode = repository.load().episodes.single()
        assertEquals(2 * HOUR_MS, episode.durationMs)
    }

    // --- Library -------------------------------------------------------------------------------

    @Test
    fun playedOutlivesTheFeed() {
        show("s", "uncategorised")
        feedEpisode("s", "old", publishedDaysAgo = 30)
        store.markPlayed(key("s", "old"), PlayedBy.FINISHED)
        db.feedQueries.deleteEpisode("s", "old") // pruned by retention

        val library = repository.load()
        val rotation = Rotation(library, ListeningRules(), now)
        assertEquals(listOf("old"), rotation.played().guids)
        assertEquals(NOW_MS - 30 * DAY_MS, library.episodes.single().publishedAt)
    }

    @Test
    fun loadsShowsWithSettingsAndFeedType() {
        category("drama")
        show("saga", "drama")
        db.feedQueries.upsertShowFeed("saga", "Saga", null, itunes_type = "serial", author = null, description = null)
        val show = repository.load().showsById.getValue(ShowId("saga"))
        assertEquals(ShowType.SERIAL, show.type)
        assertEquals(CategoryId("drama"), show.categoryId)
    }

    @Test
    fun aliasesResolveToTheRootAndMergeEpisodes() {
        show("root", "uncategorised", hlc = 1)
        show("dupe", "uncategorised", hlc = 2)
        db.showAliasQueries.upsert(alias_id = "dupe", canonical_id = "root", hlc = 3)
        feedEpisode("root", "e")
        feedEpisode("dupe", "e")
        db.episodeStateQueries.upsertPosition("root", "e", 1_000, null, null, hlc = 10)
        db.episodeStateQueries.upsertPosition("dupe", "e", 7_000, null, null, hlc = 20) // newer

        val library = repository.load()
        assertEquals(setOf(ShowId("root")), library.showsById.keys)
        val episode = library.episodes.single()
        assertEquals(key("root", "e"), episode.key)
        assertEquals(7_000, episode.position?.positionMs)
    }

    @Test
    fun endToEndTheRotationRunsOverTheDatabase() {
        category("history")
        category("tech")
        show("hist", "history")
        show("tech", "tech")
        feedEpisode("hist", "h-old", publishedDaysAgo = 9)
        feedEpisode("hist", "h-new")
        feedEpisode("tech", "t-old", publishedDaysAgo = 9)
        feedEpisode("tech", "t-new")
        store.markPlayed(key("tech", "t-old"), PlayedBy.FINISHED) // Tech listened just now

        val rotation = Rotation(repository.load(), ListeningRules(), now)
        // History (never listened) leads; one episode per category per round.
        assertEquals(listOf("h-new", "t-new", "h-old"), rotation.upNext().guids)
        assertEquals(listOf("history", "Uncategorised", "tech"), rotation.genres().map { it.name })
    }
}
