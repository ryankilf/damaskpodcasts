package uk.co.kilfeder.damask.podcasts.presentation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GreetingViewModelTest {

    @Test
    fun toggleFlipsGreetingVisibility() {
        val viewModel = GreetingViewModel()
        assertFalse(viewModel.uiState.value.isGreetingVisible)

        viewModel.onToggleGreeting()
        assertTrue(viewModel.uiState.value.isGreetingVisible)

        viewModel.onToggleGreeting()
        assertFalse(viewModel.uiState.value.isGreetingVisible)
    }
}
