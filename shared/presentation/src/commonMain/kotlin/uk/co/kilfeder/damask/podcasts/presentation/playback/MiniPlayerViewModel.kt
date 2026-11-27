package uk.co.kilfeder.damask.podcasts.presentation.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.STOP_TIMEOUT_MS
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toKey

/** The mini-player above the tabs (design hand-off › MiniPlayer). */
data class MiniPlayerUi(
    val id: EpisodeId,
    val title: String,
    val showTitle: String,
    val artworkUrl: String?,
    val isPlaying: Boolean,
    val buffering: Boolean,
    /** 0..1; null when the length isn't known. */
    val progress: Float?,
)

data class MiniPlayerUiState(
    /** null = nothing loaded: no mini-player. */
    val player: MiniPlayerUi? = null,
)

class MiniPlayerViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    val uiState: StateFlow<MiniPlayerUiState> = combine(graph.playback.state, graph.library) { now, library ->
        val episode = now?.let { library.episodesOf(it.id.toKey().showId).firstOrNull { e -> e.key == it.id.toKey() } }
        if (now == null || episode == null) {
            MiniPlayerUiState()
        } else {
            MiniPlayerUiState(
                MiniPlayerUi(
                    id = now.id,
                    title = episode.title,
                    showTitle = library.showsById[episode.key.showId]?.title.orEmpty(),
                    artworkUrl = episode.imageUrl,
                    isPlaying = now.isPlaying,
                    buffering = now.buffering,
                    progress = now.durationMs?.takeIf { it > 0 }
                        ?.let { (now.positionMs.toFloat() / it).coerceIn(0f, 1f) },
                ),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MiniPlayerUiState())

    fun onPlayPause() = graph.playback.togglePlayPause()
}
