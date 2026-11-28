package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.NOW_MS
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeOrder
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShowSettingsTest {
    private val driver = inMemoryDriver()
    private val db = DamaskDatabase(driver)
    private val clock = DeviceClock(db) { NOW_MS }
    private val settings = ShowSettings(db, clock)
    private val id = ShowId("s")

    init {
        listOf("a", "b").forEach { db.categoryQueries.upsert(it, it, is_system = 0, deleted = 0, hlc = 1) }
        db.showQueries.upsert("s", "https://s.example.com/feed", "s.example.com/feed", "a", null, null, 10, 1, hlc = 1)
    }

    @AfterTest
    fun close() = driver.close()

    private fun show() = LibraryRepository(db).load().showsById.getValue(id)

    @Test
    fun changesGenreOrderAndSubscription() {
        settings.setCategory(id, CategoryId("b"))
        settings.setEpisodeOrder(id, EpisodeOrder.OLDEST_FIRST)
        assertEquals(CategoryId("b"), show().categoryId)
        assertEquals(EpisodeOrder.OLDEST_FIRST, show().episodeOrder)
        assertEquals("https://s.example.com/feed", show().feedUrl)

        settings.setSubscribed(id, false)
        assertFalse(show().subscribed, "a tombstone: the show stays, unsubscribed")
        settings.setSubscribed(id, true)
        assertTrue(show().subscribed)

        settings.setEpisodeOrder(id, null)
        assertEquals(EpisodeOrder.NEWEST_FIRST, show().episodeOrder, "back to automatic")
    }

    @Test
    fun eachChangeIsANewerSyncedWrite() {
        val before = db.showQueries.selectById("s").executeAsOne().hlc
        settings.setCategory(id, CategoryId("b"))
        assertTrue(db.showQueries.selectById("s").executeAsOne().hlc > before)
    }

    @Test
    fun anUnknownShowIsIgnored() {
        settings.setCategory(ShowId("missing"), CategoryId("b"))
        assertEquals(1, LibraryRepository(db).load().subscribedShows.size)
    }
}
