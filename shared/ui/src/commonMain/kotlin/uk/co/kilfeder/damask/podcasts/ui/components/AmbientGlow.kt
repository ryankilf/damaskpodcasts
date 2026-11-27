package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

private const val GLOW_OPACITY = 0.9f
private const val SOLID_UNTIL = 0.6f
private const val HALF_FROM = 0.82f
private const val HALF_ALPHA = 0.6f

/**
 * The design's ambient light (NowPlayingPane artboard): the artwork's own colours, blurred and
 * spread [Sizes.AmbientGlowInset] beyond its edges, solid in the middle and fading out (radial
 * stops at 60% and 82%, blur [Sizes.AmbientGlowBlur], 90% opacity). Drawing the blurred artwork
 * itself gives the cover's real colours on every platform without extracting a palette.
 *
 * [content] (the artwork) is laid out inset by [Sizes.AmbientGlowInset], so the glow stays
 * within this composable's bounds and moves with it.
 */
@Composable
internal fun AmbientGlow(
    artworkUrl: String?,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier) {
        AsyncImage(
            model = artworkUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                // Outer to inner: an offscreen layer, the radial fade over what's inside it, then
                // the blur. Fading the already-blurred image takes it to nothing before the edges,
                // so the glow has no visible boundary.
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    alpha = GLOW_OPACITY
                }
                .drawWithContent {
                    drawContent()
                    drawRect(brush = glowMask(), blendMode = BlendMode.DstIn)
                }
                .blur(Sizes.AmbientGlowBlur, BlurredEdgeTreatment.Unbounded),
        )
        Box(Modifier.padding(Sizes.AmbientGlowInset), content = content)
    }
}

/**
 * An alpha mask, not a visible colour: only its opacity is used (DstIn keeps the glow where the
 * mask is opaque).
 */
@Suppress("NoHardcodedColors")
private fun glowMask(): Brush = Brush.radialGradient(
    0f to Color.Black,
    SOLID_UNTIL to Color.Black,
    HALF_FROM to Color.Black.copy(alpha = HALF_ALPHA),
    1f to Color.Transparent,
)
