package uk.co.kilfeder.damask.podcasts.presentation.episodes

import uk.co.kilfeder.damask.podcasts.presentation.format.fraction
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
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
    /** Initials on a colour, behind the artwork and in its place when there isn't any. */
    val fallback: ArtworkFallback,
    val published: Summarised,
    /** Length, or time left once started; null when unknown. */
    val length: Summarised?,
    /** 0..1 while in progress, else null. */
    val progress: Float?,
    /** Played and not being re-listened: shown dimmed, with a replay button. */
    val played: Boolean = false,
)

internal fun EpisodeId.toKey() = EpisodeKey(ShowId(showId), guid)

/** One string per episode: list keys, media IDs. */
val EpisodeId.key: String get() = "$showId/$guid"

internal fun Episode.toRow(library: Library, rotation: Rotation, nowMs: Long): EpisodeRowUi {
    val inProgress = rotation.isInProgress(this)
    val position = position?.positionMs?.takeIf { inProgress }
    val total = durationMs
    val show = library.showsById[key.showId]
    val showTitle = show?.title.orEmpty()
    return EpisodeRowUi(
        id = EpisodeId(key.showId.value, key.guid),
        title = title,
        showTitle = showTitle,
        genre = show?.let(library::categoryOf)?.name,
        artworkUrl = imageUrl,
        fallback = ArtworkFallback.of(key.showId.value, showTitle),
        published = Formats.date(publishedAt, nowMs),
        length = Formats.length(total, position),
        progress = fraction(position, total),
        played = isPlayed && !inProgress,
    )
}

