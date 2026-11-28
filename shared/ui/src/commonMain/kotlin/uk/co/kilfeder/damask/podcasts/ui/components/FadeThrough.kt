package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import uk.co.kilfeder.damask.podcasts.ui.theme.Motion

/**
 * Fades from one screen to the next whenever [screenOf] of [state] changes (spec › Motion). Other
 * changes to [state] redraw the current screen in place. [content] must draw from the state it's
 * given, not from outside, so the screen on its way out keeps showing what it showed.
 */
@Composable
internal fun <S> FadeThrough(
    state: S,
    screenOf: (S) -> Any?,
    modifier: Modifier = Modifier,
    content: @Composable (S) -> Unit,
) {
    AnimatedContent(
        targetState = state,
        contentKey = screenOf,
        contentAlignment = Alignment.TopCenter,
        transitionSpec = {
            val enter = tween<Float>(Motion.FADE_IN_MS, Motion.FADE_OUT_MS, LinearOutSlowInEasing)
            val exit = tween<Float>(Motion.FADE_OUT_MS, easing = FastOutLinearInEasing)
            fadeIn(enter) togetherWith fadeOut(exit)
        },
        modifier = modifier,
        label = "screen",
    ) { content(it) }
}
