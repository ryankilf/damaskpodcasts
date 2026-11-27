package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

/** Each cover further back is this much smaller than the one in front of it. */
private const val SHRINK_PER_STEP = 0.12f

/**
 * Up to [maxCovers] show covers stacked front to back, each one behind peeking out towards the
 * end edge, a little smaller. Says "a group of shows" at row size, where a 2×2 mosaic would be
 * too small to read. With no covers it shows the empty artwork placeholder. Decorative, like
 * [Artwork].
 */
@Composable
internal fun CoverStack(urls: List<String>, maxCovers: Int, modifier: Modifier = Modifier) {
    val covers = urls.take(maxCovers).ifEmpty { listOf(null) }
    val endOrigin = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1f else 0f
    val peek = Sizes.CoverStackPeek
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier.size(DpSize(Sizes.RowArtwork + peek * (maxCovers - 1), Sizes.RowArtwork)),
    ) {
        // Back to front, so the front cover draws last.
        covers.indices.reversed().forEach { depth ->
            val scale = 1f - depth * SHRINK_PER_STEP
            Artwork(
                url = covers[depth],
                modifier = Modifier
                    .offset(x = peek * depth)
                    // Shrink towards the end edge, so each cover peeks out by exactly [peek].
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin(endOrigin, 0.5f)
                    }
                    .border(Sizes.CoverStackOutline, MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small),
            )
        }
    }
}
