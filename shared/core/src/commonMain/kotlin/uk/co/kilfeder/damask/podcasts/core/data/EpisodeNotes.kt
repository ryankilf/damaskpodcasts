package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey

/**
 * Show notes, read one episode at a time for the episode page. They're kept out of the library
 * snapshot, which loads every episode and never needs them.
 */
class EpisodeNotes(private val db: DamaskDatabase) {
    /** The notes as the feed wrote them; null when it has none or has left its feed. */
    fun of(key: EpisodeKey): String? =
        db.feedQueries.selectNotes(key.showId.value, key.guid).executeAsOneOrNull()?.notes_html
}
