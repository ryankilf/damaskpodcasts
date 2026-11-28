package uk.co.kilfeder.damask.podcasts.presentation.genres

import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.GenreColour
import uk.co.kilfeder.damask.podcasts.core.model.Show
import uk.co.kilfeder.damask.podcasts.core.rotation.Library

/**
 * A genre's colour for the UI (design hand-off › GenrePalette): the 16 hues, plus [GREY] for
 * Uncategorised and a genre with no colour yet. A hint beside the genre's name, never on its own.
 */
enum class GenreHue {
    RED, ORANGE, AMBER, YELLOW, LIME, GREEN, TEAL, CYAN,
    BLUE, INDIGO, VIOLET, PURPLE, MAGENTA, PINK, BROWN, SLATE,
    GREY,
}

internal fun GenreColour?.toHue(): GenreHue = this?.let { GenreHue.valueOf(it.name) } ?: GenreHue.GREY

internal fun GenreHue.toColour(): GenreColour? = GenreColour.entries.firstOrNull { it.name == name }

/** A genre's colour. */
internal fun Library.hueOf(id: CategoryId): GenreHue = category(id)?.colour.toHue()

/** The colour of the genre a show is in. */
internal fun Library.hueOf(show: Show): GenreHue = categoryOf(show)?.colour.toHue()
