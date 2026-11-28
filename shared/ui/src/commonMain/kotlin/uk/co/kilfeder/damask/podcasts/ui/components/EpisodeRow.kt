package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.row_show_prefix
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * One episode (design hand-off › EpisodeRow): artwork, two-line title, "show · time" with the
 * exact value on long press, and a progress ring once started.
 *
 * Tapping the row plays and opens the episode ([onClick]); the button in the ring plays or pauses
 * it in place ([onPlayPause], [RowPlayButton]). With [swipeLabel] set, swiping the row
 * away (or the matching accessibility action) calls [onSwipe]: Mark played in queues, Mark
 * unplayed in Played.
 */
@Composable
internal fun EpisodeRow(
    row: EpisodeRowUi,
    playing: Boolean,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
    swipeLabel: String? = null,
    onSwipe: () -> Unit = {},
    background: Color = MaterialTheme.colorScheme.surface,
) {
    if (swipeLabel == null) {
        RowContent(row, playing, onClick, onPlayPause, modifier)
        return
    }
    val state = rememberSwipeToDismissBoxState()
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        onDismiss = { if (it == SwipeToDismissBoxValue.EndToStart) onSwipe() },
        backgroundContent = { SwipeBackground(swipeLabel) },
        modifier = modifier.semantics {
            customActions = listOf(CustomAccessibilityAction(swipeLabel) { onSwipe(); true })
        },
    ) {
        // Opaque over the swipe background; [background] matches what the row sits on.
        RowContent(row, playing, onClick, onPlayPause, Modifier.background(background))
    }
}

@Composable
private fun RowContent(
    row: EpisodeRowUi,
    playing: Boolean,
    onClick: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space3),
        modifier = modifier
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = Sizes.RowMinHeight)
            .padding(horizontal = Spacing.Space4, vertical = Spacing.Space2),
    ) {
        Artwork(row.artworkUrl)
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Space1),
            modifier = Modifier.weight(1f),
        ) {
            row.genre?.let { GenreChip(it) }
            Text(
                text = row.title,
                style = DamaskTextStyles.Title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            MetaLine(row)
        }
        RowPlayButton(row, playing, onPlayPause)
    }
}

/** "Show · 32 min left": the show name truncates first, so the time always stays visible. */
@Composable
private fun MetaLine(row: EpisodeRowUi, modifier: Modifier = Modifier) {
    val time = row.length ?: row.published
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier) {
        Text(
            text = stringResource(Res.string.row_show_prefix, row.showTitle),
            style = DamaskTextStyles.BodySmall,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        ExactValueText(value = time, style = DamaskTextStyles.Time, color = color)
    }
}

@Composable
private fun SwipeBackground(label: String, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.CenterEnd,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = Spacing.Space6),
    ) {
        Icon(
            imageVector = Icons.Rounded.Done,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
