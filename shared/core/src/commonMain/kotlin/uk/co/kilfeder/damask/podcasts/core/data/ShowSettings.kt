package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeOrder
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.db.Show as ShowRow

/**
 * What you change about a show from its page (spec › Screens › Show): its genre, its episode
 * order, and whether you subscribe. Each change is a synced write with a fresh HLC, so the newest
 * change wins on every device. Not thread-safe; call on the database dispatcher.
 */
class ShowSettings(private val db: DamaskDatabase, private val clock: DeviceClock) {

    fun setCategory(id: ShowId, categoryId: CategoryId) = update(id) { it.copy(category_id = categoryId.value) }

    /**
     * The order the show's page lists its episodes in, which is also the order they play in.
     * null = automatic (oldest first for serials, newest first otherwise).
     */
    fun setEpisodeOrder(id: ShowId, order: EpisodeOrder?) =
        update(id) { it.copy(episode_order = Columns.episodeOrder(order)) }

    /** Unsubscribing keeps the row (a tombstone), so Played keeps the show and undo is a write back. */
    fun setSubscribed(id: ShowId, subscribed: Boolean) =
        update(id) { it.copy(subscribed = Columns.bool(subscribed)) }

    private fun update(id: ShowId, change: (ShowRow) -> ShowRow) = db.transaction {
        val row = db.showQueries.selectById(id.value).executeAsOneOrNull() ?: return@transaction
        val next = change(row)
        db.showQueries.upsert(
            id = next.id,
            feed_url = next.feed_url,
            feed_url_norm = next.feed_url_norm,
            category_id = next.category_id,
            episode_order = next.episode_order,
            keep_mode = next.keep_mode,
            keep_count = next.keep_count,
            subscribed = next.subscribed,
            hlc = clock.issue().value,
        )
    }
}
