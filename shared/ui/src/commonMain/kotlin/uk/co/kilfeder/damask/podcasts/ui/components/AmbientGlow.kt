package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

private const val GLOW_OPACITY = 0.9f

/**
 * The design's ambient light (NowPlayingPane artboard): the artwork's own colours glowing out
 * from behind it. The artwork is drawn again at its own footprint and shape, blurred by
 * [Sizes.AmbientGlowBlur] at 90% opacity, so the glow follows the artwork's shape and fades out
 * evenly into the [Sizes.AmbientGlowInset] margin around it. Drawing the blurred artwork itself
 * gives the cover's real colours on every platform without extracting a palette.
 *
 * [content] (the artwork) is laid out inset by [Sizes.AmbientGlowInset], so the glow sits around
 * it and moves with it.
 */
@Composable
internal fun AmbientGlow(
    artworkUrl: String?,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        // A layer the size of the whole margin, with the artwork drawn at its own footprint in the
        // middle, blurred as a whole: the blur has room to spread into the margin and fade out.
        // The layer reaches a further blur-width beyond this composable (drawing outside its
        // bounds), so the blur's tail fades to nothing rather than ending at a faint edge.
        Box(
            Modifier
                .matchParentSize()
                .overflow(Sizes.AmbientGlowBlur)
                .graphicsLayer { alpha = GLOW_OPACITY }
                .blur(Sizes.AmbientGlowBlur, BlurredEdgeTreatment.Unbounded),
        ) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .matchParentSize()
                    .padding(Sizes.AmbientGlowInset + Sizes.AmbientGlowBlur)
                    .clip(shape),
            )
        }
        Box(Modifier.padding(Sizes.AmbientGlowInset), content = content)
    }
}

/** Measures and draws this element [extra] larger on every side than the space it's given. */
private fun Modifier.overflow(extra: Dp): Modifier = layout { measurable, constraints ->
    val px = extra.roundToPx()
    val placeable = measurable.measure(
        Constraints.fixed(constraints.maxWidth + 2 * px, constraints.maxHeight + 2 * px),
    )
    layout(constraints.maxWidth, constraints.maxHeight) { placeable.place(-px, -px) }
}
