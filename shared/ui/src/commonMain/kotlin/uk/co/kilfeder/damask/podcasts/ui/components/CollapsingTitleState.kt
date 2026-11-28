package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot

/**
 * Whether a page's big title has scrolled up under its small bar ([CollapsingTopBar]). Put
 * [Modifier.collapsingListTop] on the list and [Modifier.collapsingTitle] on the big title.
 */
@Stable
internal class CollapsingTitleState {
    internal var listTop by mutableFloatStateOf(0f)
    internal var titleBottom by mutableFloatStateOf(Float.MAX_VALUE)

    /** The big title is out of sight: the bar shows the name. */
    val collapsed: Boolean get() = titleBottom <= listTop
}

@Composable
internal fun rememberCollapsingTitleState(): CollapsingTitleState = remember { CollapsingTitleState() }

/** Marks where the scrolling content starts (just under the bar). */
internal fun Modifier.collapsingListTop(state: CollapsingTitleState): Modifier =
    onGloballyPositioned { state.listTop = it.positionInRoot().y }

/** Marks the big title, whose bottom edge passing under the bar collapses it. */
internal fun Modifier.collapsingTitle(state: CollapsingTitleState): Modifier =
    onGloballyPositioned { state.titleBottom = it.positionInRoot().y + it.size.height }
