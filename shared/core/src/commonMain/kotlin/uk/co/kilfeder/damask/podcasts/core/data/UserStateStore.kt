package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.Hlc
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext
import uk.co.kilfeder.damask.podcasts.core.model.PlayedBy

/**
 * Writes this device's user state: playback position, played state, the player's duration.
 *
 * Every write gets a fresh HLC from the [DeviceClock]. Each write also makes sure the episode has
 * its `episode_meta` record, so Played outlives the feed.
 *
 * Writes aren't queued in the outbox yet: the private build has no sync. When sync lands, enqueue
 * here, inside the same transaction.
 *
 * Not thread-safe: use from a single database dispatcher.
 */
class UserStateStore(
    private val db: DamaskDatabase,
    private val clock: DeviceClock,
) {
    /** Called on the spec's write triggers: every 30 s of playback, pause, stop, seek. */
    fun savePosition(key: EpisodeKey, positionMs: Long, durationAtSaveMs: Long?, context: PlayContext) =
        db.transaction {
            val hlc = issue()
            ensureMeta(key, hlc)
            writePosition(key, positionMs, durationAtSaveMs, context, hlc)
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
        writePosition(key, positionMs = 0, durationAtSaveMs = null, context = PlayContext.None, hlc = issue())
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

    /**
     * Adds listening time to today's `listen_stats` row for [categoryId]: [wallMs] of real time,
     * [contentMs] of audio (more than wall time when sped up). Time skipped by seeking isn't added.
     */
    fun addListenTime(categoryId: CategoryId, day: String, wallMs: Long, contentMs: Long) {
        if (wallMs <= 0 && contentMs <= 0) return
        db.listenStatsQueries.addLocal(
            device_id = clock.deviceId,
            day = day,
            category_id = categoryId.value,
            wall_ms = wallMs,
            content_ms = contentMs,
            hlc = issue().value,
        )
    }

    private fun writePosition(
        key: EpisodeKey,
        positionMs: Long,
        durationAtSaveMs: Long?,
        context: PlayContext,
        hlc: Hlc,
    ) = db.episodeStateQueries.upsertPosition(
        show_id = key.showId.value,
        guid = key.guid,
        position_ms = positionMs,
        duration_at_save_ms = durationAtSaveMs,
        play_context = context.encode(),
        hlc = hlc.value,
    )

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

    private fun issue(): Hlc = clock.issue()
}
