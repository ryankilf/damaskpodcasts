package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowOrder

/** What a show page can ask of the app (navigation and app-wide messages live above it). */
internal class ShowPageActions(
    /** Opens an episode's screen (it doesn't play). */
    val onOpenEpisode: (EpisodeId, PlaySource) -> Unit,
    val onUnsubscribe: (showId: String, title: String) -> Unit,
    /** null beside the Shows list on wide layouts, where there's nothing to go back to. */
    val onBack: (() -> Unit)?,
)

/** What the show page's own controls change (handled by its ViewModel). */
internal class ShowIntents(
    val onGenreChange: (String) -> Unit,
    val onOrderChange: (ShowOrder) -> Unit,
    val onSubscribe: () -> Unit,
)
