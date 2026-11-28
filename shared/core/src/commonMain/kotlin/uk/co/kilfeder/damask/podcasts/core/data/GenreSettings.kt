package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.GenreOrder

/**
 * What you change about a genre from its page: its order, which sets its queue, the episodes it
 * offers Up Next, and what autoplay picks within it. A synced write with a fresh HLC. Not
 * thread-safe; call on the database dispatcher.
 */
class GenreSettings(private val db: DamaskDatabase, private val clock: DeviceClock) {
    /**
     * Oldest first needs every episode, so its shows' feeds are marked to download in full on
     * their next refresh (refresh them straight after, to bring the older episodes back).
     */
    fun setOrder(id: CategoryId, order: GenreOrder) = db.transaction {
        db.categoryQueries.setEpisodeOrder(Columns.genreOrder(order), clock.issue().value, id.value)
        if (order == GenreOrder.OLDEST_FIRST) db.feedFetchStateQueries.forgetValidatorsInCategory(id.value)
    }
}
