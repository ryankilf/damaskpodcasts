package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import uk.co.kilfeder.damask.podcasts.ui.theme.Motion
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** The skeleton's pattern strength (design: the damask at 10% of on-surface-variant). */
private const val SKELETON_PATTERN_ALPHA = 0.10f

/** Each placeholder row's title and meta widths, as the design varies them so it reads as a list. */
private val ROW_WIDTHS = listOf(
    0.72f to 0.44f,
    0.58f to 0.36f,
    0.80f to 0.40f,
    0.64f to 0.30f,
    0.76f to 0.42f,
    0.52f to 0.34f,
)

/**
 * Placeholder rows while a list loads (design: `.skel`): [count] rows of an artwork block, a title
 * bar and a shorter line, and a round button block if [withButton], in the list's own sizes, so the
 * real rows land where the placeholders were. Blocks are surface-container-high with the damask
 * drifting slowly across them. Screen readers hear only [label] ("Searching", "Loading episodes").
 */
@Composable
internal fun SkeletonRows(
    count: Int,
    artwork: Dp,
    label: String,
    modifier: Modifier = Modifier,
    rowHeight: Dp = Sizes.ShowRowMinHeight,
    withButton: Boolean = false,
) {
    val driftState = rememberSkeletonDrift()
    val drift = { driftState.value }
    Column(modifier.clearAndSetSemantics { contentDescription = label }) {
        repeat(count) { index ->
            val (title, meta) = ROW_WIDTHS[index % ROW_WIDTHS.size]
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Space3),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = rowHeight)
                    .padding(horizontal = Spacing.Space4, vertical = Spacing.Space2),
            ) {
                SkeletonBlock(drift, Modifier.size(artwork))
                Column(verticalArrangement = Arrangement.spacedBy(Sizes.SkeletonGap), modifier = Modifier.weight(1f)) {
                    SkeletonBlock(drift, Modifier.fillMaxWidth(title).height(Sizes.SkeletonTitle))
                    SkeletonBlock(drift, Modifier.fillMaxWidth(meta).height(Sizes.SkeletonMeta))
                }
                if (withButton) SkeletonBlock(drift, Modifier.size(Sizes.SkeletonButton), CircleShape)
            }
        }
    }
}

/** One placeholder block; [drift] is read while drawing, so the drift doesn't recompose it. */
@Composable
private fun SkeletonBlock(
    drift: () -> Float,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .damaskFill(colors.onSurfaceVariant, Sizes.DamaskPitchMedium, SKELETON_PATTERN_ALPHA, drift),
    )
}

/** 0..1, repeating: how far the skeleton's pattern has drifted. Still when animations are off. */
@Composable
private fun rememberSkeletonDrift(): State<Float> =
    rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(Motion.SKELETON_DRIFT_MS, easing = LinearEasing), RepeatMode.Restart),
        label = "drift",
    )
