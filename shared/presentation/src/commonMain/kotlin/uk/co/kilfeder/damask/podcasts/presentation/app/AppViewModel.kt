package uk.co.kilfeder.damask.podcasts.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Destination
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
)

/** Top-level state: navigation and appearance. Every launch opens on Home. */
class AppViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    private val nav = MutableStateFlow(NavState())
    private val message = MutableStateFlow<AppMessage?>(null)

    val uiState: StateFlow<AppUiState> = combine(graph.settings.values, nav, message) { settings, nav, message ->
        AppUiState(Stage.MAIN, nav, settings.useWallpaperColours, message)
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

    /** Returns false when there was nothing to go back to, so the platform can handle back. */
    fun onBack(): Boolean {
        val popped = nav.value.pop() ?: return false
        nav.value = popped
        return true
    }
}

internal const val STOP_TIMEOUT_MS = 5_000L
