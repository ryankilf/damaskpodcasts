package uk.co.kilfeder.damask.podcasts.presentation.app

import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
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

    /** Offers Undo ([AppViewModel.onUndoUnsubscribe]). [title] is blank for a show never read. */
    data class Unsubscribed(val showId: String, val title: String) : AppMessage
}

data class AppUiState(
    val stage: Stage = Stage.LOADING,
    val nav: NavState = NavState(),
    val useWallpaperColours: Boolean = false,
    /** Light, dark, or follow the system. */
    val themeMode: ThemeMode = ThemeMode.AUTOMATIC,
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
        AppUiState(
            stage = Stage.MAIN,
            nav = nav,
            useWallpaperColours = settings.useWallpaperColours,
            themeMode = settings.themeMode,
            message = message,
            playing = playing,
            playingNow = playingNow,
        )
    }.stateInUi(viewModelScope, AppUiState())

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
     * Tapping an episode opens its screen; it doesn't play (spec › Screens). Play buttons play in
     * place without opening anything ([onRowPlayPause]). Another episode already open is replaced.
     */
    fun onOpenEpisode(id: EpisodeId, source: PlaySource?) =
        nav.update { it.replaceTop(Destination.Episode(id, source)) }

    /** A row's own play button: pauses or resumes the playing episode, or plays this one, in place. */
    fun onRowPlayPause(id: EpisodeId, source: PlaySource) {
        graph.playback.playOrToggle(id, source.toContext())
    }

    /**
     * Opens a show's page in Shows, from anywhere (an episode's screen, Discover), so the
     * navigation follows; in Shows, one already open beside the list is replaced.
     */
    fun onOpenShow(showId: String) = nav.update { it.openIn(Tab.SHOWS, Destination.Show(showId)) }

    /** Opens a genre's queue in Genres, from anywhere; in Genres, one already open is replaced. */
    fun onOpenGenre(categoryId: String) = nav.update { it.openIn(Tab.GENRES, Destination.GenreQueue(categoryId)) }

    /**
     * From the show page's menu: unsubscribes, leaves the page, and offers Undo. The show stays
     * in Played; its episodes leave every queue.
     */
    fun onUnsubscribe(showId: String, title: String) {
        viewModelScope.launch { graph.onDb { graph.showSettings.setSubscribed(ShowId(showId), false) } }
        if (nav.value.top == Destination.Show(showId)) onBack()
        message.value = AppMessage.Unsubscribed(showId, title)
    }

    fun onUndoUnsubscribe(showId: String) {
        viewModelScope.launch { graph.onDb { graph.showSettings.setSubscribed(ShowId(showId), true) } }
    }

    /** Returns false when there was nothing to go back to, so the platform can handle back. */
    fun onBack(): Boolean {
        val popped = nav.value.pop() ?: return false
        nav.value = popped
        return true
    }
}
