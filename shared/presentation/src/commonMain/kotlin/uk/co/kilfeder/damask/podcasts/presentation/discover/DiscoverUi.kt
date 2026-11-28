package uk.co.kilfeder.damask.podcasts.presentation.discover

import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback

/** Where a search result stands for you. */
enum class DiscoverRowState {
    /** Not subscribed: Subscribe. */
    NEW,

    /** Reading its feed to open its page (not subscribing). */
    OPENING,

    /** Reading its feed to subscribe. */
    SUBSCRIBING,

    /** You subscribe to it. */
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
    /** Set when [state] is [DiscoverRowState.SUBSCRIBED]: its page opens straight away. */
    val showId: String? = null,
    /** Why its feed couldn't be read, after a Subscribe or open that failed. */
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

data class DiscoverUiState(
    val query: String = "",
    val results: DiscoverResults = DiscoverResults.Idle,
    /** A show whose page should open now (its feed has just been read); see onShowOpened. */
    val openShowId: String? = null,
)
