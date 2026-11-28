package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey

/** Where an episode's audio is: a downloaded file, else the feed's enclosure URL. */
data class EpisodeMedia(val url: String, val localPath: String?)

/**
 * Per-episode details read one episode at a time: show notes for the episode screen, and the
 * audio's address for playback. They're kept out of the library snapshot, which loads every
 * episode and never needs them.
 */
class EpisodeLookup(private val db: DamaskDatabase) {
    /** The notes as the feed wrote them; null when it has none or has left its feed. */
    fun notes(key: EpisodeKey): String? =
        db.feedQueries.selectNotes(key.showId.value, key.guid).executeAsOneOrNull()?.notes_html

    /** null when the episode has left its feed (only its Played record remains). */
    fun media(key: EpisodeKey): EpisodeMedia? =
        db.feedQueries.selectMedia(key.showId.value, key.guid).executeAsOneOrNull()
            ?.let { EpisodeMedia(it.enclosure_url, it.download_path) }
}
