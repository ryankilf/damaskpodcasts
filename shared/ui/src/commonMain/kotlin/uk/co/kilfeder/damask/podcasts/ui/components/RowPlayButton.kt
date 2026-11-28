package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.material.icons.rounded.Replay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.row_pause
import uk.co.kilfeder.damask.podcasts.ui.resources.row_play
import uk.co.kilfeder.damask.podcasts.ui.resources.row_replay
import uk.co.kilfeder.damask.podcasts.ui.resources.row_resume
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

/**
 * The button at the end of an episode row (design hand-off › EpisodeRow), inside the progress
 * ring: ▶ on an empty ring when not started, ▶ on the progress ring to resume, and a filled
 * damask ⏸ while it's the episode playing. It plays or pauses in place; the row opens the episode.
 */
@Composable
internal fun RowPlayButton(row: EpisodeRowUi, playing: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val exact = row.length?.exact.orEmpty()
    val label = when {
        playing -> stringResource(Res.string.row_pause, row.title, exact)
        row.progress != null -> stringResource(Res.string.row_resume, row.title, exact)
        else -> stringResource(Res.string.row_play, row.title)
    }
    Box(contentAlignment = Alignment.Center, modifier = modifier.size(Sizes.MinTouchTarget)) {
        CircularProgressIndicator(
            progress = { row.progress ?: 0f },
            strokeWidth = Sizes.ProgressRingStroke,
            trackColor = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(Sizes.MinTouchTarget),
        )
        IconButton(onClick = onClick, modifier = Modifier.size(Sizes.MinTouchTarget)) {
            PlayPauseCrossfade(playing) { now ->
                if (now) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(Sizes.RowPauseButton)
                            .damaskButton(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.onPrimary,
                                Sizes.DamaskPitchSmall,
                            ),
                    ) {
                        PlayPauseIcon(playing = true, tint = MaterialTheme.colorScheme.onPrimary, description = label)
                    }
                } else {
                    PlayPauseIcon(playing = false, tint = MaterialTheme.colorScheme.onSurface, description = label)
                }
            }
        }
    }
}

/** A played episode on a show page: play it again (design hand-off › Show). */
@Composable
internal fun ReplayButton(row: EpisodeRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier.size(Sizes.MinTouchTarget)) {
        Icon(
            Icons.Rounded.Replay,
            contentDescription = stringResource(Res.string.row_replay, row.title),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
