package uk.co.kilfeder.damask.podcasts.presentation.shows

import uk.co.kilfeder.damask.podcasts.core.model.Show
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation

/** How Shows is sorted (remembered in settings). */
enum class ShowsOrder {
    A_TO_Z,
    Z_TO_A,

    /** The show you listened to last at the top: finding what you were just listening to. */
    RECENTLY_PLAYED,

    /** Never-played shows first, then the longest since you listened. */
    LEAST_RECENTLY_PLAYED,
}

/**
 * [shows] in [order]. Names compare ignoring case and a leading "The ", and shows with no name yet
 * (unread feeds) go last in both A–Z and Z–A. Played orders fall back to A–Z between equals.
 */
internal fun sortShows(shows: List<Show>, order: ShowsOrder, rotation: Rotation): List<Show> {
    val byName = compareBy<Show>({ it.title.isBlank() }, { it.sortKey() }, { it.id.value })
    val lastPlayed = { show: Show -> rotation.history.lastListened(show.id)?.wallMs }
    return shows.sortedWith(
        when (order) {
            ShowsOrder.A_TO_Z -> byName
            ShowsOrder.Z_TO_A -> compareBy<Show> { it.title.isBlank() }
                .thenByDescending { it.sortKey() }
                .thenBy { it.id.value }
            ShowsOrder.RECENTLY_PLAYED -> compareByDescending<Show> { lastPlayed(it) ?: Long.MIN_VALUE }
                .then(byName)
            ShowsOrder.LEAST_RECENTLY_PLAYED -> compareBy<Show> { lastPlayed(it) ?: Long.MIN_VALUE }
                .then(byName)
        },
    )
}

/** A name to sort by: lower case, without a leading "The ". */
private fun Show.sortKey(): String = title.lowercase().removePrefix("the ").trim()
