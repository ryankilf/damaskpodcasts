package uk.co.kilfeder.damask.podcasts.presentation.navigation

import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId

/** The four top-level tabs (design hand-off › flow map). */
enum class Tab { HOME, GENRES, PLAYED, SEARCH }

/** Screens pushed on top of a tab. */
sealed interface Destination {
    data class GenreQueue(val categoryId: String) : Destination

    data class Episode(val id: EpisodeId) : Destination

    data class ShowNotes(val id: EpisodeId) : Destination

    data object InProgress : Destination

    data object Settings : Destination

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
}
