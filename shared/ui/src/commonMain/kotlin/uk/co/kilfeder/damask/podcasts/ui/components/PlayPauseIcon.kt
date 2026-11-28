package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.pause
import uk.co.kilfeder.damask.podcasts.ui.resources.play
import uk.co.kilfeder.damask.podcasts.ui.theme.Motion

/** Crossfades between two states of a play button (spec › Motion), so the swap isn't sudden. */
@Composable
internal fun PlayPauseCrossfade(
    playing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (Boolean) -> Unit,
) {
    Crossfade(
        targetState = playing,
        animationSpec = tween(Motion.PLAY_PAUSE_MS),
        modifier = modifier,
        label = "playPause",
    ) { content(it) }
}

/** ▶ or ⏸, crossfading between them. Labelled Play or Pause unless [description] says more. */
@Composable
internal fun PlayPauseIcon(
    playing: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    description: String? = null,
) {
    PlayPauseCrossfade(playing) { now ->
        Icon(
            imageVector = if (now) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = description ?: stringResource(if (now) Res.string.pause else Res.string.play),
            tint = tint,
            modifier = modifier,
        )
    }
}
