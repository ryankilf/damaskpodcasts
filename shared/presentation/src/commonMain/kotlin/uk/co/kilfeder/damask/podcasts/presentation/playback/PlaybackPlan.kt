package uk.co.kilfeder.damask.podcasts.presentation.playback

import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext
import uk.co.kilfeder.damask.podcasts.core.playback.ResumePosition
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph

/** Everything needed to start one episode: the engine request and the session's details. */
internal data class PlaybackPlan(
    val request: MediaRequest,
    val context: PlayContext,
    val categoryId: CategoryId?,
    val durationMs: Long?,
) {
    companion object {
        /** null when the episode or its audio address is gone (it left the feed). */
        suspend fun of(
            graph: AppGraph,
            library: Library,
            key: EpisodeKey,
            requested: PlayContext?,
            speed: Float,
        ): PlaybackPlan? {
            val episode = library.episodesOf(key.showId).firstOrNull { it.key == key } ?: return null
            val media = graph.onDb { graph.lookup.media(key) } ?: return null
            val show = library.showsById[key.showId]
            return PlaybackPlan(
                request = MediaRequest(
                    mediaId = "${key.showId.value}/${key.guid}",
                    uri = media.localPath ?: media.url,
                    startMs = startPosition(graph, library, episode),
                    title = episode.title,
                    showTitle = show?.title.orEmpty(),
                    artworkUrl = episode.imageUrl,
                    speed = speed,
                ),
                context = requested ?: episode.position?.context ?: PlayContext.None,
                categoryId = show?.let(library::effectiveCategory),
                durationMs = episode.durationMs,
            )
        }

        /** The saved position when the episode is in progress, rewound if the copy's length changed. */
        private fun startPosition(graph: AppGraph, library: Library, episode: Episode): Long {
            val saved = episode.position ?: return 0
            val show = library.showsById[episode.key.showId] ?: return 0
            if (!graph.rules.isInProgress(episode, show, graph.wallClockMs())) return 0
            return ResumePosition.resumeAtMs(saved, episode.durationMs)
        }
    }
}
