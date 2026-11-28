package uk.co.kilfeder.damask.podcasts.core.rotation

import uk.co.kilfeder.damask.podcasts.core.NOW_MS
import uk.co.kilfeder.damask.podcasts.core.at
import uk.co.kilfeder.damask.podcasts.core.category
import uk.co.kilfeder.damask.podcasts.core.daysAgo
import uk.co.kilfeder.damask.podcasts.core.episode
import uk.co.kilfeder.damask.podcasts.core.finished
import uk.co.kilfeder.damask.podcasts.core.guids
import uk.co.kilfeder.damask.podcasts.core.hoursAgo
import uk.co.kilfeder.damask.podcasts.core.listened
import uk.co.kilfeder.damask.podcasts.core.markedPlayed
import uk.co.kilfeder.damask.podcasts.core.minutesAgo
import uk.co.kilfeder.damask.podcasts.core.model.Category
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeOrder
import uk.co.kilfeder.damask.podcasts.core.model.Show
import uk.co.kilfeder.damask.podcasts.core.model.ShowType
import uk.co.kilfeder.damask.podcasts.core.rules.ListeningRules
import uk.co.kilfeder.damask.podcasts.core.model.MINUTE_MS
import uk.co.kilfeder.damask.podcasts.core.show
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RotationTest {
    private fun rotation(categories: List<Category>, shows: List<Show>, episodes: List<Episode>) =
        Rotation(Library(categories, shows, episodes), ListeningRules(), NOW_MS)

    // --- Spec › Ordering rules › Worked example -------------------------------------------------

    private val worked = listOf(category("History"), category("Politics"), category("Tech"))
    private val workedShows = listOf(show("hist", "History"), show("pol", "Politics"), show("tech", "Tech"))
    private val workedEpisodes = listOf(
        episode("hist", "h-old").listened(daysAgo(9)),
        episode("hist", "h-new"),
        episode("pol", "p-old").listened(daysAgo(2)),
        episode("pol", "p-new"),
        episode("tech", "t-old").listened(hoursAgo(4)),
        episode("tech", "t-new"),
    )

    @Test
    fun workedExampleUpNextLeadsWithStalestCategory() {
        val upNext = rotation(worked, workedShows, workedEpisodes).upNext()
        assertEquals(listOf("h-new", "p-new", "t-new"), upNext.guids)
    }

    @Test
    fun workedExamplePassingThirtyPercentMovesHistoryToTheBack() {
        val afterListening = workedEpisodes.map { if (it.key.guid == "h-new") it.listened(minutesAgo(1)) else it }
            .plus(episode("hist", "h-next", publishedDaysAgo = 2))
        val upNext = rotation(worked, workedShows, afterListening).upNext()
        assertEquals(listOf("p-new", "t-new", "h-next"), upNext.guids)
    }

    // --- Genres list ----------------------------------------------------------------------------

    @Test
    fun genresAreLrpWithNeverListenedFirst() {
        val categories = worked + category("Arts")
        val genres = rotation(categories, workedShows, workedEpisodes).genres()
        assertEquals(listOf("Arts", "History", "Politics", "Tech"), genres.map { it.name })
    }

    // --- Genre queue ----------------------------------------------------------------------------

    @Test
    fun genreQueueIsInProgressThenShowsRoundRobinByLrp() {
        val shows = listOf(show("a", "News"), show("b", "News"), show("c", "News"))
        val episodes = listOf(
            episode("a", "a-listened", publishedDaysAgo = 9).finished(daysAgo(1)),
            episode("a", "a1", publishedDaysAgo = 1),
            episode("a", "a2", publishedDaysAgo = 2),
            episode("b", "b-listened", publishedDaysAgo = 9).finished(daysAgo(5)),
            episode("b", "b1", publishedDaysAgo = 1),
            episode("b", "b-started", publishedDaysAgo = 3).at(5 * MINUTE_MS, hoursAgo(2)),
            episode("c", "c1", publishedDaysAgo = 1),
            episode("c", "c-started", publishedDaysAgo = 3).at(5 * MINUTE_MS, hoursAgo(1)),
        )
        val queue = rotation(listOf(category("News")), shows, episodes).genreQueue(CategoryId("News"))
        // In progress (most recent first), then c (never listened), b (5 days), a (1 day); round 2: a only.
        assertEquals(listOf("c-started", "b-started", "c1", "b1", "a1", "a2"), queue.guids)
    }

    @Test
    fun coverShowsFollowTheQueueThenFillByLrp() {
        val shows = listOf(show("a", "News"), show("b", "News"), show("c", "News"), show("d", "News"))
        val episodes = listOf(
            episode("a", "a1", publishedDaysAgo = 1).finished(daysAgo(1)),
            episode("b", "b1", publishedDaysAgo = 1),
            episode("b", "b-old", publishedDaysAgo = 9).finished(daysAgo(3)),
            episode("c", "c-started", publishedDaysAgo = 3).at(5 * MINUTE_MS, hoursAgo(1)),
            // d has nothing left to play, so it only appears as filler.
            episode("d", "d1", publishedDaysAgo = 1).finished(daysAgo(7)),
        )
        val rotation = rotation(listOf(category("News")), shows, episodes)
        // Queue: c-started (in progress), then b1. a (fully listened) and d fill, by LRP.
        assertEquals(listOf("c", "b"), rotation.coverShows(CategoryId("News"), 2).map { it.id.value })
        assertEquals(listOf("c", "b", "d", "a"), rotation.coverShows(CategoryId("News"), 4).map { it.id.value })
    }

    @Test
    fun serialShowsOfferOldestFirstAndOverridesWin() {
        val shows = listOf(
            show("serial", "Drama", ShowType.SERIAL),
            show("flipped", "Drama").copy(episodeOrderOverride = EpisodeOrder.OLDEST_FIRST),
        )
        val episodes = listOf(
            episode("serial", "s-ch1", publishedDaysAgo = 30),
            episode("serial", "s-ch2", publishedDaysAgo = 20),
            episode("flipped", "f-old", publishedDaysAgo = 10),
            episode("flipped", "f-new", publishedDaysAgo = 1),
        )
        val queue = rotation(listOf(category("Drama")), shows, episodes).genreQueue(CategoryId("Drama"))
        assertEquals(listOf("f-old", "s-ch1", "f-new", "s-ch2"), queue.guids)
    }

    @Test
    fun playedEpisodesAndRelistensStayOutOfQueues() {
        val shows = listOf(show("a", "News"))
        val episodes = listOf(
            episode("a", "finished").finished(daysAgo(3)),
            episode("a", "marked").markedPlayed(daysAgo(3)),
            episode("a", "relisten", publishedDaysAgo = 5).finished(daysAgo(4)).at(MINUTE_MS, minutesAgo(5)),
            episode("a", "fresh"),
        )
        val rotation = rotation(listOf(category("News")), shows, episodes)
        assertEquals(listOf("fresh"), rotation.genreQueue(CategoryId("News")).guids)
        assertEquals(listOf("relisten"), rotation.inProgress().guids)
    }

    // --- Up Next --------------------------------------------------------------------------------

    @Test
    fun upNextExcludesInProgressAndRepeatsRoundsUntilExhausted() {
        val shows = listOf(show("a", "A"), show("b", "B"))
        val episodes = listOf(
            episode("a", "a1", publishedDaysAgo = 1),
            episode("a", "a2", publishedDaysAgo = 2),
            episode("a", "a3", publishedDaysAgo = 3),
            episode("a", "a-started", publishedDaysAgo = 4).at(MINUTE_MS, minutesAgo(1)),
            episode("b", "b1", publishedDaysAgo = 1),
        )
        val upNext = rotation(listOf(category("A"), category("B")), shows, episodes).upNext()
        assertEquals(listOf("a1", "b1", "a2", "a3"), upNext.guids)
        assertEquals(listOf("a1", "b1"), upNext.take(2).guids, "pages lazily")
    }

    @Test
    fun missingCategoryFallsBackToUncategorised() {
        val categories = listOf(category("Uncategorised", isSystem = true), category("A"))
        val shows = listOf(show("orphan", "deleted-elsewhere"), show("a", "A"))
        val episodes = listOf(episode("orphan", "o1"), episode("a", "a1").listened(minutesAgo(1)), episode("a", "a2"))
        val rotation = rotation(categories, shows, episodes)
        assertEquals(listOf("o1"), rotation.genreQueue(CategoryId("Uncategorised")).guids)
        assertEquals(listOf("o1", "a2"), rotation.upNext().guids)
    }

    @Test
    fun unsubscribedShowsLeaveTheRotationButStayInPlayed() {
        val shows = listOf(show("gone", "A").copy(subscribed = false), show("a", "A"))
        val episodes = listOf(episode("gone", "g1").finished(hoursAgo(1)), episode("gone", "g2"), episode("a", "a1"))
        val rotation = rotation(listOf(category("A")), shows, episodes)
        assertEquals(listOf("a1"), rotation.upNext().guids)
        assertEquals(listOf("g1"), rotation.played().guids)
    }

    // --- In Progress / Played -------------------------------------------------------------------

    @Test
    fun inProgressIsMostRecentlyPlayedFirstAndExpires() {
        val shows = listOf(show("a", "A"), show("serial", "A", ShowType.SERIAL))
        val episodes = listOf(
            episode("a", "older").at(MINUTE_MS, hoursAgo(5)),
            episode("a", "newer").at(MINUTE_MS, hoursAgo(1)),
            episode("a", "expired").at(MINUTE_MS, daysAgo(200)),
            episode("serial", "serial-old").at(MINUTE_MS, daysAgo(400)),
        )
        val inProgress = rotation(listOf(category("A")), shows, episodes).inProgress()
        assertEquals(listOf("newer", "older", "serial-old"), inProgress.guids)
    }

    @Test
    fun playedIsNewestActivityFirstOneEntryPerEpisode() {
        val episodes = listOf(
            episode("a", "x").finished(daysAgo(3)).at(MINUTE_MS, hoursAgo(1)),
            episode("a", "y").markedPlayed(daysAgo(1)),
            episode("a", "never"),
        )
        val played = rotation(listOf(category("A")), listOf(show("a", "A")), episodes).played()
        assertEquals(listOf("x", "y"), played.guids)
    }

    @Test
    fun emptyLibraryProducesEmptyLists() {
        val rotation = rotation(emptyList(), emptyList(), emptyList())
        assertTrue(rotation.upNext().none())
        assertTrue(rotation.genres().isEmpty())
    }
}
