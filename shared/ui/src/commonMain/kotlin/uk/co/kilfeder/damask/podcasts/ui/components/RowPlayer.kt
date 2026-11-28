package uk.co.kilfeder.damask.podcasts.ui.components

import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource

/** What episode rows need to know about the player, and how their buttons reach it. */
internal class RowPlayer(
    private val playing: EpisodeId?,
    private val playingNow: Boolean,
    private val onPlayPause: (EpisodeId, PlaySource) -> Unit,
) {
    fun isPlaying(id: EpisodeId) = playingNow && playing == id

    fun playPause(id: EpisodeId, source: PlaySource) = onPlayPause(id, source)
}
