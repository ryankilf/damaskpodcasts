package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import uk.co.kilfeder.damask.podcasts.ui.theme.PillShape
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * Dragging a sheet by its handle: [up] when dragged up far enough, [down] when dragged down.
 * Screen readers use the matching buttons instead.
 */
internal fun Modifier.sheetDrag(up: (() -> Unit)? = null, down: (() -> Unit)? = null): Modifier =
    pointerInput(up, down) {
        val threshold = Sizes.SheetDragThreshold.toPx()
        var dragged = 0f
        detectVerticalDragGestures(
            onDragStart = { dragged = 0f },
            onDragEnd = {
                when {
                    dragged < -threshold -> up?.invoke()
                    dragged > threshold -> down?.invoke()
                }
            },
        ) { _, amount -> dragged += amount }
    }

/** The small bar at the top of a sheet. Decorative. */
@Composable
internal fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(bottom = Spacing.Space2)
            .width(Sizes.SheetHandleWidth)
            .heightIn(min = Sizes.ProgressBarHeight)
            .background(MaterialTheme.colorScheme.outlineVariant, PillShape),
    )
}
