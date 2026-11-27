package uk.co.kilfeder.damask.podcasts.presentation.playback

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext
import uk.co.kilfeder.damask.podcasts.core.model.PlayedBy
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import kotlin.time.Instant

/**
 * The episode being played and its bookkeeping (spec › Sync › Write triggers): the position is
 * saved every 30 seconds while playing and on pause, stop and seek, and each save adds the time
 * listened since the last one to today's `listen_stats`. Time skipped by seeking never counts.
 */
internal class ListeningSession(
    private val graph: AppGraph,
    val key: EpisodeKey,
    val context: PlayContext,
    private val categoryId: CategoryId?,
) {
    /** Wall-clock start and position of the current stretch of listening; null while paused. */
    private var stretchWallMs: Long? = null
    private var stretchPositionMs = 0L
    private var lastSaveWallMs = 0L

    var durationMs: Long? = null

    fun startStretch(positionMs: Long) {
        stretchWallMs = graph.wallClockMs()
        stretchPositionMs = positionMs
        lastSaveWallMs = graph.wallClockMs()
    }

    fun isSaveDue(): Boolean =
        stretchWallMs != null && graph.wallClockMs() - lastSaveWallMs >= SAVE_INTERVAL_MS

    /**
     * Saves [positionMs] and the listening time up to it. With [stopped], also applies the finish
     * rule (stopping in the last couple of minutes finishes the episode). With [continueFromMs],
     * a new stretch starts there (still playing, or after a seek).
     */
    suspend fun save(positionMs: Long, stopped: Boolean, continueFromMs: Long? = null) =
        record(savedMs = positionMs, listenedToMs = positionMs, stopped = stopped, continueFromMs = continueFromMs)

    /**
     * A seek from [fromMs] to [toMs]: listening time counts up to [fromMs] only, and the new
     * position is saved. A new stretch starts at [toMs] when still [playing].
     */
    suspend fun seeked(fromMs: Long, toMs: Long, playing: Boolean) =
        record(savedMs = toMs, listenedToMs = fromMs, stopped = false, continueFromMs = toMs.takeIf { playing })

    private suspend fun record(savedMs: Long, listenedToMs: Long, stopped: Boolean, continueFromMs: Long?) {
        val now = graph.wallClockMs()
        val wallMs = stretchWallMs?.let { now - it } ?: 0
        val contentMs = if (stretchWallMs == null) 0 else (listenedToMs - stretchPositionMs).coerceAtLeast(0)
        val finished = stopped && graph.rules.isFinishedOnStop(savedMs, durationMs, reachedEnd = false)
        // One transaction, so the position, the listening time and the finish land together.
        graph.onDb {
            graph.db.transaction {
                graph.userState.savePosition(key, savedMs, durationMs, context)
                categoryId?.let { graph.userState.addListenTime(it, today(now), wallMs, contentMs) }
                if (finished) graph.userState.markPlayed(key, PlayedBy.FINISHED)
            }
        }
        lastSaveWallMs = now
        stretchWallMs = continueFromMs?.let { now }
        stretchPositionMs = continueFromMs ?: stretchPositionMs
    }

    /** Played to the true end: the position goes to the end, and the episode is finished. */
    suspend fun finish(positionMs: Long) {
        save(positionMs, stopped = false)
        graph.onDb { graph.userState.markPlayed(key, PlayedBy.FINISHED) }
    }

    private fun today(nowMs: Long): String =
        Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

    companion object {
        const val SAVE_INTERVAL_MS = 30_000L
    }
}
