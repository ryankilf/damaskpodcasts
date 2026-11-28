package uk.co.kilfeder.damask.podcasts.core.data

import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi
import uk.co.kilfeder.damask.podcasts.core.model.GenreColour
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
    /** A new genre (Manage categories, or the show page's genre sheet), in [colour]. */
    @OptIn(ExperimentalUuidApi::class)
    fun create(name: String, colour: GenreColour): CategoryId = db.transactionWithResult {
        val id = Uuid.random().toString()
        db.categoryQueries.upsert(id = id, name = name.trim(), is_system = 0, deleted = 0, hlc = clock.issue().value)
        db.categoryQueries.setColour(colour.storedName, clock.issue().value, id)
        CategoryId(id)
    }

    fun rename(id: CategoryId, name: String) = db.categoryQueries.rename(name.trim(), clock.issue().value, id.value)

    /** Its shows count as Uncategorised from now on (Library.effectiveCategory). */
    fun delete(id: CategoryId) = db.categoryQueries.markDeleted(clock.issue().value, id.value)

    /** The user picked a colour for a genre (Manage categories, or when creating one). */
    fun setColour(id: CategoryId, colour: GenreColour) =
        db.categoryQueries.setColour(colour.storedName, clock.issue().value, id.value)

    /**
     * Gives every genre in use that has no colour the least-used one, so colours only repeat past
     * 16 genres (ties go in palette order). Run at start-up and after anything that can put shows
     * into a new genre (import, moving a show).
     */
    fun assignMissingColours() = db.transaction {
        val used = usedColours().toMutableList()
        db.categoryQueries.selectNeedingColour().executeAsList().forEach { id ->
            val pick = GenreColour.leastUsed(used)
            db.categoryQueries.setColour(pick.storedName, clock.issue().value, id)
            used += pick
        }
    }

    /** The colour a new genre would get now. */
    fun nextColour(): GenreColour = GenreColour.leastUsed(usedColours())

    private fun usedColours(): List<GenreColour> =
        db.categoryQueries.selectUsedColours().executeAsList().mapNotNull { GenreColour.fromStored(it) }

    fun setOrder(id: CategoryId, order: GenreOrder) = db.transaction {
        db.categoryQueries.setEpisodeOrder(Columns.genreOrder(order), clock.issue().value, id.value)
        if (order == GenreOrder.OLDEST_FIRST) db.feedFetchStateQueries.forgetValidatorsInCategory(id.value)
    }
}
