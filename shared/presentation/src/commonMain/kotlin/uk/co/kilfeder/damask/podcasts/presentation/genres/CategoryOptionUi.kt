package uk.co.kilfeder.damask.podcasts.presentation.genres

import uk.co.kilfeder.damask.podcasts.core.model.GenreColour
import uk.co.kilfeder.damask.podcasts.core.rotation.Library

/** A genre to choose in the genre sheet (Import's review, a show's page). */
data class CategoryOptionUi(
    val id: String,
    val name: String,
    /** The genre's colour, for the square beside its name. */
    val hue: GenreHue = GenreHue.GREY,
)

/**
 * The categories matching a search: case-insensitive, and "&" matches "and" both ways, so
 * "health and" finds "Health & Fitness". An empty query matches everything.
 */
fun List<CategoryOptionUi>.matching(query: String): List<CategoryOptionUi> {
    val wanted = searchKey(query)
    return if (wanted.isEmpty()) this else filter { searchKey(it.name).contains(wanted) }
}

private fun searchKey(text: String): String =
    text.lowercase().replace("&", " and ").split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

/** Every genre, as the genre sheet lists them everywhere: A–Z, with Uncategorised last. */
internal fun Library.categoryOptions(): List<CategoryOptionUi> = categories
    .sortedWith(compareBy({ it.isSystem }, { it.name.lowercase() }))
    .map { CategoryOptionUi(it.id.value, it.name, it.colour.toHue()) }

/** The colour of the genre with [id] (grey if it's gone or has none). */
fun List<CategoryOptionUi>.hueOf(id: String): GenreHue = firstOrNull { it.id == id }?.hue ?: GenreHue.GREY

/** The name of the genre with [id], or "" if it's gone. */
fun List<CategoryOptionUi>.nameOf(id: String): String = firstOrNull { it.id == id }?.name.orEmpty()

/** The colour a new genre gets: the least used (Manage genres' New genre, the genre sheet's Create). */
internal fun Library.nextHue(): GenreHue = GenreColour.leastUsed(categories.mapNotNull { it.colour }).toHue()
