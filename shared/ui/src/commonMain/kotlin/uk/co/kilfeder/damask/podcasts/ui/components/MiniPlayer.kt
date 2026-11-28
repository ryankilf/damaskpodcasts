package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.playback.MiniPlayerUi
import uk.co.kilfeder.damask.podcasts.presentation.playback.MiniPlayerViewModel
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.mini_player_open
import uk.co.kilfeder.damask.podcasts.ui.resources.pause
import uk.co.kilfeder.damask.podcasts.ui.resources.play
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * The episode that's playing, above the tabs on every screen (design hand-off › MiniPlayer):
 * artwork, title, show, progress, and play/pause. Tapping it opens the episode. Nothing shows
 * when nothing is loaded.
 */
@Composable
internal fun MiniPlayer(
    graph: AppGraph,
    extendUnderGestureBar: Boolean,
    onOpen: (EpisodeId) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MiniPlayerViewModel = viewModel { graph.miniPlayerViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val gestureBar = Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars)
    val player = state.player
    when {
        player != null -> Column(modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
            MiniPlayerBar(player, onOpen = { onOpen(player.id) }, onPlayPause = viewModel::onPlayPause)
            if (extendUnderGestureBar) Spacer(gestureBar)
        }
        extendUnderGestureBar -> Spacer(modifier.then(gestureBar))
    }
}

@Composable
private fun MiniPlayerBar(
    player: MiniPlayerUi,
    onOpen: () -> Unit,
    onPlayPause: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openLabel = stringResource(Res.string.mini_player_open, player.title)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space3),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClickLabel = openLabel, role = Role.Button, onClick = onOpen)
            .height(Sizes.MiniPlayerHeight)
            .padding(start = Spacing.Space4, end = Spacing.Space1),
    ) {
        Artwork(player.artworkUrl, size = Sizes.MiniPlayerArtwork)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1 / 2), modifier = Modifier.weight(1f)) {
            Text(
                text = player.title,
                style = DamaskTextStyles.Label,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = player.showTitle,
                style = DamaskTextStyles.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Progress(player, Modifier.padding(top = Spacing.Space1))
        }
        IconButton(onClick = onPlayPause) {
            Icon(
                imageVector = if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = stringResource(if (player.isPlaying) Res.string.pause else Res.string.play),
            )
        }
    }
}

@Composable
private fun Progress(player: MiniPlayerUi, modifier: Modifier = Modifier) {
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    val progress = player.progress
    val barModifier = modifier.fillMaxWidth().height(Sizes.MiniPlayerProgressHeight)
    if (progress == null || player.buffering) {
        LinearProgressIndicator(trackColor = trackColor, modifier = barModifier)
    } else {
        LinearProgressIndicator(progress = { progress }, trackColor = trackColor, modifier = barModifier)
    }
}
