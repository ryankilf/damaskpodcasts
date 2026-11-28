package uk.co.kilfeder.damask.podcasts.presentation.episodes

import uk.co.kilfeder.damask.podcasts.core.model.EpisodeOrder
import uk.co.kilfeder.damask.podcasts.core.model.GenreOrder

/**
 * The order a list of episodes is in, which is also the order they play in. A show offers newest
 * and oldest first; a genre offers variety (the rotation) too.
 */
enum class ListOrder { VARIETY, NEWEST_FIRST, OLDEST_FIRST }

internal fun EpisodeOrder.toListOrder() = when (this) {
    EpisodeOrder.NEWEST_FIRST -> ListOrder.NEWEST_FIRST
    EpisodeOrder.OLDEST_FIRST -> ListOrder.OLDEST_FIRST
}

/** A show has no variety order; it means newest first there. */
internal fun ListOrder.toEpisodeOrder() =
    if (this == ListOrder.OLDEST_FIRST) EpisodeOrder.OLDEST_FIRST else EpisodeOrder.NEWEST_FIRST

internal fun GenreOrder.toListOrder() = when (this) {
    GenreOrder.VARIETY -> ListOrder.VARIETY
    GenreOrder.NEWEST_FIRST -> ListOrder.NEWEST_FIRST
    GenreOrder.OLDEST_FIRST -> ListOrder.OLDEST_FIRST
}

internal fun ListOrder.toGenreOrder() = when (this) {
    ListOrder.VARIETY -> GenreOrder.VARIETY
    ListOrder.NEWEST_FIRST -> GenreOrder.NEWEST_FIRST
    ListOrder.OLDEST_FIRST -> GenreOrder.OLDEST_FIRST
}
