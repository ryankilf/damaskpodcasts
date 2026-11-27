package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.episodes.PlayerPositionUi
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.play
import uk.co.kilfeder.damask.podcasts.ui.resources.position
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.PillShape
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * The filled play button with the damask diaper ([damaskFill]). [enabled] is false until
 * playback exists; it keeps its shape but greys out like any disabled button.
 */
@Composable
internal fun PlayButton(
    size: Dp,
    iconSize: Dp,
    pitch: Dp,
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
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .damaskFill(MaterialTheme.colorScheme.onPrimary, pitch),
    ) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(Res.string.play),
            modifier = Modifier.size(iconSize),
        )
    }
}

/**
 * The position bar with time played and time left under it (NowPlayingPane artboard). Shows the
 * saved position; it becomes draggable when playback arrives. [showThumb] is false in the slim
 * bar on the show notes view.
 */
@Composable
internal fun PositionBar(position: PlayerPositionUi, showThumb: Boolean, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.position)
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1), modifier = modifier) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(Sizes.ScrubberThumbHeight)
                .semantics {
                    contentDescription = label
                    progressBarRangeInfo = ProgressBarRangeInfo(position.fraction, 0f..1f)
                },
        ) {
            Track(position.fraction, showThumb)
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
