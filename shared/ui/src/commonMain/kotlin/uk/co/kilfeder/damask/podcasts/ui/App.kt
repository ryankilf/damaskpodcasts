package uk.co.kilfeder.damask.podcasts.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.GreetingUiState
import uk.co.kilfeder.damask.podcasts.presentation.GreetingViewModel
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.compose_multiplatform
import uk.co.kilfeder.damask.podcasts.ui.resources.greeting_message
import uk.co.kilfeder.damask.podcasts.ui.resources.greeting_toggle
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTheme

/** Top-level screen: the only place a ViewModel is obtained. Children get state + callbacks. */
@Composable
fun App(
    modifier: Modifier = Modifier,
    viewModel: GreetingViewModel = viewModel { GreetingViewModel() },
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    DamaskTheme {
        GreetingScreen(
            uiState = uiState,
            onToggleGreeting = viewModel::onToggleGreeting,
            modifier = modifier,
        )
    }
}

@Composable
private fun GreetingScreen(
    uiState: GreetingUiState,
    onToggleGreeting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.primaryContainer)
            .safeContentPadding()
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Button(onClick = onToggleGreeting) {
            Text(stringResource(Res.string.greeting_toggle))
        }
        AnimatedVisibility(uiState.isGreetingVisible) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Image(
                    painter = painterResource(Res.drawable.compose_multiplatform),
                    contentDescription = null,
                )
                Text(
                    text = stringResource(Res.string.greeting_message, uiState.greeting),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

@Preview
@Composable
private fun GreetingScreenPreview() {
    DamaskTheme {
        GreetingScreen(
            uiState = GreetingUiState(isGreetingVisible = true, greeting = "Hello, Preview!"),
            onToggleGreeting = {},
        )
    }
}
