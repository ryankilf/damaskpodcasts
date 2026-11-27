package uk.co.kilfeder.damask.podcasts.core.retention

import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.KeepMode
import uk.co.kilfeder.damask.podcasts.core.model.Show
import uk.co.kilfeder.damask.podcasts.core.rules.ListeningRules

/**
 * Feed-cache retention, applied when a feed is fetched (spec › Retention and show type). Only the
 * local `episode` cache is pruned; `episode_meta` (what Played needs) is never touched here.
 *
 * Pinned episodes are never pruned: anything in progress (expired in-progress episodes lose this
 * protection) and anything downloaded.
 */
class Retention(private val rules: ListeningRules) {

    fun episodesToPrune(show: Show, episodes: List<Episode>, nowMs: Long): List<EpisodeKey> {
        if (show.keepMode == KeepMode.ALL) return emptyList()
        val kept = episodes
            .sortedWith(compareByDescending<Episode> { it.publishedAt }.thenBy { it.key.guid })
            .take(show.keepCount.coerceAtLeast(0))
            .mapTo(mutableSetOf()) { it.key }
        return episodes
            .filter { it.key !in kept && !isPinned(it, show, nowMs) }
            .map { it.key }
    }

    private fun isPinned(episode: Episode, show: Show, nowMs: Long): Boolean =
        episode.isDownloaded || rules.isInProgress(episode, show, nowMs)
}
