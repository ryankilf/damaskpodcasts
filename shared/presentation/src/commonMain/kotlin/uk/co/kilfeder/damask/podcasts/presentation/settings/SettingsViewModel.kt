package uk.co.kilfeder.damask.podcasts.presentation.settings

import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph

data class SettingsUiState(
    val hideExplicit: Boolean = false,
    val useWallpaperColours: Boolean = false,
)

class SettingsViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = graph.settings.values
        .map { SettingsUiState(it.hideExplicit, it.useWallpaperColours) }
        .stateInUi(viewModelScope, SettingsUiState())

    fun onHideExplicitChanged(value: Boolean) {
        viewModelScope.launch { graph.onDb { graph.settings.setHideExplicit(value) } }
    }

    fun onUseWallpaperColoursChanged(value: Boolean) {
        viewModelScope.launch { graph.onDb { graph.settings.setUseWallpaperColours(value) } }
    }
}
