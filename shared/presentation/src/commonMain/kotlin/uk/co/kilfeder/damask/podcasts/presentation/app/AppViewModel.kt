package uk.co.kilfeder.damask.podcasts.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Destination
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.presentation.playback.toContext
import uk.co.kilfeder.damask.podcasts.presentation.navigation.NavState
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Tab

/** Which part of the app is showing. */
enum class Stage {
    LOADING,
    MAIN,
}

/** One-off messages the UI shows once (as a snackbar) and then reports shown. */
sealed interface AppMessage {
    data class ShowsAdded(val count: Int) : AppMessage
}

data class AppUiState(
    val stage: Stage = Stage.LOADING,
    val nav: NavState = NavState(),
    val useWallpaperColours: Boolean = false,
    /** Show once, then call [AppViewModel.onMessageShown]. */
    val message: AppMessage? = null,
    /** The episode loaded in the player, if any (wide layouts show it in a pane). */
    val playing: EpisodeId? = null,
    /** It's playing rather than paused (rows show pause for it). */
    val playingNow: Boolean = false,
)

/** Top-level state: navigation and appearance. Every launch opens on Home. */
class AppViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    private val nav = MutableStateFlow(NavState())
    private val message = MutableStateFlow<AppMessage?>(null)

    val uiState: StateFlow<AppUiState> = combine(
        graph.settings.values,
        nav,
        message,
        graph.playback.state.map { it?.id to (it?.isPlaying == true) }.distinctUntilChanged(),
    ) { settings, nav, message, (playing, playingNow) ->
        AppUiState(Stage.MAIN, nav, settings.useWallpaperColours, message, playing, playingNow)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppUiState())

    fun onSelectTab(tab: Tab) = nav.update { it.select(tab) }

    /** An import finished: go to Home, where the new shows are, and say how many were added. */
    fun onImportFinished(count: Int) {
        nav.value = NavState(tab = Tab.HOME)
        message.value = AppMessage.ShowsAdded(count)
    }

    fun onMessageShown() {
        message.value = null
    }

    fun onNavigate(destination: Destination) = nav.update { it.push(destination) }

    /**
     * Tapping an episode plays it, and opens its screen when [open] (narrow layouts). Wide layouts
     * show the playing episode in a pane beside the list instead.
     */
    fun onPlayEpisode(id: EpisodeId, source: PlaySource, open: Boolean) {
        graph.playback.play(id, source.toContext())
        val episode = Destination.Episode(id)
        if (open && nav.value.top != episode) nav.update { it.push(episode) }
    }

    /** A row's own play button: pauses or resumes the playing episode, or plays this one, in place. */
    fun onRowPlayPause(id: EpisodeId, source: PlaySource) {
        val playback = graph.playback
        if (playback.state.value?.id == id) playback.togglePlayPause() else playback.play(id, source.toContext())
    }

    /** Opens a show's page; one already showing (beside the list, on wide layouts) is replaced. */
    fun onOpenShow(showId: String) = nav.update { state ->
        val page = Destination.Show(showId)
        if (state.top is Destination.Show) state.pop()?.push(page) ?: state.push(page) else state.push(page)
    }

    /** Opens a genre's queue; one already showing (beside the list, on wide layouts) is replaced. */
    fun onOpenGenre(categoryId: String) = nav.update { state ->
        val queue = Destination.GenreQueue(categoryId)
        if (state.top is Destination.GenreQueue) state.pop()?.push(queue) ?: state.push(queue) else state.push(queue)
    }

    /** Returns false when there was nothing to go back to, so the platform can handle back. */
    fun onBack(): Boolean {
        val popped = nav.value.pop() ?: return false
        nav.value = popped
        return true
    }
}

internal const val STOP_TIMEOUT_MS = 5_000L
