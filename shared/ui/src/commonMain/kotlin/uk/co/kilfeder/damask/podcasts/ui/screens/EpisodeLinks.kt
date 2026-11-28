package uk.co.kilfeder.damask.podcasts.ui.screens

/**
 * Where an episode's screen links to: its show's page (the show's name) and its genre (the
 * chip). Each opens in its own section (Shows, Genres), so the navigation follows.
 */
internal class EpisodeLinks(val onOpenShow: (showId: String) -> Unit, val onOpenGenre: (genreId: String) -> Unit)
