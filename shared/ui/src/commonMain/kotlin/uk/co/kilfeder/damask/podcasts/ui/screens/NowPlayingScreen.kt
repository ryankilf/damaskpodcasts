package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.min
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeUiState
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.AmbientGlow
import uk.co.kilfeder.damask.podcasts.ui.components.PlayButton
import uk.co.kilfeder.damask.podcasts.ui.components.PositionBar
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_gone_body
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_gone_title
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_no_notes
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_played
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_unplayed
import uk.co.kilfeder.damask.podcasts.ui.resources.now_playing_close
import uk.co.kilfeder.damask.podcasts.ui.resources.playback_speed
import uk.co.kilfeder.damask.podcasts.ui.resources.show_notes
import uk.co.kilfeder.damask.podcasts.ui.resources.show_notes_open
import uk.co.kilfeder.damask.podcasts.ui.resources.skip_back
import uk.co.kilfeder.damask.podcasts.ui.resources.skip_forward
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.PillShape
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** Playback isn't built yet: play, skip and speed show but stay disabled until it is. */
private const val PLAYBACK_AVAILABLE = false

/**
 * Now Playing (design hand-off › NowPlayingPane): reached from any episode row, full screen.
 * Artwork with the ambient glow takes whatever height is left; the genre chip, title, show,
 * position bar and controls keep their spacing; the show notes peek sits at the bottom and opens
 * the notes view (NowPlayingNotes).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun NowPlayingScreen(
    graph: AppGraph,
    id: EpisodeId,
    onClose: () -> Unit,
    onOpenGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EpisodeViewModel = viewModel(key = "${id.showId}/${id.guid}") { graph.episodeViewModel(id) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(enabled = state.notesOpen) { viewModel.onNotesOpenChange(false) }
    val episode = state.episode
    when {
        state.loading -> Loading(modifier)
        episode == null -> EmptyState(
            title = stringResource(Res.string.episode_gone_title),
            body = stringResource(Res.string.episode_gone_body),
            modifier = modifier,
        )
        state.notesOpen -> ShowNotesView(state, episode, onClose = { viewModel.onNotesOpenChange(false) }, modifier)
        else -> Player(
            state = state,
            episode = episode,
            onClose = onClose,
            onOpenGenre = onOpenGenre,
            onPlayedChange = { played -> if (played) viewModel.onMarkPlayed() else viewModel.onMarkUnplayed() },
            onOpenNotes = { viewModel.onNotesOpenChange(true) },
            modifier = modifier,
        )
    }
}

@Composable
private fun Player(
    state: EpisodeUiState,
    episode: EpisodeRowUi,
    onClose: () -> Unit,
    onOpenGenre: (String) -> Unit,
    onPlayedChange: (Boolean) -> Unit,
    onOpenNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.weight(1f).padding(horizontal = Spacing.Space6)) {
            IconButton(onClick = onClose, modifier = Modifier.padding(top = Spacing.Space2)) {
                Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(Res.string.now_playing_close))
            }
            Artwork(episode.artworkUrl, Modifier.weight(1f).fillMaxWidth())
            Details(state, episode, onOpenGenre, Modifier.padding(top = Spacing.Space6))
            state.position?.let { PositionBar(it, showThumb = true, modifier = Modifier.padding(top = Spacing.Space4)) }
            Controls(state, onPlayedChange, Modifier.padding(top = Spacing.Space3))
        }
        NotesPeek(state.notesPreview, onOpenNotes, Modifier.padding(top = Spacing.Space6))
    }
}

/** The artwork fills the space left over, square, at most [Sizes.NowPlayingArtworkMax]. */
@Composable
private fun Artwork(url: String?, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.heightIn(min = Sizes.NowPlayingArtworkMin),
    ) {
        val glow = Sizes.AmbientGlowInset * 2
        val side = min(min(maxWidth, maxHeight - glow), Sizes.NowPlayingArtworkMax)
            .coerceAtLeast(Sizes.NowPlayingArtworkMin - glow)
        AmbientGlow(artworkUrl = url, shape = MaterialTheme.shapes.medium) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(side).clip(MaterialTheme.shapes.medium),
            )
        }
    }
}

@Composable
private fun Details(
    state: EpisodeUiState,
    episode: EpisodeRowUi,
    onOpenGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space2), modifier = modifier.fillMaxWidth()) {
        state.genre?.let { genre ->
            Text(
                text = genre,
                style = DamaskTextStyles.Label,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.tertiaryContainer, PillShape)
                    .padding(horizontal = Spacing.Space2, vertical = Spacing.Space1 / 2),
            )
        }
        Text(
            text = episode.title,
            style = DamaskTextStyles.TitleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() },
        )
        val showModifier = state.genreId?.let { id ->
            Modifier.clickable(role = Role.Button) { onOpenGenre(id) }
        } ?: Modifier
        Text(
            text = episode.showTitle,
            style = DamaskTextStyles.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = showModifier,
        )
    }
}

/** Speed, back 10, play, forward 30, mark played (NowPlayingPane artboard). */
@Composable
private fun Controls(state: EpisodeUiState, onPlayedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        val speedLabel = stringResource(Res.string.playback_speed, state.speed)
        TextButton(
            onClick = {},
            enabled = PLAYBACK_AVAILABLE,
            modifier = Modifier.semantics { contentDescription = speedLabel },
        ) { Text(state.speed, style = DamaskTextStyles.Label) }
        SkipButton(Icons.Rounded.Replay10, stringResource(Res.string.skip_back))
        PlayButton(
            size = Sizes.NowPlayingPlayButton,
            iconSize = Sizes.NowPlayingPlayIcon,
            pitch = Sizes.DamaskPitchLarge,
            enabled = PLAYBACK_AVAILABLE,
            onClick = {},
        )
        SkipButton(Icons.Rounded.Forward30, stringResource(Res.string.skip_forward))
        PlayedToggle(state.isPlayed, onPlayedChange)
    }
}

/** Mark played (an outlined tick), or Mark unplayed once played (a filled one). */
@Composable
private fun PlayedToggle(isPlayed: Boolean, onPlayedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    IconButton(onClick = { onPlayedChange(!isPlayed) }, modifier = modifier) {
        Icon(
            imageVector = if (isPlayed) Icons.Rounded.CheckCircle else Icons.Outlined.CheckCircle,
            contentDescription = stringResource(if (isPlayed) Res.string.mark_unplayed else Res.string.mark_played),
            tint = if (isPlayed) colors.primary else colors.onSurfaceVariant,
        )
    }
}

@Composable
private fun SkipButton(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    IconButton(onClick = {}, enabled = PLAYBACK_AVAILABLE, modifier = modifier.size(Sizes.NowPlayingSkipButton)) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(Sizes.NowPlayingSkipIcon))
    }
}

/** The show notes peek at the bottom (NowPlayingPane artboard); tapping opens the notes view. */
@Composable
private fun NotesPeek(preview: String?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.show_notes_open)
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Space1),
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(topStart = Sizes.SheetCornerRadius, topEnd = Sizes.SheetCornerRadius),
            )
            .clickable(enabled = preview != null, onClickLabel = label, role = Role.Button, onClick = onOpen)
            .navigationBarsPadding()
            .padding(horizontal = Spacing.Space6, vertical = Spacing.Space3),
    ) {
        SheetHandle(Modifier.align(Alignment.CenterHorizontally))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.show_notes),
                style = DamaskTextStyles.Title,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            if (preview != null) {
                Icon(
                    imageVector = Icons.Rounded.ExpandLess,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = preview ?: stringResource(Res.string.episode_no_notes),
            style = DamaskTextStyles.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The small bar at the top of a sheet. Decorative. */
@Composable
internal fun SheetHandle(modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(bottom = Spacing.Space2)
            .width(Sizes.SheetHandleWidth)
            .heightIn(min = Sizes.ProgressBarHeight)
            .background(MaterialTheme.colorScheme.outlineVariant, PillShape),
    )
}
