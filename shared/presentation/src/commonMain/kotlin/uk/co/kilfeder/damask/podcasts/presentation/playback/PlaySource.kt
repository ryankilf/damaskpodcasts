package uk.co.kilfeder.damask.podcasts.presentation.playback

import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext

/** Where an episode was started from; decides what autoplays after it (spec › Autoplay). */
sealed interface PlaySource {
    data object UpNext : PlaySource

    /** Resuming from In Progress or the episode screen: keeps the context saved with it. */
    data object Resume : PlaySource

    data class Genre(val categoryId: String) : PlaySource

    /** Played (and later Search): playback stops at the end. */
    data object Played : PlaySource
}

/** null = use the context saved with the episode. */
internal fun PlaySource.toContext(): PlayContext? = when (this) {
    PlaySource.UpNext -> PlayContext.UpNext
    PlaySource.Resume -> null
    is PlaySource.Genre -> PlayContext.Genre(CategoryId(categoryId))
    PlaySource.Played -> PlayContext.None
}
