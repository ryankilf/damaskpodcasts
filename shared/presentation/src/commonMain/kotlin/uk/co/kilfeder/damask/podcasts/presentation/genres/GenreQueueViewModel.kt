package uk.co.kilfeder.damask.podcasts.presentation.genres

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
    /** The queue's in-progress episodes (it starts with them), shown in their own panel. */
    val inProgress: List<EpisodeRowUi> = emptyList(),
    /** The rest of the queue: one episode per show, round-robin. */
    val upNext: List<EpisodeRowUi> = emptyList(),
    val canLoadMore: Boolean = false,
)

/** One category's queue: in progress first, then shows by LRP, round-robin. */
class GenreQueueViewModel internal constructor(
    private val graph: AppGraph,
    categoryId: String,
) : ViewModel() {
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
            showCount = library.showsByCategory[id].orEmpty().size,
            lastListened = rotation.lastPlayed(id, now),
            inProgress = inProgress,
            upNext = upNext,
            canLoadMore = more,
        )
    }.stateInUi(viewModelScope, GenreQueueUiState())

    fun onLoadMore() = size.update { it + PAGE_SIZE }

    fun onMarkPlayed(id: EpisodeId) {
        viewModelScope.launch { graph.markPlayed(id) }
    }
}
