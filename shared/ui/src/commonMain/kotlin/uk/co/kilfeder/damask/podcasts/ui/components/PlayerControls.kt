package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.episodes.PlayerPositionUi
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.pause
import uk.co.kilfeder.damask.podcasts.ui.resources.play
import uk.co.kilfeder.damask.podcasts.ui.resources.position
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.PillShape
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * Tap or drag along the bar. [onDrag] reports the fraction under the finger while dragging, and
 * null when the drag ends; [onSeek] gets the final fraction.
 */
private fun Modifier.seekGestures(rtl: Boolean, onSeek: (Float) -> Unit, onDrag: (Float?) -> Unit): Modifier {
    fun PointerInputScope.fraction(x: Float): Float {
        val f = (x / size.width).coerceIn(0f, 1f)
        return if (rtl) 1f - f else f
    }
    return pointerInput(rtl, onSeek) { detectTapGestures { onSeek(fraction(it.x)) } }
        .pointerInput(rtl, onSeek) {
            var last = 0f
            detectHorizontalDragGestures(
                onDragStart = {
                    last = fraction(it.x)
                    onDrag(last)
                },
                onDragEnd = {
                    onSeek(last)
                    onDrag(null)
                },
                onDragCancel = { onDrag(null) },
            ) { change, _ ->
                last = fraction(change.position.x)
                onDrag(last)
            }
        }
}

/**
 * The filled play/pause button with the damask diaper ([damaskFill]). [enabled] is false where
 * the platform has no player yet; it keeps its shape but greys out like any disabled button.
 */
@Composable
internal fun PlayButton(
    size: Dp,
    iconSize: Dp,
    pitch: Dp,
    playing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = IconButtonDefaults.filledIconButtonColors()
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = colors,
        modifier = modifier.size(size),
    ) {
        // Drawn inside the button, over its fill and under the icon; not on the grey disabled fill.
        val pattern = if (enabled) Modifier.damaskFill(MaterialTheme.colorScheme.onPrimary, pitch) else Modifier
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().clip(CircleShape).then(pattern)) {
            Icon(
                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (playing) Res.string.pause else Res.string.play),
                modifier = Modifier.size(iconSize),
            )
        }
    }
}

/**
 * The position bar with time played and time left under it (NowPlayingPane artboard). With
 * [onSeek] (the episode is the one playing) it can be tapped or dragged; the seek happens when the
 * finger lifts. Screen readers can set it too. [showThumb] is false in the slim bar on the show
 * notes screen.
 */
@Composable
internal fun PositionBar(
    position: PlayerPositionUi,
    showThumb: Boolean,
    onSeek: ((Float) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.position)
    var dragging by remember { mutableStateOf<Float?>(null) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1), modifier = modifier) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.MinTouchTarget)
                .then(if (onSeek == null) Modifier else Modifier.seekGestures(rtl, onSeek) { dragging = it })
                .semantics {
                    contentDescription = label
                    stateDescription = position.description
                    progressBarRangeInfo = ProgressBarRangeInfo(position.fraction, 0f..1f)
                    if (onSeek != null) {
                        setProgress { value ->
                            onSeek(value)
                            true
                        }
                    }
                },
        ) {
            Track(dragging ?: position.fraction, showThumb)
        }
        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            val color = MaterialTheme.colorScheme.onSurfaceVariant
            Text(position.played, style = DamaskTextStyles.Time, color = color)
            position.remaining?.let { Text(it, style = DamaskTextStyles.Time, color = color) }
        }
    }
}

@Composable
private fun Track(fraction: Float, showThumb: Boolean, modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .height(Sizes.ScrubberThumbHeight),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(Sizes.ProgressBarHeight)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Box(
            Modifier
                .fillMaxWidth(fraction)
                .height(Sizes.ProgressBarHeight)
                .clip(PillShape)
                .background(primary),
        )
        if (showThumb) {
            Row(Modifier.fillMaxWidth(fraction)) {
                Box(Modifier.weight(1f))
                Box(
                    Modifier
                        .offset(x = Sizes.ScrubberThumbWidth / 2)
                        .width(Sizes.ScrubberThumbWidth)
                        .fillMaxHeight()
                        .clip(PillShape)
                        .background(primary),
                )
            }
        }
    }
}
