package uk.co.kilfeder.damask.podcasts.ui.components

import uk.co.kilfeder.damask.podcasts.ui.resources.row_pause
import uk.co.kilfeder.damask.podcasts.ui.resources.pause
import uk.co.kilfeder.damask.podcasts.ui.components.PlayPauseIcon
import uk.co.kilfeder.damask.podcasts.ui.components.damaskButton
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised
import uk.co.kilfeder.damask.podcasts.ui.components.ExactValueText
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.show_button_time
import uk.co.kilfeder.damask.podcasts.ui.resources.play
import uk.co.kilfeder.damask.podcasts.ui.resources.show_play_label
import uk.co.kilfeder.damask.podcasts.ui.resources.show_resume
import uk.co.kilfeder.damask.podcasts.ui.resources.row_resume
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing
import uk.co.kilfeder.damask.podcasts.presentation.episodes.NextUpUi

/** The time beside "Resume" or "Play" is a little quieter than the word. */
private const val BUTTON_TIME_ALPHA = 0.8f

/**
 * "Resume · 32 min left" or "Play · 47 min", and "Pause" while it plays. It plays in place; it
 * doesn't open the episode. Never the episode's name (feed titles aren't reliable enough for a
 * button); that's in the label read out, and on long press.
 */
@Composable
internal fun PlayNextButton(
    next: NextUpUi,
    playing: Boolean,
    onPlayPause: (EpisodeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val time = next.length
    val exact = time?.exact.orEmpty()
    val label = when {
        playing -> stringResource(Res.string.row_pause, next.title, exact)
        next.resume -> stringResource(Res.string.row_resume, next.title, exact)
        else -> stringResource(Res.string.show_play_label, next.title, exact)
    }
    val word = when {
        playing -> Res.string.pause
        next.resume -> Res.string.show_resume
        else -> Res.string.play
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.MinTouchTarget)
            .damaskButton(colors.primary, colors.onPrimary, Sizes.DamaskPitchMedium)
            .clickable(role = Role.Button, onClickLabel = label) { onPlayPause(next.id) },
    ) {
        PlayPauseIcon(playing, tint = colors.onPrimary, description = "")
        Text(
            text = stringResource(word),
            style = DamaskTextStyles.Label,
            color = colors.onPrimary,
        )
        time?.let {
            ExactValueText(
                value = Summarised(it.summary, "${next.title} · ${it.exact}"),
                display = stringResource(Res.string.show_button_time, it.summary),
                style = DamaskTextStyles.Time,
                color = colors.onPrimary,
                modifier = Modifier.alpha(BUTTON_TIME_ALPHA),
            )
        }
    }
}
