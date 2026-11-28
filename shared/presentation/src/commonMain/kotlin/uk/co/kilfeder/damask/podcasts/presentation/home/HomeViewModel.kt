package uk.co.kilfeder.damask.podcasts.presentation.home

import uk.co.kilfeder.damask.podcasts.presentation.app.PAGE_SIZE
import uk.co.kilfeder.damask.podcasts.presentation.episodes.markPlayed
import uk.co.kilfeder.damask.podcasts.presentation.app.page
import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.refreshFeeds
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
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
        graph.rotation,
        upNextSize,
        refreshing,
    ) { rotation, size, refreshing ->
        val library = rotation.library
        val now = graph.wallClockMs()
        val (upNext, more) = rotation.upNext().page(size)
        HomeUiState(
            loading = false,
            hasSubscriptions = library.subscribedShows.isNotEmpty(),
            inProgress = rotation.inProgress().map { it.toRow(library, rotation, now) },
            upNext = upNext.map { it.toRow(library, rotation, now) },
            canLoadMore = more,
            refreshing = refreshing,
        )
    }.stateInUi(viewModelScope, HomeUiState())

    fun onLoadMore() = upNextSize.update { it + PAGE_SIZE }

    /** Swipe to mark played: the episode leaves every queue; undo from Played. */
    fun onMarkPlayed(id: EpisodeId) {
        viewModelScope.launch { graph.markPlayed(id) }
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

}
