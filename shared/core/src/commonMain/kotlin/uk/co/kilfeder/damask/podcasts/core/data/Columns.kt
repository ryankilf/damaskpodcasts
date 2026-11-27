package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.model.EpisodeOrder
import uk.co.kilfeder.damask.podcasts.core.model.KeepMode
import uk.co.kilfeder.damask.podcasts.core.model.PlayedBy

/** Encodings of enum-like TEXT/INTEGER columns. Unknown values decode to null ("automatic"). */
internal object Columns {
    fun episodeOrder(raw: String?): EpisodeOrder? = when (raw) {
        "NEWEST" -> EpisodeOrder.NEWEST_FIRST
        "OLDEST" -> EpisodeOrder.OLDEST_FIRST
        else -> null
    }

    fun episodeOrder(value: EpisodeOrder?): String? = when (value) {
        EpisodeOrder.NEWEST_FIRST -> "NEWEST"
        EpisodeOrder.OLDEST_FIRST -> "OLDEST"
        null -> null
    }

    fun keepMode(raw: String?): KeepMode? = KeepMode.entries.firstOrNull { it.name == raw }

    fun playedBy(raw: String?): PlayedBy? = PlayedBy.entries.firstOrNull { it.name == raw }

    fun bool(raw: Long): Boolean = raw != 0L

    fun bool(value: Boolean): Long = if (value) 1L else 0L
}
