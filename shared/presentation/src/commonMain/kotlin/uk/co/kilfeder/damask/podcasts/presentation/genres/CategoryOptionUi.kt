package uk.co.kilfeder.damask.podcasts.presentation.genres

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
