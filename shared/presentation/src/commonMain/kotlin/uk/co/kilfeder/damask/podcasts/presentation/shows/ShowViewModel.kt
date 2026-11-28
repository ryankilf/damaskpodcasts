package uk.co.kilfeder.damask.podcasts.presentation.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.STOP_TIMEOUT_MS
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toRow

data class ShowUiState(
    val loading: Boolean = true,
    /** null once loaded = the show isn't in the library any more. */
    val show: ShowRowUi? = null,
    /** Every episode the app has, in the show's own order (oldest first for serials). */
    val episodes: List<EpisodeRowUi> = emptyList(),
)

/**
 * A show's page: its episodes in the show's own order. Playing from here uses the show's context,
 * so autoplay carries on through the show (spec › Autoplay).
 */
class ShowViewModel internal constructor(graph: AppGraph, showId: String) : ViewModel() {
    private val id = ShowId(showId)

    val uiState: StateFlow<ShowUiState> = combine(graph.library, graph.rotation) { library, rotation ->
        val now = graph.wallClockMs()
        val show = library.showsById[id]
        ShowUiState(
            loading = false,
            show = show?.toRow(library, rotation, now),
            // Every row is this show: no genre chip.
            episodes = rotation.showOrder(id).map { it.toRow(library, rotation, now).copy(genre = null) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ShowUiState())
}
