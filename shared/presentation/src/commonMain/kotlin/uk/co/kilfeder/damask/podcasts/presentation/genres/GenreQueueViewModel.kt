package uk.co.kilfeder.damask.podcasts.presentation.genres

import uk.co.kilfeder.damask.podcasts.presentation.genres.hueOf
import kotlinx.coroutines.flow.first
import uk.co.kilfeder.damask.podcasts.presentation.app.refreshShows
import uk.co.kilfeder.damask.podcasts.core.model.GenreOrder
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toGenreOrder
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toListOrder
import uk.co.kilfeder.damask.podcasts.presentation.episodes.nextUpOf
import uk.co.kilfeder.damask.podcasts.presentation.episodes.NextUpUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.ListOrder
import uk.co.kilfeder.damask.podcasts.presentation.format.lastPlayed
import uk.co.kilfeder.damask.podcasts.presentation.episodes.markPlayed
import uk.co.kilfeder.damask.podcasts.presentation.app.PAGE_SIZE
import uk.co.kilfeder.damask.podcasts.presentation.app.page
import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toRow
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised

data class GenreQueueUiState(
    val loading: Boolean = true,
    val name: String = "",
    val showCount: Int = 0,
    /** null = never listened. */
    val lastListened: Summarised? = null,
    /** The shows next in the queue, for the stack at the top. */
    val covers: List<CoverUi> = emptyList(),
    /** What the big button plays: the start of the queue (resuming, if it's in progress). */
    val nextUp: NextUpUi? = null,
    val order: ListOrder = ListOrder.VARIETY,
    /** The genre's colour: the bar by its name and its rows' stripes. */
    val hue: GenreHue = GenreHue.GREY,
    /** Uncategorised: no colour, name or delete to change (no ⋮ menu). */
    val isSystem: Boolean = false,
    /** The queue's in-progress episodes (it starts with them), shown in their own panel. */
    val inProgress: List<EpisodeRowUi> = emptyList(),
    /** The rest of the queue, in the genre's [order]. */
    val upNext: List<EpisodeRowUi> = emptyList(),
    val canLoadMore: Boolean = false,
)

/** One category's queue: in progress first, then shows by LRP, round-robin. */
class GenreQueueViewModel internal constructor(
    private val graph: AppGraph,
    categoryId: String,
) : ViewModel(), GenreEdits by GenreEditor(graph) {
    private val id = CategoryId(categoryId)
    private val size = MutableStateFlow(PAGE_SIZE)

    val uiState: StateFlow<GenreQueueUiState> = combine(
        graph.rotation,
        size,
    ) { rotation, size ->
        val library = rotation.library
        val now = graph.wallClockMs()
        val (queue, more) = rotation.genreQueue(id).page(size)
        // Every row is this genre: no chip.
        val rows = queue.map { it.toRow(library, rotation, now).copy(genre = null) }
        val (inProgress, upNext) = rows.partition { it.progress != null }
        GenreQueueUiState(
            loading = false,
            name = library.category(id)?.name.orEmpty(),
            showCount = library.showsIn(id).size,
            covers = rotation.covers(id),
            nextUp = queue.firstOrNull()?.let { nextUpOf(it, rows.first(), rotation) },
            order = (library.category(id)?.order ?: GenreOrder.VARIETY).toListOrder(),
            hue = library.hueOf(id),
            isSystem = library.category(id)?.isSystem ?: false,
            lastListened = rotation.lastPlayed(id, now),
            inProgress = inProgress,
            upNext = upNext,
            canLoadMore = more,
        )
    }.stateInUi(viewModelScope, GenreQueueUiState())

    fun onLoadMore() = size.update { it + PAGE_SIZE }

    /** The order chip: the queue's order, which is also what autoplay plays next in this genre. */
    fun onOrderChange(order: ListOrder) {
        viewModelScope.launch {
            graph.onDb { graph.genreSettings.setOrder(id, order.toGenreOrder()) }
            // Oldest first keeps every episode: fetch the genre's feeds in full now.
            if (order == ListOrder.OLDEST_FIRST) graph.refreshShows(currentShows())
        }
    }

    /** "Find more comedy" on an empty genre: Discover opens searching for the genre's name. */
    fun onFindMore() {
        graph.discoverSeed.value = uiState.value.name
    }

    private suspend fun currentShows() = graph.rotation.first().library.showsIn(id).map { it.id }

    fun onMarkPlayed(id: EpisodeId) {
        viewModelScope.launch { graph.markPlayed(id) }
    }
}
