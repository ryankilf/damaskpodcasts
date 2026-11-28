package uk.co.kilfeder.damask.podcasts.presentation.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.Show
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.STOP_TIMEOUT_MS
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toRow
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import uk.co.kilfeder.damask.podcasts.presentation.format.Formats
import uk.co.kilfeder.damask.podcasts.presentation.genres.CategoryOptionUi

data class ShowUiState(
    val loading: Boolean = true,
    /** null once loaded = the show isn't in the library any more. */
    val show: ShowPageUi? = null,
    /** Every episode the app has, in the show's order; each row's first line is its date. */
    val episodes: List<EpisodeRowUi> = emptyList(),
    val order: ShowOrder = ShowOrder.NEWEST_FIRST,
    /** What the big button plays; highlighted in the list. null = everything's played. */
    val nextUp: NextUpUi? = null,
    /** Oldest first: open the list here (the first unplayed episode), played ones above it. */
    val startAt: Int? = null,
    /** Every genre, for the genre sheet. */
    val genres: List<CategoryOptionUi> = emptyList(),
)

/**
 * A show's page (design hand-off › Show): details, the button that resumes or plays it, its
 * description, and every episode in its order. That one order is also the order the show plays
 * in, so the list always matches what comes next. Playing from here uses the show's context.
 */
class ShowViewModel internal constructor(private val graph: AppGraph, showId: String) : ViewModel() {
    private val id = ShowId(showId)

    val uiState: StateFlow<ShowUiState> = combine(graph.library, graph.rotation) { library, rotation ->
        val show = library.showsById[id] ?: return@combine ShowUiState(loading = false)
        state(show, library, rotation)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ShowUiState())

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
        val order = ShowFacts.order(show.episodeOrder)
        return ShowUiState(
            loading = false,
            show = page(show, library, rotation, now),
            episodes = rows,
            order = order,
            nextUp = next?.let { episode ->
                val row = rows.first { it.id.guid == episode.key.guid }
                NextUpUi(row.id, resume = rotation.isInProgress(episode), title = row.title, length = row.length)
            },
            startAt = if (order == ShowOrder.OLDEST_FIRST) rows.indexOfFirst { !it.played }.takeIf { it > 0 } else null,
            genres = library.categories.map { CategoryOptionUi(it.id.value, it.name) },
        )
    }

    private fun page(show: Show, library: Library, rotation: Rotation, now: Long): ShowPageUi {
        val episodes = library.episodesOf(id)
        val categoryId = library.effectiveCategory(show)
        return ShowPageUi(
            showId = id.value,
            title = show.title,
            author = show.author,
            artworkUrl = show.imageUrl,
            fallback = ArtworkFallback.of(id.value, show.title),
            subscribed = show.subscribed,
            genreId = (categoryId ?: show.categoryId).value,
            genre = library.categories.firstOrNull { it.id == categoryId }?.name,
            cadence = ShowFacts.cadence(episodes),
            usualDay = ShowFacts.usualDay(episodes),
            episodeCount = episodes.size,
            since = ShowFacts.since(episodes),
            lastPlayed = rotation.history.lastListened(id)?.let { Formats.lastPlayed(it.wallMs, now) },
            description = ShowFacts.plainText(show.description),
            feedUrl = show.feedUrl,
        )
    }

    /** The genre chip: move the show to another genre. */
    fun onGenreChange(genreId: String) = write { graph.showSettings.setCategory(id, CategoryId(genreId)) }

    /** The order chip: the list's order, and the order the show plays in. */
    fun onOrderChange(order: ShowOrder) = write { graph.showSettings.setEpisodeOrder(id, ShowFacts.order(order)) }

    /** Subscribe again (a show you've just unsubscribed from), in the genre it had. */
    fun onSubscribe() = write { graph.showSettings.setSubscribed(id, true) }

    private fun write(block: () -> Unit) {
        viewModelScope.launch { graph.onDb(block) }
    }
}
