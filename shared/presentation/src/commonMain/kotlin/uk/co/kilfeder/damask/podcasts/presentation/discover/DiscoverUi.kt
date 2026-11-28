package uk.co.kilfeder.damask.podcasts.presentation.discover

import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import uk.co.kilfeder.damask.podcasts.presentation.genres.CategoryOptionUi
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue

/** Where a search result stands for you. */
enum class DiscoverRowState {
    /** Not subscribed: Subscribe. */
    NEW,

    /** Fetching its feed before the genre sheet opens. */
    FETCHING,

    /** You subscribe to it: tapping opens its page. */
    SUBSCRIBED,
}

/** One show in Discover's results. */
data class DiscoverRowUi(
    /** Its normalised feed URL: unique within a search. */
    val key: String,
    val title: String,
    val author: String,
    val artworkUrl: String?,
    val fallback: ArtworkFallback,
    val state: DiscoverRowState,
    /** Set when [state] is [DiscoverRowState.SUBSCRIBED]. */
    val showId: String? = null,
    /** Why its feed couldn't be read, after a Subscribe that failed. */
    val problem: String? = null,
)

/** What Discover shows under the search box. */
sealed interface DiscoverResults {
    /** Nothing typed yet. */
    data object Idle : DiscoverResults

    data object Searching : DiscoverResults

    /** At least one show; Apple's order. */
    data class Found(val rows: List<DiscoverRowUi>) : DiscoverResults

    data object NoMatch : DiscoverResults

    /** Apple's directory refused: try again in a minute. */
    data object Busy : DiscoverResults

    /** Couldn't reach the directory (offline, or an error). */
    data object Failed : DiscoverResults
}

/** The genre sheet for a show being subscribed: its feed has been read. */
data class GenreChoiceUi(val showTitle: String, val suggestedId: String)

data class DiscoverUiState(
    val query: String = "",
    val results: DiscoverResults = DiscoverResults.Idle,
    /** Open while you pick a genre for the show you're subscribing to. */
    val choosing: GenreChoiceUi? = null,
    val genres: List<CategoryOptionUi> = emptyList(),
    /** The colour "Create “…”" in the genre sheet would use. */
    val nextHue: GenreHue = GenreHue.GREY,
)
