package uk.co.kilfeder.damask.podcasts.core.rotation

import uk.co.kilfeder.damask.podcasts.core.NOW_MS
import uk.co.kilfeder.damask.podcasts.core.category
import uk.co.kilfeder.damask.podcasts.core.daysAgo
import uk.co.kilfeder.damask.podcasts.core.episode
import uk.co.kilfeder.damask.podcasts.core.guids
import uk.co.kilfeder.damask.podcasts.core.finished
import uk.co.kilfeder.damask.podcasts.core.model.GenreOrder
import uk.co.kilfeder.damask.podcasts.core.rules.ListeningRules
import uk.co.kilfeder.damask.podcasts.core.show
import kotlin.test.Test
import kotlin.test.assertEquals

class GenreOrderTest {
    private val shows = listOf(show("a", "news"), show("b", "news"))
    private val episodes = listOf(
        episode("a", "a-old", publishedDaysAgo = 9).finished(daysAgo(1)),
        episode("a", "a3", publishedDaysAgo = 3),
        episode("a", "a1", publishedDaysAgo = 1),
        episode("b", "b4", publishedDaysAgo = 4),
        episode("b", "b2", publishedDaysAgo = 2),
    )

    private fun rotation(order: GenreOrder) = Rotation(
        Library(listOf(category("news").copy(order = order)), shows, episodes),
        ListeningRules(),
        NOW_MS,
    )

    @Test
    fun varietyGoesShowByShowLeastRecentlyPlayedFirst() {
        // b was never played, so it leads; each round takes one unplayed episode per show.
        assertEquals(listOf("b2", "a1", "b4", "a3"), rotation(GenreOrder.VARIETY).genreQueue(news).guids)
    }

    @Test
    fun newestAndOldestFirstListEveryUnplayedEpisodeByDate() {
        assertEquals(listOf("a1", "b2", "a3", "b4"), rotation(GenreOrder.NEWEST_FIRST).genreQueue(news).guids)
        assertEquals(listOf("b4", "a3", "b2", "a1"), rotation(GenreOrder.OLDEST_FIRST).genreQueue(news).guids)
    }

    @Test
    fun upNextTakesTheGenresEpisodesInItsOrder() {
        assertEquals(listOf("b4", "a3", "b2", "a1"), rotation(GenreOrder.OLDEST_FIRST).upNext().guids)
    }

    private val news = uk.co.kilfeder.damask.podcasts.core.model.CategoryId("news")
}
