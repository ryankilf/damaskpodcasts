package uk.co.kilfeder.damask.podcasts.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.PlayedBy
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.STOP_TIMEOUT_MS
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toKey
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toRow

data class HomeUiState(
    val loading: Boolean = true,
    /** False until the first subscription: Home shows how to add podcasts instead of empty lists. */
    val hasSubscriptions: Boolean = false,
    val inProgress: List<EpisodeRowUi> = emptyList(),
    val upNext: List<EpisodeRowUi> = emptyList(),
    /** True when Up Next may have more to show. */
    val canLoadMore: Boolean = false,
    val refreshing: Boolean = false,
)

/** Home: In Progress, then Up Next (spec › Screens). */
class HomeViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    private val upNextSize = MutableStateFlow(PAGE_SIZE)
    private val refreshing = MutableStateFlow(false)

    val uiState: StateFlow<HomeUiState> = combine(
        graph.library,
        graph.rotation,
        upNextSize,
        refreshing,
    ) { library, rotation, size, refreshing ->
        val now = graph.wallClockMs()
        val upNext = rotation.upNext().take(size + 1).toList()
        HomeUiState(
            loading = false,
            hasSubscriptions = library.subscribedShows.isNotEmpty(),
            inProgress = rotation.inProgress().map { it.toRow(library, rotation, now) },
            upNext = upNext.take(size).map { it.toRow(library, rotation, now) },
            canLoadMore = upNext.size > size,
            refreshing = refreshing,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), HomeUiState())

    fun onLoadMore() = upNextSize.update { it + PAGE_SIZE }

    /** Swipe to mark played: the episode leaves every queue; undo from Played. */
    fun onMarkPlayed(id: EpisodeId) {
        viewModelScope.launch { graph.onDb { graph.userState.markPlayed(id.toKey(), PlayedBy.MARKED) } }
    }

    fun onRefresh() {
        if (refreshing.value) return
        refreshing.value = true
        viewModelScope.launch {
            try {
                graph.refreshFeeds()
            } finally {
                refreshing.value = false
            }
        }
    }

    private companion object {
        const val PAGE_SIZE = 30
    }
}
