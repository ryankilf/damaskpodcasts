package uk.co.kilfeder.damask.podcasts.core.importer

import kotlinx.coroutines.test.runTest
import uk.co.kilfeder.damask.podcasts.core.NOW_MS
import uk.co.kilfeder.damask.podcasts.core.data.CategoryPrefill
import uk.co.kilfeder.damask.podcasts.core.data.DeviceClock
import uk.co.kilfeder.damask.podcasts.core.data.FeedStore
import uk.co.kilfeder.damask.podcasts.core.data.inMemoryDriver
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.feed.AppleCategories
import uk.co.kilfeder.damask.podcasts.core.feed.MockFeeds
import uk.co.kilfeder.damask.podcasts.core.feed.SampleFeeds
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** Discover's single-show path: Apple's genre, previewing without subscribing, subscribing. */
class DiscoverSubscribeTest {
    private val driver = inMemoryDriver()
    private val db = DamaskDatabase(driver)
    private val store = FeedStore(db, DeviceClock(db) { NOW_MS })
    private val web = MockFeeds()
    private val importer = OpmlImporter(web.fetcher, store, CategoryPrefill(db))
    private val url = "https://zeta.example.com/feed"

    @AfterTest
    fun close() = driver.close()

    @Test
    fun applesMainGenreWinsOverTheFeeds() = runTest {
        web.feed(url, SampleFeeds.episodic("Zeta", "Technology", count = 3))
        val candidate = importer.prepareFeed("Zeta", url, appleGenres = listOf("History"))
        assertEquals(AppleCategories.categoryId("History"), candidate.suggestedCategory)
    }

    @Test
    fun aSubgenreMapsThroughItsParent() = runTest {
        web.feed(url, SampleFeeds.episodic("Zeta", "Technology", count = 3))
        val candidate = importer.prepareFeed("Zeta", url, appleGenres = listOf("Daily News", "News", "Politics"))
        assertEquals(AppleCategories.categoryId("News"), candidate.suggestedCategory)
    }

    @Test
    fun withoutAppleGenresTheFeedsCategoryIsUsed() = runTest {
        web.feed(url, SampleFeeds.episodic("Zeta", "Technology", count = 3))
        assertEquals(AppleCategories.categoryId("Technology"), importer.prepareFeed("Zeta", url).suggestedCategory)
        val unknown = importer.prepareFeed("Zeta", url, appleGenres = listOf("Nonsense"))
        assertEquals(AppleCategories.categoryId("Technology"), unknown.suggestedCategory)
    }

    @Test
    fun previewStoresTheShowUnsubscribedWithItsEpisodes() = runTest {
        web.feed(url, SampleFeeds.episodic("Zeta", "Technology", count = 3))
        val id = importer.preview(importer.prepareFeed("Zeta", url, listOf("History")))
        val row = assertNotNull(store.show(id))
        assertEquals(0L, row.subscribed)
        assertEquals(AppleCategories.categoryId("History").value, row.category_id)
        assertEquals(emptyList(), store.subscribedShowIds())
        assertEquals(3, db.libraryQueries.selectFeedEpisodesForShow(id.value).executeAsList().size)
    }

    @Test
    fun previewNeverUnsubscribesOrMovesAShowYouHave() = runTest {
        web.feed(url, SampleFeeds.episodic("Zeta", "Technology", count = 3))
        val candidate = importer.prepareFeed("Zeta", url, listOf("History"))
        val id = importer.confirm(listOf(candidate to AppleCategories.categoryId("Comedy"))).single()
        importer.preview(candidate)
        val row = assertNotNull(store.show(id))
        assertEquals(1L, row.subscribed)
        assertEquals(AppleCategories.categoryId("Comedy").value, row.category_id)
    }
}
