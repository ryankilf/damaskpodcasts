package uk.co.kilfeder.damask.podcasts.presentation.episodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.PlayedBy
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.STOP_TIMEOUT_MS

/** The episode page: the row's details, whether it's played, and its show notes. */
data class EpisodeUiState(
    val loading: Boolean = true,
    /** null once loaded means the episode no longer exists (e.g. pruned and never played). */
    val episode: EpisodeRowUi? = null,
    val isPlayed: Boolean = false,
    /** Show notes as styled blocks ([ShowNotes]); empty when the feed gave none. */
    val notes: List<NotesBlock> = emptyList(),
)

class EpisodeViewModel internal constructor(
    private val graph: AppGraph,
    private val id: EpisodeId,
) : ViewModel() {
    val uiState: StateFlow<EpisodeUiState> = combine(graph.library, graph.rotation) { library, rotation ->
        val key = id.toKey()
        val episode = library.episodesOf(key.showId).firstOrNull { it.key == key }
        EpisodeUiState(
            loading = false,
            episode = episode?.toRow(library, rotation, graph.wallClockMs()),
            isPlayed = episode?.isPlayed == true,
            notes = if (episode == null) emptyList() else ShowNotes.parse(graph.onDb { graph.notes.of(key) }),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EpisodeUiState())

    fun onMarkPlayed() {
        viewModelScope.launch { graph.onDb { graph.userState.markPlayed(id.toKey(), PlayedBy.MARKED) } }
    }

    /** Also resets the position, per the spec. */
    fun onMarkUnplayed() {
        viewModelScope.launch { graph.onDb { graph.userState.markUnplayed(id.toKey()) } }
    }
}
