package uk.co.kilfeder.damask.podcasts.core.rotation

import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.Hlc
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.rules.ListeningRules

/**
 * The LRP primitive (spec › Core concepts): for any group of episodes, `lastListened(group)` is the
 * latest time any episode in it passed the listen threshold. null = never listened, which sorts first.
 */
class ListenHistory(library: Library, rules: ListeningRules) {
    private val byShow: Map<ShowId, Hlc?> = library.subscribedShows.associate { show ->
        show.id to library.episodesOf(show.id).mapNotNull { rules.listenedAt(it) }.maxOrNull()
    }

    private val byCategory: Map<CategoryId, Hlc?> = library.showsByCategory.mapValues { (_, shows) ->
        shows.mapNotNull { byShow[it.id] }.maxOrNull()
    }

    /** lastListened(category), over its subscribed shows. */
    fun lastListened(categoryId: CategoryId): Hlc? = byCategory[categoryId]

    /** lastListened(show). */
    fun lastListened(showId: ShowId): Hlc? = byShow[showId]
}
