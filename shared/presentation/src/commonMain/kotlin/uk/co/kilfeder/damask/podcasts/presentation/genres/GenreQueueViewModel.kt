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

data class GenreQueueUiState(
    val loading: Boolean = true,
    val name: String = "",
    val episodes: List<EpisodeRowUi> = emptyList(),
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
        GenreQueueUiState(
            loading = false,
            name = library.categories.firstOrNull { it.id == id }?.name.orEmpty(),
            episodes = queue.take(size).map { it.toRow(library, rotation, now) },
            canLoadMore = queue.size > size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GenreQueueUiState())

    fun onLoadMore() = size.update { it + PAGE_SIZE }

    fun onMarkPlayed(id: EpisodeId) {
        viewModelScope.launch { graph.onDb { graph.userState.markPlayed(id.toKey(), PlayedBy.MARKED) } }
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}
