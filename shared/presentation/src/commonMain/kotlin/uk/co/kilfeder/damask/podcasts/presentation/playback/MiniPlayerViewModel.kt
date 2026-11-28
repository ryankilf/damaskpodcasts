package uk.co.kilfeder.damask.podcasts.presentation.playback

import uk.co.kilfeder.damask.podcasts.presentation.genres.hueOf
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import kotlinx.coroutines.flow.combine
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
    /** What's playing, with its show's genre colour for the artwork placeholder. */
    val uiState: StateFlow<MiniPlayerUiState> = combine(graph.playback.state, graph.rotation) { now, rotation ->
        val library = rotation.library
        MiniPlayerUiState(
            now?.let {
                MiniPlayerUi(
                    id = it.id,
                    title = it.title,
                    showTitle = it.showTitle,
                    artworkUrl = it.artworkUrl,
                    fallback = ArtworkFallback.of(
                        it.showTitle,
                        library.showsById[ShowId(it.id.showId)]?.let(library::hueOf) ?: GenreHue.GREY,
                    ),
                    isPlaying = it.isPlaying,
                    buffering = it.buffering,
                    progress = fraction(it.positionMs, it.durationMs),
                )
            },
        )
    }.stateInUi(viewModelScope, MiniPlayerUiState())

    fun onPlayPause() = graph.playback.togglePlayPause()
}
