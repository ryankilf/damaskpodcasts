package uk.co.kilfeder.damask.podcasts.core.feed

import uk.co.kilfeder.damask.podcasts.core.model.CategoryId

/**
 * Apple's top-level podcast categories: the starting set of categories, and the keys of
 * `apple_category_map`. Feeds write these as fixed English keys whatever the show's language; the
 * key is only used to look up a prefill and is never displayed (spec › Categories and language).
 */
object AppleCategories {
    val TOP_LEVEL: List<String> = listOf(
        "Arts", "Business", "Comedy", "Education", "Fiction", "Government", "Health & Fitness",
        "History", "Kids & Family", "Leisure", "Music", "News", "Religion & Spirituality", "Science",
        "Society & Culture", "Sports", "Technology", "True Crime", "TV & Film",
    )

    /** Pre-2019 and commonly misspelled names, keyed by [key]. */
    private val LEGACY: Map<String, String> = mapOf(
        "gamesandhobbies" to "Leisure",
        "scienceandmedicine" to "Science",
        "health" to "Health & Fitness",
        "healthandwellness" to "Health & Fitness",
        "governmentandorganizations" to "Government",
        "governmentandorganisations" to "Government",
        "newsandpolitics" to "News",
        "politics" to "News",
        "sportsandrecreation" to "Sports",
        "tvandfilm" to "TV & Film",
        "tvfilm" to "TV & Film",
        "film" to "TV & Film",
        "tech" to "Technology",
        "kidsfamily" to "Kids & Family",
        "religion" to "Religion & Spirituality",
        "spirituality" to "Religion & Spirituality",
    )

    private val BY_KEY: Map<String, String> = TOP_LEVEL.associateBy(::key) + LEGACY

    /** The stable ID of a seeded category, so every device seeds the same rows. */
    fun categoryId(appleName: String): CategoryId = CategoryId("apple:" + key(appleName))

    /** The canonical top-level name for whatever the feed wrote, or null if unrecognised. */
    fun normalise(raw: String): String? = BY_KEY[key(raw)]

    /** Case-, punctuation- and `&`/`and`-insensitive: "Health &amp; Fitness" == "health and fitness". */
    private fun key(name: String): String =
        name.lowercase().replace("&", "and").filter { it.isLetterOrDigit() }
}
