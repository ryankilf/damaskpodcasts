package uk.co.kilfeder.damask.podcasts.presentation.episodes

import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised

/**
 * The big button on a show's or a genre's page: resume the episode in progress, else play the next unplayed one in
 * the list's order. It names no episode (feed titles are too unreliable to put in a button);
 * [title] is for the accessibility label and long press.
 */
data class NextUpUi(
    val id: EpisodeId,
    val resume: Boolean,
    val title: String,
    /** "32 min left" or "47 min"; null when the length isn't known. */
    val length: Summarised?,
)

/** The button's episode, from its row (already formatted). */
internal fun nextUpOf(episode: Episode, row: EpisodeRowUi, rotation: Rotation) =
    NextUpUi(row.id, resume = rotation.isInProgress(episode), title = row.title, length = row.length)
