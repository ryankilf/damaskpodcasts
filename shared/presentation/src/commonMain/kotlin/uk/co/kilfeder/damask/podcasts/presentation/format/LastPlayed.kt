package uk.co.kilfeder.damask.podcasts.presentation.format

import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation

/** "Last played …" for a genre; null = never played. */
internal fun Rotation.lastPlayed(id: CategoryId, nowMs: Long): Summarised? =
    history.lastListened(id)?.let { Formats.lastPlayed(it.wallMs, nowMs) }

/** "Last played …" for a show; null = never played. */
internal fun Rotation.lastPlayed(id: ShowId, nowMs: Long): Summarised? =
    history.lastListened(id)?.let { Formats.lastPlayed(it.wallMs, nowMs) }
