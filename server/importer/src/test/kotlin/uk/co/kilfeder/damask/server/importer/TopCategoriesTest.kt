package uk.co.kilfeder.damask.server.importer

import uk.co.kilfeder.damask.podcasts.api.SearchCategories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TopCategoriesTest {
    /** Every distinct `categories` value in the 21 Nov 2026 dump (107), as stored in the index. */
    private val dumpValues = listOf(
        "society", "culture", "education", "business", "religion", "spirituality", "arts", "health",
        "fitness", "news", "music", "comedy", "self improvement", "sports", "christianity", "leisure",
        "tv", "film", "personal", "journals", "entrepreneurship", "mental", "books", "technology",
        "science", "history", "kids", "family", "fiction", "politics", "interviews", "relationships",
        "games", "commentary", "careers", "hobbies", "marketing", "management", "performing", "investing",
        "government", "documentary", "philosophy", "medicine", "places", "how to", "travel", "courses",
        "entertainment", "true crime", "reviews", "improv", "alternative", "video games", "parenting",
        "islam", "non-profit", "drama", "language", "food", "visual", "learning", "daily", "fashion",
        "soccer", "beauty", "social", "football", "design", "sexuality", "stories", "nutrition", "nature",
        "basketball", "pets", "automotive", "judaism", "life", "animation", "animals", "manga", "stand-up",
        "home", "natural", "garden", "wrestling", "after-shows", "baseball", "wilderness", "fantasy",
        "hockey", "buddhism", "earth", "running", "golf", "astronomy", "hinduism", "aviation", "crafts",
        "physics", "cricket", "rugby", "chemistry", "mathematics", "tennis", "volleyball", "swimming",
    )

    @Test
    fun everyValueInTheDumpIsMapped() {
        assertEquals(107, dumpValues.size)
        val unmapped = dumpValues.filterNot(TopCategories::isMapped)
        assertTrue(unmapped.isEmpty(), "unmapped: $unmapped")
    }

    @Test
    fun onlyEverProducesTheSharedKeys() {
        val produced = TopCategories.of(dumpValues).toSet()
        assertEquals(SearchCategories.KEYS.toSet(), produced, "all 19, and nothing else")
    }

    @Test
    fun subcategoriesGoToTheirParent() {
        assertEquals(listOf("news"), TopCategories.of(listOf("Daily", "Politics", "News")))
        assertEquals(listOf("comedy"), TopCategories.of(listOf("improv", "stand-up")))
        assertEquals(listOf("tvandfilm"), TopCategories.of(listOf("tv", "film", "after-shows")))
        assertEquals(listOf("societyandculture"), TopCategories.of(listOf("society", "culture", "documentary")))
        assertEquals(listOf("kidsandfamily", "healthandfitness"), TopCategories.of(listOf("parenting", "fitness")))
    }

    @Test
    fun legacyNamesMatchTheApp() {
        // The app's AppleCategories.LEGACY table, applied to full feed category names.
        mapOf(
            "Games & Hobbies" to "leisure",
            "Science & Medicine" to "science",
            "Health & Wellness" to "healthandfitness",
            "Government & Organizations" to "government",
            "News & Politics" to "news",
            "Sports & Recreation" to "sports",
            "TV & Film" to "tvandfilm",
            "Tech" to "technology",
            "Religion" to "religionandspirituality",
            "Health & Fitness" to "healthandfitness",
            "Society & Culture" to "societyandculture",
        ).forEach { (raw, key) -> assertEquals(listOf(key), TopCategories.of(listOf(raw)), raw) }
    }

    @Test
    fun unknownValuesAreDropped() {
        assertEquals(emptyList(), TopCategories.of(listOf("podcasts", "")))
    }
}
