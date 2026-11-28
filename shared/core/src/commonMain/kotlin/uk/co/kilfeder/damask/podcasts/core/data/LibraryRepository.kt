package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.model.GenreColour
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.model.Category
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.model.ShowType
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.sync.ShowAliases
import uk.co.kilfeder.damask.podcasts.core.db.Category as CategoryRow

/**
 * Builds the rotation's [Library] snapshot from the database.
 *
 * Every read goes through canonical(show_id): alias shows are dropped (their root's settings
 * apply) and their episodes are re-keyed to the root. If both copies of a feed have the same
 * episode, the two are merged, with position and played state each taking the newer write.
 */
class LibraryRepository(private val db: DamaskDatabase) {

    fun load(): Library {
        val aliasRows = db.showAliasQueries.selectAll().executeAsList()
        val aliases = ShowAliases.flatten(aliasRows.associate { ShowId(it.alias_id) to ShowId(it.canonical_id) })
        val feeds = db.feedQueries.selectShowFeeds().executeAsList().associateBy { it.show_id }
        val shows = db.showQueries.selectAll().executeAsList()
            .filter { ShowId(it.id) !in aliases }
            .map { row ->
                val feed = feeds[row.id]
                row.toModel(ShowType.fromItunesType(feed?.itunes_type))
                    .copy(
                        title = feed?.title.orEmpty(),
                        imageUrl = feed?.image_url,
                        author = feed?.author.orEmpty(),
                        description = feed?.description.orEmpty(),
                    )
            }
        val episodes = db.libraryQueries.selectEpisodes(::episodeRow).executeAsList()
            .map { it.canonical(aliases) }
            .groupBy { it.key }
            .values
            .map { copies -> copies.reduce(::merge) }
        val categories = db.categoryQueries.selectLive().executeAsList().map { it.toModel() }
        return Library(categories, shows, episodes)
    }

    private fun Episode.canonical(aliases: Map<ShowId, ShowId>): Episode =
        copy(key = key.copy(showId = ShowAliases.canonical(key.showId, aliases)))

    private fun merge(a: Episode, b: Episode): Episode = a.copy(
        feedDurationMs = a.feedDurationMs ?: b.feedDurationMs,
        playerDurationMs = a.playerDurationMs ?: b.playerDurationMs,
        isDownloaded = a.isDownloaded || b.isDownloaded,
        title = a.title.ifEmpty { b.title },
        imageUrl = a.imageUrl ?: b.imageUrl,
        position = listOfNotNull(a.position, b.position).maxByOrNull { it.hlc },
        played = listOfNotNull(a.played, b.played).maxByOrNull { it.hlc },
    )

    private fun CategoryRow.toModel() =
        Category(
            id = CategoryId(id),
            name = name,
            isSystem = Columns.bool(is_system),
            order = Columns.genreOrder(episode_order),
            colour = GenreColour.fromStored(colour),
        )
}
