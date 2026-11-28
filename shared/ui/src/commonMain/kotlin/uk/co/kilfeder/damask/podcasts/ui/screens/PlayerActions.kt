package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeUiState
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.PlayButton
import uk.co.kilfeder.damask.podcasts.ui.components.SkipButton
import uk.co.kilfeder.damask.podcasts.ui.components.SpeedButton
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_played
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_unplayed
import uk.co.kilfeder.damask.podcasts.ui.resources.playback_speed
import uk.co.kilfeder.damask.podcasts.ui.resources.skip_back
import uk.co.kilfeder.damask.podcasts.ui.resources.skip_forward
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

/** What the player's controls do. */
internal class PlayerActions(
    val onPlayPause: () -> Unit,
    val onSkipBack: () -> Unit,
    val onSkipForward: () -> Unit,
    val onCycleSpeed: () -> Unit,
    val onSeek: (Float) -> Unit,
    val onPlayedChange: (Boolean) -> Unit,
)

/**
 * The controls' actions on an episode's ViewModel (the player and its show notes share them).
 * Play starts it from [source], where the episode was opened from.
 */
internal fun playerActions(viewModel: EpisodeViewModel, source: PlaySource? = null) = PlayerActions(
    onPlayPause = { viewModel.onPlayPause(source) },
    onSkipBack = viewModel::onSkipBack,
    onSkipForward = viewModel::onSkipForward,
    onCycleSpeed = viewModel::onCycleSpeed,
    onSeek = viewModel::onSeek,
    onPlayedChange = viewModel::onPlayedChange,
)

/**
 * Speed, back 10, play/pause, forward 30, and the Played tick. Full size on the player; [compact]
 * (48dp buttons, evenly spaced) under the show notes.
 */
@Composable
internal fun TransportRow(
    state: EpisodeUiState,
    actions: PlayerActions,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val live = state.playbackAvailable && state.isCurrent
    val sizes = if (compact) TransportSizes.Compact else TransportSizes.Full
    val skipSize = sizes.skip
    val skipIcon = sizes.skipIcon
    Row(
        horizontalArrangement = if (compact) Arrangement.SpaceEvenly else Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        SpeedButton(state.speed, stringResource(Res.string.playback_speed, state.speed), live, actions.onCycleSpeed)
        val back = stringResource(Res.string.skip_back)
        SkipButton(Icons.Rounded.Replay10, back, live, actions.onSkipBack, size = skipSize, iconSize = skipIcon)
        PlayButton(
            size = sizes.play,
            iconSize = sizes.playIcon,
            pitch = sizes.pitch,
            playing = state.isPlaying,
            enabled = state.playbackAvailable,
            onClick = actions.onPlayPause,
        )
        val forward = stringResource(Res.string.skip_forward)
        SkipButton(Icons.Rounded.Forward30, forward, live, actions.onSkipForward, size = skipSize, iconSize = skipIcon)
        PlayedToggle(state.isPlayed, actions.onPlayedChange)
    }
}

/** The transport row's button sizes: full on the player, compact under the show notes. */
private enum class TransportSizes(val skip: Dp, val skipIcon: Dp, val play: Dp, val playIcon: Dp, val pitch: Dp) {
    Full(
        skip = Sizes.NowPlayingSkipButton,
        skipIcon = Sizes.NowPlayingSkipIcon,
        play = Sizes.NowPlayingPlayButton,
        playIcon = Sizes.NowPlayingPlayIcon,
        pitch = Sizes.DamaskPitchLarge,
    ),
    Compact(Sizes.MinTouchTarget, Sizes.Icon, Sizes.MinTouchTarget, Sizes.Icon, Sizes.DamaskPitchSmall),
}

/** Mark played (an outlined tick), or Mark unplayed once played (a filled one). */
@Composable
internal fun PlayedToggle(isPlayed: Boolean, onPlayedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    IconButton(onClick = { onPlayedChange(!isPlayed) }, modifier = modifier) {
        Icon(
            imageVector = if (isPlayed) Icons.Rounded.CheckCircle else Icons.Outlined.CheckCircle,
            contentDescription = stringResource(if (isPlayed) Res.string.mark_unplayed else Res.string.mark_played),
            tint = if (isPlayed) colors.primary else colors.onSurfaceVariant,
        )
    }
}
