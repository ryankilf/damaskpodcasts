package uk.co.kilfeder.damask.podcasts.presentation.played

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.laterOf
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.STOP_TIMEOUT_MS
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toKey
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toRow
import uk.co.kilfeder.damask.podcasts.presentation.format.Formats
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised

data class PlayedRowUi(
    val episode: EpisodeRowUi,
    val isPlayed: Boolean,
    /** Most recent activity: "Last played …". */
    val lastActivity: Summarised,
)

data class PlayedUiState(
    val loading: Boolean = true,
    val rows: List<PlayedRowUi> = emptyList(),
)

/** Played: every episode with activity, newest first; supports undoing a mark-played. */
class PlayedViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    val uiState: StateFlow<PlayedUiState> = combine(graph.library, graph.rotation) { library, rotation ->
        val now = graph.wallClockMs()
        PlayedUiState(
            loading = false,
            rows = rotation.played().map { episode ->
                val last = laterOf(episode.position?.hlc, episode.played?.hlc)?.wallMs ?: episode.publishedAt
                PlayedRowUi(episode.toRow(library, rotation, now), episode.isPlayed, Formats.date(last, now))
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlayedUiState())

    /** Undo an accidental mark-played; also resets the position, per the spec. */
    fun onMarkUnplayed(id: EpisodeId) {
        viewModelScope.launch { graph.onDb { graph.userState.markUnplayed(id.toKey()) } }
    }
}
