package uk.co.kilfeder.damask.podcasts.presentation.genres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.PlayedBy
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.STOP_TIMEOUT_MS
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toKey
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toRow
import uk.co.kilfeder.damask.podcasts.presentation.format.Formats
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
        graph.library,
        graph.rotation,
        size,
    ) { library, rotation, size ->
        val now = graph.wallClockMs()
        val queue = rotation.genreQueue(id).take(size + 1).toList()
        // Every row is this genre: no chip.
        val rows = queue.take(size).map { it.toRow(library, rotation, now).copy(genre = null) }
        val (inProgress, upNext) = rows.partition { it.progress != null }
        GenreQueueUiState(
            loading = false,
            name = library.categories.firstOrNull { it.id == id }?.name.orEmpty(),
            showCount = library.showsByCategory[id].orEmpty().size,
            lastListened = rotation.history.lastListened(id)?.let { Formats.lastPlayed(it.wallMs, now) },
            inProgress = inProgress,
            upNext = upNext,
            canLoadMore = queue.size > size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GenreQueueUiState())

    fun onLoadMore() = size.update { it + PAGE_SIZE }

    fun onMarkPlayed(id: EpisodeId) {
        graph.playback.onMarkedPlayed(id)
        viewModelScope.launch { graph.onDb { graph.userState.markPlayed(id.toKey(), PlayedBy.MARKED) } }
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}
