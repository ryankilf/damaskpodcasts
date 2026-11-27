package uk.co.kilfeder.damask.podcasts.presentation.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Destination
import uk.co.kilfeder.damask.podcasts.presentation.navigation.NavState
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Tab

/** Which part of the app is showing. */
enum class Stage {
    LOADING,

    /** Self-declared 18+ (spec › Compliance) hasn't been given yet. */
    AGE_GATE,

    /** No subscriptions yet: first run goes straight to Import. */
    WELCOME,
    MAIN,
}

data class AppUiState(
    val stage: Stage = Stage.LOADING,
    val nav: NavState = NavState(),
    val useWallpaperColours: Boolean = true,
)

/** Top-level state: age gate, first run, and navigation. */
class AppViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    private val nav = MutableStateFlow(NavState())

    val uiState: StateFlow<AppUiState> = combine(graph.settings.values, graph.library, nav) { settings, library, nav ->
        val stage = when {
            !settings.ageConfirmed -> Stage.AGE_GATE
            library.subscribedShows.isEmpty() -> Stage.WELCOME
            else -> Stage.MAIN
        }
        AppUiState(stage, nav, settings.useWallpaperColours)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AppUiState())

    fun onConfirmAge() {
        viewModelScope.launch { graph.onDb { graph.settings.confirmAge() } }
    }

    fun onSelectTab(tab: Tab) = nav.update { it.select(tab) }

    fun onNavigate(destination: Destination) = nav.update { it.push(destination) }

    /** Returns false when there was nothing to go back to, so the platform can handle back. */
    fun onBack(): Boolean {
        val popped = nav.value.pop() ?: return false
        nav.value = popped
        return true
    }
}

internal const val STOP_TIMEOUT_MS = 5_000L
