package uk.co.kilfeder.damask.podcasts.presentation.shows

import uk.co.kilfeder.damask.podcasts.presentation.genres.toColour
import uk.co.kilfeder.damask.podcasts.presentation.genres.toHue
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue
import uk.co.kilfeder.damask.podcasts.presentation.genres.hueOf
import uk.co.kilfeder.damask.podcasts.presentation.episodes.NextUpUi
import uk.co.kilfeder.damask.podcasts.presentation.app.refreshShows
import uk.co.kilfeder.damask.podcasts.presentation.episodes.nextUpOf
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toEpisodeOrder
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toListOrder
import uk.co.kilfeder.damask.podcasts.presentation.episodes.ListOrder
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import uk.co.kilfeder.damask.podcasts.presentation.format.lastPlayed
import uk.co.kilfeder.damask.podcasts.presentation.genres.categoryOptions
import uk.co.kilfeder.damask.podcasts.presentation.genres.nextHue
import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.Show
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.discover.PreviewState
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toRow
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import uk.co.kilfeder.damask.podcasts.presentation.genres.CategoryOptionUi

data class ShowUiState(
    val loading: Boolean = true,
    /** null once loaded = the show isn't in the library any more. */
    val show: ShowPageUi? = null,
    /** Every episode the app has, in the show's order; each row's first line is its date. */
    val episodes: List<EpisodeRowUi> = emptyList(),
    val order: ListOrder = ListOrder.NEWEST_FIRST,
    /** What the big button plays; highlighted in the list. null = everything's played. */
    val nextUp: NextUpUi? = null,
    /** Oldest first: open the list here (the first unplayed episode), played ones above it. */
    val startAt: Int? = null,
    /** Every genre, for the genre sheet. */
    val genres: List<CategoryOptionUi> = emptyList(),
    /** The colour a genre created from the sheet starts with: the least used. */
    val nextHue: GenreHue = GenreHue.RED,
    /** Opened from Discover and its feed couldn't be read: why. */
    val problem: String? = null,
)

/**
 * A show's page (design hand-off › Show): details, the button that resumes or plays it, its
 * description, and every episode in its order. That one order is also the order the show plays
 * in, so the list always matches what comes next. Playing from here uses the show's context.
 */
class ShowViewModel internal constructor(private val graph: AppGraph, showId: String) : ViewModel() {
    private val id = ShowId(showId)

    val uiState: StateFlow<ShowUiState> = combine(graph.rotation, graph.previews.state) { rotation, previews ->
        val library = rotation.library
        val show = library.showsById[id] ?: return@combine when (val preview = previews[id]) {
            // Opened from Discover: its feed is still being read.
            PreviewState.Loading -> ShowUiState(loading = true)
            is PreviewState.Failed -> ShowUiState(loading = false, problem = preview.reason)
            null -> ShowUiState(loading = false)
        }
        state(show, library, rotation)
    }.stateInUi(viewModelScope, ShowUiState())

    private fun state(show: Show, library: Library, rotation: Rotation): ShowUiState {
        val now = graph.wallClockMs()
        val ordered = rotation.showOrder(id)
        val rows = ordered.map { episode ->
            val row = episode.toRow(library, rotation, now)
            // Every row is this show, so the date takes the show name's place.
            row.copy(genre = null, showTitle = row.published.summary)
        }
        val next = ordered.filter { rotation.isInProgress(it) }.maxByOrNull { it.position?.hlc?.value ?: 0 }
            ?: ordered.firstOrNull { !it.isPlayed }
        val order = show.episodeOrder.toListOrder()
        return ShowUiState(
            loading = false,
            show = page(show, library, rotation, now),
            episodes = rows,
            order = order,
            nextUp = next?.let { episode ->
                nextUpOf(episode, rows.first { it.id.guid == episode.key.guid }, rotation)
            },
            startAt = if (order == ListOrder.OLDEST_FIRST) rows.indexOfFirst { !it.played }.takeIf { it > 0 } else null,
            genres = library.categoryOptions(),
            nextHue = library.nextHue(),
        )
    }

    private fun page(show: Show, library: Library, rotation: Rotation, now: Long): ShowPageUi {
        val episodes = library.episodesOf(id)
        val category = library.categoryOf(show)
        return ShowPageUi(
            showId = id.value,
            title = show.title,
            author = show.author,
            artworkUrl = show.imageUrl,
            fallback = ArtworkFallback.of(show.title, library.hueOf(show)),
            subscribed = show.subscribed,
            genreId = (category?.id ?: show.categoryId).value,
            genre = category?.name,
            cadence = ShowFacts.cadence(episodes),
            usualDay = ShowFacts.usualDay(episodes),
            lastPlayed = rotation.lastPlayed(id, now),
            description = ShowFacts.plainText(show.description),
            feedUrl = show.feedUrl,
        )
    }

    /** The genre chip: move the show to another genre. */
    fun onGenreChange(genreId: String) = write {
        graph.showSettings.setCategory(id, CategoryId(genreId))
        // A genre gets its colour once it has a show.
        graph.genreSettings.assignMissingColours()
    }

    /** "Create “<name>”" in the genre sheet: makes the genre and moves the show into it. */
    fun onCreateGenre(name: String, hue: GenreHue) {
        val colour = hue.toColour() ?: return
        if (name.isBlank()) return
        write { graph.showSettings.setCategory(id, graph.genreSettings.create(name, colour)) }
    }

    /** The order chip: the list's order, and the order the show plays in. */
    fun onOrderChange(order: ListOrder) {
        viewModelScope.launch {
            graph.onDb { graph.showSettings.setEpisodeOrder(id, order.toEpisodeOrder()) }
            // Oldest first keeps every episode: fetch the feed in full now to bring older ones back.
            if (order == ListOrder.OLDEST_FIRST) graph.refreshShows(listOf(id))
        }
    }

    /**
     * Subscribe: a show you've just unsubscribed from, or one opened from Discover. It keeps the
     * genre it has (for Discover, Apple's genre for it).
     */
    fun onSubscribe() = write {
        graph.showSettings.setSubscribed(id, true)
        // A genre gets its colour once it has a show.
        graph.genreSettings.assignMissingColours()
    }

    private fun write(block: () -> Unit) {
        viewModelScope.launch { graph.onDb(block) }
    }
}
