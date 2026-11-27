package uk.co.kilfeder.damask.podcasts.presentation

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import uk.co.kilfeder.damask.podcasts.core.Greeting

data class GreetingUiState(
    val isGreetingVisible: Boolean = false,
    val greeting: String = "",
)

class GreetingViewModel : ViewModel() {
    private val greeting = Greeting()

    private val _uiState = MutableStateFlow(GreetingUiState(greeting = greeting.greet()))
    val uiState: StateFlow<GreetingUiState> = _uiState.asStateFlow()

    fun onToggleGreeting() {
        _uiState.update { it.copy(isGreetingVisible = !it.isGreetingVisible) }
    }
}
