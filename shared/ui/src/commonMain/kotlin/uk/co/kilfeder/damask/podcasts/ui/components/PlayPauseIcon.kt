package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.pause
import uk.co.kilfeder.damask.podcasts.ui.resources.play
import uk.co.kilfeder.damask.podcasts.ui.theme.Motion

/**
 * Swaps a play button between play and pause (spec › Motion): the old state fades out, then the
 * new one fades and grows in, so the two never show on top of each other. Both stay centred (the
 * pause disc is bigger than the ▶; a plain `Crossfade` pins them top-left).
 */
@Composable
internal fun PlayPauseCrossfade(
    playing: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable (Boolean) -> Unit,
) {
    AnimatedContent(
        targetState = playing,
        transitionSpec = {
            val fadeSpec = tween<Float>(Motion.PLAY_PAUSE_IN_MS, delayMillis = Motion.PLAY_PAUSE_OUT_MS)
            val enter = fadeIn(fadeSpec) + scaleIn(fadeSpec, Motion.PLAY_PAUSE_START_SCALE)
            (enter togetherWith fadeOut(tween(Motion.PLAY_PAUSE_OUT_MS))).using(null)
        },
        contentAlignment = Alignment.Center,
        modifier = modifier,
        label = "playPause",
    ) { content(it) }
}

/** ▶ or ⏸, swapping with [PlayPauseCrossfade]. Labelled Play or Pause unless [description] says more. */
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
