package uk.co.kilfeder.damask.podcasts.core.rotation

import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.Category
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.Show
import uk.co.kilfeder.damask.podcasts.core.model.ShowId

/**
 * A read-time snapshot of everything the rotation needs.
 *
 * Callers resolve show aliases first: every [Show.id] and [Episode.key] show ID must already be
 * canonical (see `ShowAliases`), and tombstoned categories must be left out of [categories].
 */
class Library(
    categories: List<Category>,
    shows: List<Show>,
    episodes: List<Episode>,
) {
    init {
        require(categories.count { it.isSystem } <= 1) { "At most one system (Uncategorised) category" }
    }

    val categories: List<Category> = categories
    val episodes: List<Episode> = episodes

    private val categoryIds: Set<CategoryId> = categories.mapTo(mutableSetOf()) { it.id }
    private val uncategorised: CategoryId? = categories.firstOrNull { it.isSystem }?.id

    /** All shows, subscribed or not: Played outlives a subscription. */
    val showsById: Map<ShowId, Show> = shows.associateBy { it.id }

    val subscribedShows: List<Show> = shows.filter { it.subscribed }

    val episodesByShow: Map<ShowId, List<Episode>> = episodes.groupBy { it.key.showId }

    /** Subscribed shows per effective category. */
    val showsByCategory: Map<CategoryId, List<Show>> =
        subscribedShows.mapNotNull { show -> effectiveCategory(show)?.let { it to show } }
            .groupBy({ it.first }, { it.second })

    /**
     * A show's category, or Uncategorised when its category is missing or tombstoned (for example
     * deleted concurrently on another device).
     */
    fun effectiveCategory(show: Show): CategoryId? =
        if (show.categoryId in categoryIds) show.categoryId else uncategorised

    fun episodesOf(showId: ShowId): List<Episode> = episodesByShow[showId].orEmpty()

    private val categoriesById: Map<CategoryId, Category> = categories.associateBy { it.id }

    fun category(id: CategoryId): Category? = categoriesById[id]

    /** The genre a show is in (Uncategorised when its own is gone). */
    fun categoryOf(show: Show): Category? = effectiveCategory(show)?.let(::category)

    fun episode(key: EpisodeKey): Episode? = episodesOf(key.showId).firstOrNull { it.key == key }
}
