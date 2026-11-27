package uk.co.kilfeder.damask.podcasts.core.rotation

import uk.co.kilfeder.damask.podcasts.core.model.Category
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.Hlc
import uk.co.kilfeder.damask.podcasts.core.model.Show
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.model.episodeComparator
import uk.co.kilfeder.damask.podcasts.core.model.laterOf
import uk.co.kilfeder.damask.podcasts.core.rules.ListeningRules

/**
 * Every list in the app, computed at read time from a [Library] snapshot (spec › Ordering rules).
 * One rule orders everything: unfinished first, then least recently played (LRP). Listening
 * reorders everything with no state to update: build a new [Rotation] from the new snapshot.
 *
 * Lists that can grow without bound ([genreQueue], [upNext]) are lazy sequences; page with
 * `drop(offset).take(pageSize)`. Generation ends when a full round adds nothing.
 */
class Rotation(
    private val library: Library,
    private val rules: ListeningRules,
    private val nowMs: Long,
) {
    /** lastListened for every category and show in the snapshot. */
    val history: ListenHistory by lazy { ListenHistory(library, rules) }

    /** Genres list: categories by LRP, never-listened first (then by name, for a stable order). */
    fun genres(): List<Category> = library.categories.sortedWith(
        compareBy<Category, Hlc?>(nullsFirst()) { history.lastListened(it.id) }
            .thenBy { it.name.lowercase() }
            .thenBy { it.id.value },
    )

    /** Home › In Progress: started, unfinished, unexpired episodes; most recently played first. */
    fun inProgress(): List<Episode> = library.subscribedShows
        .flatMap { show -> library.episodesOf(show.id).filter { rules.isInProgress(it, show, nowMs) } }
        .sortedByDescending { it.position?.hlc }

    /**
     * A category's queue: its in-progress episodes (most recently played first), then one episode
     * per show, shows by LRP, round-robin. Played episodes being re-listened stay out of queues.
     */
    fun genreQueue(categoryId: CategoryId): Sequence<Episode> {
        val inProgress = showsIn(categoryId)
            .flatMap { show ->
                library.episodesOf(show.id).filter { !it.isPlayed && rules.isInProgress(it, show, nowMs) }
            }
            .sortedByDescending { it.position?.hlc }
        return inProgress.asSequence() + showRounds(categoryId)
    }

    /**
     * The shows pictured on a genre's row: whose episodes come next in its queue, in queue order,
     * then (when the queue runs dry first) its other shows by LRP. Reads the lazy queue only as
     * far as it needs, so it costs about the same as the first page of Up Next.
     */
    fun coverShows(categoryId: CategoryId, count: Int): List<Show> {
        val shows = showsIn(categoryId).sortedWith(byShowLrp)
        val queued = genreQueue(categoryId)
            .map { it.key.showId }
            .distinct()
            .take(minOf(count, shows.size))
            .mapNotNull { library.showsById[it] }
            .toList()
        return (queued + shows.filter { it !in queued }).take(count)
    }

    /**
     * Home › Up Next: one episode per category, categories by LRP, repeated to fill. Each category
     * offers its genre-queue candidates that aren't in progress.
     */
    fun upNext(): Sequence<Episode> = roundRobin(genres().map { showRounds(it.id) })

    /** Every episode of a show in the show's own order, played or not. Used by autoplay. */
    fun showOrder(showId: ShowId): List<Episode> {
        val show = library.showsById[showId] ?: return emptyList()
        return library.episodesOf(showId).sortedWith(show.episodeComparator())
    }

    /**
     * Played screen: every episode with position or played state, at its most recent activity,
     * newest first. Includes unsubscribed shows; one entry per episode.
     */
    fun played(): List<Episode> = library.episodes
        .filter { it.position != null || it.played != null }
        .sortedByDescending { laterOf(it.position?.hlc, it.played?.hlc) }

    fun isInProgress(episode: Episode): Boolean {
        val show = library.showsById[episode.key.showId] ?: return false
        return rules.isInProgress(episode, show, nowMs)
    }

    /** The round-robin part of a genre queue: shows by LRP, one unplayed episode each per round. */
    private fun showRounds(categoryId: CategoryId): Sequence<Episode> =
        roundRobin(showsIn(categoryId).sortedWith(byShowLrp).map { candidates(it) })

    /** Shows within a genre: LRP, never listened first, then by ID for a stable order. */
    private val byShowLrp: Comparator<Show> =
        compareBy<Show, Hlc?>(nullsFirst()) { history.lastListened(it.id) }.thenBy { it.id.value }

    /** A show's unplayed, not-in-progress episodes in its own order (newest- or oldest-first). */
    private fun candidates(show: Show): Sequence<Episode> = library.episodesOf(show.id)
        .asSequence()
        .filter { !it.isPlayed && !rules.isInProgress(it, show, nowMs) }
        .sortedWith(show.episodeComparator())

    private fun showsIn(categoryId: CategoryId): List<Show> = library.showsByCategory[categoryId].orEmpty()
}
