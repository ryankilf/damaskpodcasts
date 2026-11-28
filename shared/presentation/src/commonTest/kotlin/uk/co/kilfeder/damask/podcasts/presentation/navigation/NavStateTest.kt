package uk.co.kilfeder.damask.podcasts.presentation.navigation

import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import kotlin.test.Test
import kotlin.test.assertEquals

class NavStateTest {
    private val episode = Destination.Episode(EpisodeId("show-1", "ep-1"))

    @Test
    fun openingAShowFromAnotherTabGoesToShows() {
        val fromHome = NavState(tab = Tab.HOME, stack = listOf(episode))
        assertEquals(
            NavState(tab = Tab.SHOWS, stack = listOf(Destination.Show("show-1"))),
            fromHome.openIn(Tab.SHOWS, Destination.Show("show-1")),
        )
    }

    @Test
    fun openingAShowInShowsReplacesTheOpenOne() {
        val inShows = NavState(tab = Tab.SHOWS, stack = listOf(Destination.Show("a")))
        assertEquals(
            NavState(tab = Tab.SHOWS, stack = listOf(Destination.Show("b"))),
            inShows.openIn(Tab.SHOWS, Destination.Show("b")),
        )
    }

    @Test
    fun openingAGenreFromAnEpisodeGoesToGenres() {
        val fromShows = NavState(tab = Tab.SHOWS, stack = listOf(Destination.Show("a"), episode))
        assertEquals(
            NavState(tab = Tab.GENRES, stack = listOf(Destination.GenreQueue("history"))),
            fromShows.openIn(Tab.GENRES, Destination.GenreQueue("history")),
        )
    }
}
