package uk.co.kilfeder.damask.podcasts.core.rules

import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.Hlc
import uk.co.kilfeder.damask.podcasts.core.model.PlayedBy
import uk.co.kilfeder.damask.podcasts.core.model.Show
import uk.co.kilfeder.damask.podcasts.core.model.ShowType
import uk.co.kilfeder.damask.podcasts.core.model.laterOf
import kotlin.math.roundToLong

/** User-configurable values behind the rules, with the spec's defaults. */
data class ListeningSettings(
    /** An episode counts as listened once the position passes this fraction of its duration. */
    val listenThresholdFraction: Double = DEFAULT_LISTEN_THRESHOLD_FRACTION,
    /** The threshold when the duration is unknown. */
    val unknownDurationThresholdMs: Long = DEFAULT_UNKNOWN_DURATION_THRESHOLD_MS,
    /** Stopping with this much or less remaining finishes the episode. 0 = only the true end counts. */
    val finishTailMs: Long = DEFAULT_FINISH_TAIL_MS,
    /** In-progress episodes untouched for longer than this drop out of In Progress (serials never do). */
    val inProgressExpiryMs: Long = DEFAULT_IN_PROGRESS_EXPIRY_MS,
) {
    companion object {
        const val DEFAULT_LISTEN_THRESHOLD_FRACTION = 0.30
        const val DEFAULT_UNKNOWN_DURATION_THRESHOLD_MS = 10 * MINUTE_MS
        const val DEFAULT_FINISH_TAIL_MS = 2 * MINUTE_MS
        const val DEFAULT_IN_PROGRESS_EXPIRY_MS = 183 * DAY_MS // about 6 months
    }
}

const val MINUTE_MS: Long = 60_000
const val DAY_MS: Long = 24 * 60 * MINUTE_MS

/** The derived values from spec › Data model › Derived values. Pure; nothing here is ever written. */
class ListeningRules(val settings: ListeningSettings = ListeningSettings()) {

    /** The position at which an episode starts counting towards the rotation. */
    fun thresholdMs(episode: Episode): Long {
        val duration = episode.durationMs ?: return settings.unknownDurationThresholdMs
        return (duration * settings.listenThresholdFraction).roundToLong()
    }

    private fun isFinished(episode: Episode): Boolean =
        episode.played?.let { it.played && it.playedBy == PlayedBy.FINISHED } == true

    private fun isPastThreshold(episode: Episode): Boolean =
        episode.position?.let { it.positionMs >= thresholdMs(episode) } == true

    /**
     * counts_as_listened: finished, or the *current* position is at or past the threshold. Scrubbing
     * back below the line undoes it; marking unplayed (position reset to 0) clears it.
     */
    fun countsAsListened(episode: Episode): Boolean = isFinished(episode) || isPastThreshold(episode)

    /** listened_at: the later of the finished time and the past-threshold position time. */
    fun listenedAt(episode: Episode): Hlc? = laterOf(
        episode.played?.hlc?.takeIf { isFinished(episode) },
        episode.position?.hlc?.takeIf { isPastThreshold(episode) },
    )

    /**
     * In progress: started, and either not played or re-listened since being played, and touched
     * within the expiry window (serial shows never expire). Expiry is derived, never written.
     */
    fun isInProgress(episode: Episode, show: Show, nowMs: Long): Boolean {
        val position = episode.position ?: return false
        if (position.positionMs <= 0) return false
        val played = episode.played
        val started = played == null || !played.played || position.hlc > played.hlc
        return started && !isExpired(position.hlc, show, nowMs)
    }

    private fun isExpired(touched: Hlc, show: Show, nowMs: Long): Boolean =
        show.type != ShowType.SERIAL && nowMs - touched.wallMs > settings.inProgressExpiryMs

    /**
     * The finish rule, evaluated only when playback stops (pause, stop, switching episode, closing),
     * never continuously. Finished = reached the true end, or stopped with [ListeningSettings.finishTailMs]
     * or less remaining. With an unknown duration only the true end counts.
     */
    fun isFinishedOnStop(positionMs: Long, durationMs: Long?, reachedEnd: Boolean): Boolean {
        if (reachedEnd) return true
        if (durationMs == null || settings.finishTailMs <= 0) return false
        return durationMs - positionMs <= settings.finishTailMs
    }
}
