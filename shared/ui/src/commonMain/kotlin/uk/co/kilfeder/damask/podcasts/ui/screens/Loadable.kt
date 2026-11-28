package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import uk.co.kilfeder.damask.podcasts.ui.components.FadeThrough

/** Which of a [Loadable]'s slots is showing. */
private enum class LoadPhase { LOADING, FAILED, READY }

/**
 * Anything that loads: [loading] (a spinner) while [isLoading], then [content], or [failed] with
 * [problem] if there is one. It fades between the three (spec › Motion), so finishing loading
 * crossfades rather than snaps; changes within [content] redraw in place, without a fade.
 *
 * The slots fill the area, so give their roots `Modifier.fillMaxSize()`, not the caller's modifier.
 */
@Composable
internal fun Loadable(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    problem: String? = null,
    failed: @Composable (problem: String) -> Unit = { EmptyMessage(it) },
    loading: @Composable () -> Unit = { Loading() },
    content: @Composable () -> Unit,
) {
    val phase = when {
        isLoading -> LoadPhase.LOADING
        problem != null -> LoadPhase.FAILED
        else -> LoadPhase.READY
    }
    FadeThrough(phase, screenOf = { it }, modifier = modifier.fillMaxSize()) {
        when (it) {
            LoadPhase.LOADING -> loading()
            LoadPhase.FAILED -> failed(problem.orEmpty())
            LoadPhase.READY -> content()
        }
    }
}
