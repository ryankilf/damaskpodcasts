package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.feed.AppleCategories
import uk.co.kilfeder.damask.podcasts.core.feed.FeedDocument
import uk.co.kilfeder.damask.podcasts.core.model.Category
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId

/**
 * The starting categories and the import prefill (spec › Core concepts › Assignment): each show's
 * suggestion comes from mapping the feed's first `itunes:category` and subcategory through
 * `apple_category_map`. The user confirms or changes every one.
 */
class CategoryPrefill(private val db: DamaskDatabase) {

    /**
     * Seeds Apple's top-level categories and their `apple_category_map` rows. Idempotent: seeded
     * rows have fixed IDs and HLC 0, so they never overwrite a user's rename, merge or delete.
     */
    fun seedAppleCategories() = db.transaction {
        AppleCategories.TOP_LEVEL.forEach { name ->
            val id = AppleCategories.categoryId(name).value
            db.categoryQueries.upsert(id = id, name = name, is_system = 0, deleted = 0, hlc = 0)
            db.appleCategoryMapQueries.upsert(name, "", id, deleted = 0, hlc = 0)
        }
    }

    /**
     * The import prefill: the feed's first `itunes:category` (normalised from legacy or misspelled
     * values) and subcategory, looked up in `apple_category_map`. Uncategorised if nothing maps.
     */
    /**
     * The genre for Apple's [genres] (main one first: a category such as "History", or a
     * subcategory such as "Daily News" followed by its parent "News"), through the same
     * Apple-category mapping as a feed's own category. null when none of them is one of Apple's.
     */
    fun categoryForAppleGenres(genres: List<String>): CategoryId? {
        val main = genres.firstOrNull() ?: return null
        val top = AppleCategories.normalise(main)
        val (apple, subcategory) = if (top != null) {
            top to ""
        } else {
            val parent = genres.drop(1).firstNotNullOfOrNull { AppleCategories.normalise(it) } ?: return null
            parent to main
        }
        val mapped = db.appleCategoryMapQueries.selectPrefill(apple, subcategory).executeAsOneOrNull()
        return mapped?.let(::CategoryId)
    }

    fun prefillCategory(feed: FeedDocument?): CategoryId {
        val first = feed?.categories?.firstOrNull() ?: return Category.UNCATEGORISED_ID
        val apple = AppleCategories.normalise(first.category) ?: return Category.UNCATEGORISED_ID
        val mapped = db.appleCategoryMapQueries.selectPrefill(apple, first.subcategory).executeAsOneOrNull()
        return mapped?.let(::CategoryId) ?: Category.UNCATEGORISED_ID
    }
}
