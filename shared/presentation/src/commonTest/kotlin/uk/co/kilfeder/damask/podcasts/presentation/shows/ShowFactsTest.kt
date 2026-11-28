package uk.co.kilfeder.damask.podcasts.presentation.shows

import kotlinx.datetime.TimeZone
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ShowFactsTest {
    private val day = 86_400_000L

    /** Thu 26 Nov 2026, 05:00 UTC. */
    private val thursday = 1_795_669_200_000L

    private fun episodes(vararg daysAgo: Long) = daysAgo.mapIndexed { i, ago ->
        Episode(EpisodeKey(ShowId("s"), "g$i"), publishedAt = thursday - ago * day)
    }

    @Test
    fun cadenceComesFromTheTypicalGap() {
        assertEquals(Cadence.DAILY, ShowFacts.cadence(episodes(0, 1, 2, 3, 4)))
        assertEquals(Cadence.WEEKLY, ShowFacts.cadence(episodes(0, 7, 14, 21, 35)), "one missed week doesn't change it")
        assertEquals(Cadence.FORTNIGHTLY, ShowFacts.cadence(episodes(0, 14, 28, 42)))
        assertEquals(Cadence.MONTHLY, ShowFacts.cadence(episodes(0, 30, 61, 91)))
        assertNull(ShowFacts.cadence(episodes(0, 90, 200, 400)), "too rare to name")
        assertNull(ShowFacts.cadence(episodes(0, 7)), "too few to tell")
    }

    @Test
    fun usualDayNeedsMostEpisodesOnIt() {
        val utc = TimeZone.UTC
        assertEquals("Thursdays", ShowFacts.usualDay(episodes(0, 7, 14, 21, 28), utc))
        assertNull(ShowFacts.usualDay(episodes(0, 1, 2, 3, 4), utc))
    }

    @Test
    fun undatedEpisodesAreIgnored() {
        val undated = Episode(EpisodeKey(ShowId("s"), "x"), publishedAt = 0)
        assertEquals("Oct 2026", ShowFacts.since(episodes(0, 7, 30) + undated, TimeZone.UTC))
    }
}
