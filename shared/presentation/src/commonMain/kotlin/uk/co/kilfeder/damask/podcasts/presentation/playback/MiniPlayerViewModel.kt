package uk.co.kilfeder.damask.podcasts.presentation.playback

import kotlinx.coroutines.flow.map
import uk.co.kilfeder.damask.podcasts.presentation.format.fraction
import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback

/** The mini-player above the tabs (design hand-off › MiniPlayer). */
data class MiniPlayerUi(
    val id: EpisodeId,
    val title: String,
    val showTitle: String,
    val artworkUrl: String?,
    val fallback: ArtworkFallback,
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
    /** Everything comes from what's playing; no library lookup needed. */
    val uiState: StateFlow<MiniPlayerUiState> = graph.playback.state.map { now ->
        MiniPlayerUiState(
            now?.let {
                MiniPlayerUi(
                    id = it.id,
                    title = it.title,
                    showTitle = it.showTitle,
                    artworkUrl = it.artworkUrl,
                    fallback = ArtworkFallback.of(it.id.showId, it.showTitle),
                    isPlaying = it.isPlaying,
                    buffering = it.buffering,
                    progress = fraction(it.positionMs, it.durationMs),
                )
            },
        )
    }.stateInUi(viewModelScope, MiniPlayerUiState())

    fun onPlayPause() = graph.playback.togglePlayPause()
}
