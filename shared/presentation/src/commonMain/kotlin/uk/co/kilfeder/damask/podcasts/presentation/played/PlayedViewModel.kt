package uk.co.kilfeder.damask.podcasts.presentation.played

import kotlinx.coroutines.flow.map
import uk.co.kilfeder.damask.podcasts.presentation.episodes.markUnplayed
import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toRow

data class PlayedRowUi(
    val episode: EpisodeRowUi,
    val isPlayed: Boolean,
)

data class PlayedUiState(
    val loading: Boolean = true,
    val rows: List<PlayedRowUi> = emptyList(),
)

/** Played: every episode with activity, newest first; supports undoing a mark-played. */
class PlayedViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    val uiState: StateFlow<PlayedUiState> = graph.rotation.map { rotation ->
        val library = rotation.library
        val now = graph.wallClockMs()
        PlayedUiState(
            loading = false,
            rows = rotation.played().map { episode ->
                PlayedRowUi(episode.toRow(library, rotation, now), episode.isPlayed)
            },
        )
    }.stateInUi(viewModelScope, PlayedUiState())

    /** Undo an accidental mark-played; also resets the position, per the spec. */
    fun onMarkUnplayed(id: EpisodeId) {
        viewModelScope.launch { graph.markUnplayed(id) }
    }
}
