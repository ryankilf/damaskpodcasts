package uk.co.kilfeder.damask.podcasts.ui.components

import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource

/**
 * What episode rows need to know from the shell (which one is playing, which one is open) and how
 * their buttons reach the player.
 */
internal class RowPlayer(
    private val playing: EpisodeId?,
    private val playingNow: Boolean,
    private val onPlayPause: (EpisodeId, PlaySource) -> Unit,
    /** The episode whose screen is open (beside the list on wide layouts), highlighted in lists. */
    private val opened: EpisodeId? = null,
) {
    fun isPlaying(id: EpisodeId) = playingNow && playing == id

    fun isOpened(id: EpisodeId) = opened == id

    fun playPause(id: EpisodeId, source: PlaySource) = onPlayPause(id, source)
}
