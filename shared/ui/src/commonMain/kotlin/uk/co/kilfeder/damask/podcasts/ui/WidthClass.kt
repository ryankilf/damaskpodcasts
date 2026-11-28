package uk.co.kilfeder.damask.podcasts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import uk.co.kilfeder.damask.podcasts.ui.components.PaneHeader
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** Material's window size classes, by width. */
internal enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

internal fun widthClassOf(width: Dp): WidthClass = when {
    width >= Sizes.ExpandedWidth -> WidthClass.EXPANDED
    width >= Sizes.MediumWidth -> WidthClass.MEDIUM
    else -> WidthClass.COMPACT
}

/**
 * The list pane (fixed width) beside the detail pane, which sits on a rounded, lightly tinted card
 * inset from the window's edges (TabletHome, TabletGenres artboards).
 */
@Composable
internal fun TwoPanes(
    left: @Composable (Modifier) -> Unit,
    right: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        left(Modifier.width(Sizes.ListPaneWidth).fillMaxHeight())
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = Spacing.Space2, top = Spacing.Space4, end = Spacing.Space4, bottom = Spacing.Space4)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
        ) { right(Modifier.fillMaxSize()) }
    }
}

/** A list pane with its own heading, as wide layouts have no app bar. */
@Composable
internal fun ListPane(title: String, modifier: Modifier = Modifier, content: @Composable (Modifier) -> Unit) {
    Column(modifier) {
        PaneHeader(title)
        content(Modifier.weight(1f).fillMaxWidth())
    }
}

