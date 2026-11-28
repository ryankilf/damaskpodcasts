package uk.co.kilfeder.damask.podcasts.presentation.genres

import uk.co.kilfeder.damask.podcasts.core.rotation.Library

/** A genre to choose in the genre sheet (Import's review, a show's page). */
data class CategoryOptionUi(val id: String, val name: String)

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
    .map { CategoryOptionUi(it.id.value, it.name) }

/** The name of the genre with [id], or "" if it's gone. */
fun List<CategoryOptionUi>.nameOf(id: String): String = firstOrNull { it.id == id }?.name.orEmpty()
