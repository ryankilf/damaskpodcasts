package uk.co.kilfeder.damask.server.importer

import uk.co.kilfeder.damask.podcasts.api.SearchCategories

/**
 * Maps the catalogue's raw categories to Apple's 19 top-level categories (the `category` filter's
 * keys, [SearchCategories.KEYS]). Podcast Index stores each show's categories as single lowercase
 * words, splitting Apple's multi-word names ("society", "culture"; "tv", "film") and listing
 * subcategories as words of their own ("daily", "politics", "improv"). Every value seen in the
 * 21 Nov 2026 dump (107 of them) is mapped below; TopCategoriesTest checks that.
 *
 * Because names arrive split, "Science Fiction" lands in both Science and Fiction, and "Comedy
 * Fiction" in both Comedy and Fiction: the words can't be told apart.
 */
object TopCategories {
    private val TABLE: Map<String, String> = buildMap {
        fun map(top: String, vararg words: String) = words.forEach { put(key(it), top) }

        map("arts", "arts", "books", "design", "fashion", "beauty", "food", "performing", "visual")
        map(
            "business", "business", "careers", "entrepreneurship", "investing", "management", "marketing",
            "non-profit",
        )
        map("comedy", "comedy", "interviews", "improv", "stand-up")
        map("education", "education", "courses", "how to", "language", "learning", "self improvement")
        map("fiction", "fiction", "drama")
        map("government", "government")
        map(
            "healthandfitness", "health", "fitness", "alternative", "medicine", "mental", "nutrition",
            "sexuality",
        )
        map("history", "history")
        map("kidsandfamily", "kids", "family", "parenting", "pets", "animals", "stories")
        map(
            "leisure", "leisure", "animation", "manga", "automotive", "aviation", "crafts", "games",
            "hobbies", "home", "garden", "video games",
        )
        map("music", "music")
        map("news", "news", "daily", "politics", "commentary", "entertainment")
        map(
            "religionandspirituality", "religion", "spirituality", "buddhism", "christianity", "hinduism",
            "islam", "judaism",
        )
        map(
            "science", "science", "astronomy", "chemistry", "earth", "life", "mathematics", "natural",
            "nature", "physics", "social",
        )
        map(
            "societyandculture", "society", "culture", "documentary", "personal", "journals",
            "philosophy", "places", "travel", "relationships",
        )
        map(
            "sports", "sports", "baseball", "basketball", "cricket", "fantasy", "football", "golf",
            "hockey", "rugby", "running", "soccer", "swimming", "tennis", "volleyball", "wilderness",
            "wrestling",
        )
        map("technology", "technology")
        map("truecrime", "true crime")
        map("tvandfilm", "tv", "film", "after-shows", "reviews")

        // Legacy (pre-2019) and misspelled names, matching the app's AppleCategories.LEGACY.
        map("leisure", "games & hobbies")
        map("science", "science & medicine")
        map("healthandfitness", "health & wellness")
        map("government", "government & organizations", "government & organisations")
        map("news", "news & politics")
        map("sports", "sports & recreation")
        map("tvandfilm", "tv & film", "tvfilm")
        map("technology", "tech")
        map("kidsandfamily", "kidsfamily")
        // Full current names too, should a feed or a future dump write them unsplit.
        SearchCategories.KEYS.forEach { put(it, it) }
    }

    /** The top-level keys for a show's raw categories, de-duplicated, in first-seen order. */
    fun of(raw: List<String>): List<String> = raw.mapNotNull { TABLE[key(it)] }.distinct()

    /** Whether a raw value is mapped (for tests and for logging unmapped values). */
    fun isMapped(raw: String): Boolean = key(raw) in TABLE

    /** The app's key function: lowercase, `&` as `and`, letters and digits only. */
    fun key(name: String): String = name.lowercase().replace("&", "and").filter { it.isLetterOrDigit() }
}
