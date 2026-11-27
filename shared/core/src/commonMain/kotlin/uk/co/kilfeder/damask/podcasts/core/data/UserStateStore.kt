package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.Hlc
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext
import uk.co.kilfeder.damask.podcasts.core.model.PlayedBy
import uk.co.kilfeder.damask.podcasts.core.sync.HlcClock
import kotlin.uuid.Uuid

/**
 * Writes this device's user state: playback position, played state, the player's duration.
 *
 * Every write gets a fresh HLC from a clock that resumes from `device.last_hlc`, so timestamps
 * keep increasing across restarts even if the wall clock goes backwards. Each write also makes
 * sure the episode has its `episode_meta` record, so Played outlives the feed.
 *
 * Writes aren't queued in the outbox yet: the private build has no sync. When sync lands, enqueue
 * here, inside the same transaction.
 *
 * Not thread-safe: use from a single database dispatcher.
 */
class UserStateStore(
    private val db: DamaskDatabase,
    wallClockMs: () -> Long,
) {
    val deviceId: String
    private val clock: HlcClock

    init {
        val device = db.deviceQueries.selectDevice().executeAsOneOrNull()
            ?: Uuid.random().toString().let { id ->
                db.deviceQueries.insertDevice(id = id, last_hlc = 0)
                db.deviceQueries.selectDevice().executeAsOne()
            }
        deviceId = device.id
        clock = HlcClock(wallClockMs, Hlc(device.last_hlc))
    }

    /** Called on the spec's write triggers: every 30 s of playback, pause, stop, seek. */
    fun savePosition(key: EpisodeKey, positionMs: Long, durationAtSaveMs: Long?, context: PlayContext) =
        db.transaction {
            val hlc = issue()
            ensureMeta(key, hlc)
            db.episodeStateQueries.upsertPosition(
                show_id = key.showId.value,
                guid = key.guid,
                position_ms = positionMs,
                duration_at_save_ms = durationAtSaveMs,
                play_context = context.encode(),
                hlc = hlc.value,
            )
        }

    fun markPlayed(key: EpisodeKey, by: PlayedBy) = db.transaction {
        val hlc = issue()
        ensureMeta(key, hlc)
        writePlayed(key, played = true, by = by, hlc = hlc)
    }

    /** Marking unplayed also resets the position to 0, so it starts again and stops counting. */
    fun markUnplayed(key: EpisodeKey) = db.transaction {
        val hlc = issue()
        ensureMeta(key, hlc)
        writePlayed(key, played = false, by = null, hlc = hlc)
        db.episodeStateQueries.upsertPosition(
            show_id = key.showId.value,
            guid = key.guid,
            position_ms = 0,
            duration_at_save_ms = null,
            play_context = PlayContext.None.encode(),
            hlc = issue().value,
        )
    }

    /** The player's duration replaces the feed's as soon as media is prepared. */
    fun reportPlayerDuration(key: EpisodeKey, durationMs: Long) = db.transaction {
        val hlc = issue()
        ensureMeta(key, hlc)
        val meta = db.episodeMetaQueries.selectByKey(key.showId.value, key.guid).executeAsOneOrNull()
            ?: return@transaction
        if (meta.duration_ms == durationMs) return@transaction
        db.episodeMetaQueries.upsert(
            show_id = meta.show_id,
            guid = meta.guid,
            title = meta.title,
            published_at = meta.published_at,
            duration_ms = durationMs,
            hlc = issue().value,
        )
    }

    /** Called for every HLC pulled from the server, so later local writes order after it. */
    fun receive(remote: Hlc) {
        clock.receive(remote)
        persistClock()
    }

    private fun writePlayed(key: EpisodeKey, played: Boolean, by: PlayedBy?, hlc: Hlc) {
        db.episodeStateQueries.upsertPlayed(
            show_id = key.showId.value,
            guid = key.guid,
            played = Columns.bool(played),
            played_by = by?.name,
            hlc = hlc.value,
        )
    }

    private fun ensureMeta(key: EpisodeKey, hlc: Hlc) {
        db.episodeMetaQueries.insertFromFeedIfAbsent(hlc = hlc.value, show_id = key.showId.value, guid = key.guid)
    }

    private fun issue(): Hlc = clock.issue().also { persistClock() }

    private fun persistClock() {
        db.deviceQueries.advanceLastHlc(last_hlc = clock.last.value, id = deviceId)
    }
}
