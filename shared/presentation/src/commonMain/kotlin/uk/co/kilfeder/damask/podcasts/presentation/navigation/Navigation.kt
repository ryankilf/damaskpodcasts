package uk.co.kilfeder.damask.podcasts.presentation.navigation

import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId

/** The five top-level tabs (design hand-off › NavBar). */
enum class Tab { HOME, GENRES, SHOWS, PLAYED, SEARCH }

/** Screens pushed on top of a tab. */
sealed interface Destination {
    data class GenreQueue(val categoryId: String) : Destination

    /**
     * An episode's screen. Opening it doesn't play it; [source] is where it was opened from, so
     * pressing play there carries on the same way (Up Next, its genre, its show). null = the
     * context saved with it (the mini-player, In progress).
     */
    data class Episode(val id: EpisodeId, val source: PlaySource? = null) : Destination

    data class ShowNotes(val id: EpisodeId) : Destination

    data object InProgress : Destination

    /** A show's page: in Shows, or in Discover for a show you haven't subscribed to. */
    data class Show(val showId: String) : Destination

    data object Settings : Destination

    /** Recolour, rename, delete and add genres (from the Genres header). */
    data object ManageGenres : Destination

    data object Import : Destination
}

/**
 * Navigation state is shared (spec › Architecture › Thin UI rule): which tab, and what's pushed
 * on top of it. Each UI renders it its own way. Switching tab clears the pushed screens.
 */
data class NavState(
    val tab: Tab = Tab.HOME,
    val stack: List<Destination> = emptyList(),
) {
    val top: Destination? get() = stack.lastOrNull()

    fun push(destination: Destination) = copy(stack = stack + destination)

    /** null when there's nothing to go back to (the platform should handle back itself). */
    fun pop(): NavState? = if (stack.isEmpty()) null else copy(stack = stack.dropLast(1))

    fun select(tab: Tab) = NavState(tab = tab)

    /**
     * Opens [destination] in the section it belongs to (a show in Shows, a genre in Genres), so
     * the navigation shows where you are: from elsewhere, that tab with just it open; already in
     * that tab, it replaces what's open (the one beside the list, on wide layouts).
     */
    fun openIn(tab: Tab, destination: Destination): NavState =
        if (this.tab == tab) replaceTop(destination) else NavState(tab = tab, stack = listOf(destination))

    /** Pushes [destination], replacing the top one if it's the same kind (another show, say). */
    fun replaceTop(destination: Destination): NavState {
        val current = top
        val sameKind = current != null && current::class == destination::class
        return (if (sameKind) copy(stack = stack.dropLast(1)) else this).push(destination)
    }
}
