package uk.co.kilfeder.damask.podcasts.core.playback

import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation

/** The two user switches that can stop autoplay. */
data class AutoplaySwitches(
    /** The global autoplay setting. */
    val enabled: Boolean = true,
    /** "Stop after this episode" on Now Playing (also a sleep option). */
    val stopAfterThis: Boolean = false,
)

/**
 * What plays when an episode finishes (spec › Finishing and autoplay). The context the episode was
 * started from decides; serial shows don't override it.
 *
 * [rotation] must be built *after* the finish was recorded, so Up Next has already moved the
 * category just heard to the back.
 */
object Autoplay {
    fun next(
        rotation: Rotation,
        context: PlayContext,
        finished: EpisodeKey,
        switches: AutoplaySwitches = AutoplaySwitches(),
    ): Episode? {
        if (!switches.enabled || switches.stopAfterThis) return null
        return when (context) {
            PlayContext.UpNext -> rotation.upNext().firstOrNull { it.key != finished }
            is PlayContext.Genre -> rotation.genreQueue(context.categoryId).firstOrNull { it.key != finished }
            is PlayContext.Show -> nextInShow(rotation, context.showId, finished)
            PlayContext.None -> null
        }
    }

    /** The show's next unplayed episode after [finished], in the show's own order. */
    private fun nextInShow(rotation: Rotation, showId: ShowId, finished: EpisodeKey): Episode? {
        val order = rotation.showOrder(showId)
        val after = order.indexOfFirst { it.key == finished }.let { if (it < 0) order else order.drop(it + 1) }
        return after.firstOrNull { !it.isPlayed && it.key != finished }
    }
}
