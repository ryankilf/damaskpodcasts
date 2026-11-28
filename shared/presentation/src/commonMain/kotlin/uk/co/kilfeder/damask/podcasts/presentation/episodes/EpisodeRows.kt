package uk.co.kilfeder.damask.podcasts.presentation.episodes

import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.presentation.format.Formats
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised

/** An episode's identity, without exposing core types to the UI. */
data class EpisodeId(val showId: String, val guid: String)

/** One episode row (design hand-off › EpisodeRow). Every summarised value carries its exact one. */
data class EpisodeRowUi(
    val id: EpisodeId,
    val title: String,
    val showTitle: String,
    /** The genre, for the chip above the title; null where every row shares it (a genre's queue). */
    val genre: String?,
    val artworkUrl: String?,
    val published: Summarised,
    /** Length, or time left once started; null when unknown. */
    val length: Summarised?,
    /** 0..1 while in progress, else null. */
    val progress: Float?,
)

internal fun EpisodeId.toKey() = EpisodeKey(ShowId(showId), guid)

internal fun Episode.toRow(library: Library, rotation: Rotation, nowMs: Long): EpisodeRowUi {
    val inProgress = rotation.isInProgress(this)
    val position = position?.positionMs?.takeIf { inProgress }
    val total = durationMs
    return EpisodeRowUi(
        id = EpisodeId(key.showId.value, key.guid),
        title = title,
        showTitle = library.showsById[key.showId]?.title.orEmpty(),
        genre = library.showsById[key.showId]
            ?.let(library::effectiveCategory)
            ?.let { id -> library.categories.firstOrNull { it.id == id }?.name },
        artworkUrl = imageUrl,
        published = Formats.date(publishedAt, nowMs),
        length = Formats.length(total, position),
        progress = progress(position, total),
    )
}

private fun progress(positionMs: Long?, totalMs: Long?): Float? {
    if (positionMs == null || totalMs == null || totalMs <= 0) return null
    return (positionMs.toFloat() / totalMs).coerceIn(0f, 1f)
}
