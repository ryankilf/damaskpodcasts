package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.ui.components.sheetSurface
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Podcasts
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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
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
import uk.co.kilfeder.damask.podcasts.presentation.episodes.key
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeUiState
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeViewModel
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import uk.co.kilfeder.damask.podcasts.ui.components.AmbientGlow
import uk.co.kilfeder.damask.podcasts.ui.components.Artwork
import uk.co.kilfeder.damask.podcasts.ui.components.GenreChip
import uk.co.kilfeder.damask.podcasts.ui.components.PlayButton
import uk.co.kilfeder.damask.podcasts.ui.components.PositionBar
import uk.co.kilfeder.damask.podcasts.ui.components.SheetHandle
import uk.co.kilfeder.damask.podcasts.ui.components.SkipButton
import uk.co.kilfeder.damask.podcasts.ui.components.SpeedButton
import uk.co.kilfeder.damask.podcasts.ui.components.sheetDrag
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_gone_body
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_gone_title
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_no_notes
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_played
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_unplayed
import uk.co.kilfeder.damask.podcasts.ui.resources.playback_speed
import uk.co.kilfeder.damask.podcasts.ui.resources.show_notes
import uk.co.kilfeder.damask.podcasts.ui.resources.show_notes_open
import uk.co.kilfeder.damask.podcasts.ui.resources.skip_back
import uk.co.kilfeder.damask.podcasts.ui.resources.skip_forward
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.PillShape
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * One episode, laid out like the design's Now Playing (NowPlayingPane): an ordinary screen with
 * the top bar and tabs, or the right-hand pane on wide layouts. When it's wide enough
 * ([Sizes.NotesBesideMinWidth]) the show notes sit beside the player. Artwork with the ambient
 * glow takes whatever height is left; the genre chip, title, show,
 * position bar and controls keep their spacing; the show notes peek sits at the bottom and opens
 * the show notes screen.
 */
@Composable
internal fun NowPlayingScreen(
    graph: AppGraph,
    id: EpisodeId,
    onOpenGenre: (String) -> Unit,
    onOpenNotes: () -> Unit,
    modifier: Modifier = Modifier,
    source: PlaySource? = null,
    viewModel: EpisodeViewModel = viewModel(key = episodeKey(id)) { graph.episodeViewModel(id) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val controls = playerActions(viewModel, source)
    BoxWithConstraints(modifier) {
        // With room for both, the show notes sit beside the player instead of behind the peek.
        val notesBeside = maxWidth >= Sizes.NotesBesideMinWidth
        EpisodeLoaded(state) { episode ->
            if (notesBeside) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Spacing.Space6),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Player(state, episode, onOpenGenre, controls, onOpenNotes = null, Modifier.weight(1f))
                    NotesSheet(state, onClose = null, modifier = Modifier.weight(1f).padding(top = Spacing.Space4))
                }
            } else {
                Player(state, episode, onOpenGenre, controls, onOpenNotes)
            }
        }
    }
}

/** One ViewModel per episode, shared by its player and show notes screens. */
internal fun episodeKey(id: EpisodeId) = "episode/${id.key}"

/** Loading, "episode not available", or [content] once the episode is known. */
@Composable
internal fun EpisodeLoaded(
    state: EpisodeUiState,
    modifier: Modifier = Modifier,
    content: @Composable (EpisodeRowUi) -> Unit,
) {
    val episode = state.episode
    Loadable(state.loading, modifier) {
        if (episode == null) {
            EmptyState(
                icon = Icons.Rounded.Podcasts,
                title = stringResource(Res.string.episode_gone_title),
                body = stringResource(Res.string.episode_gone_body),
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            content(episode)
        }
    }
}

@Composable
private fun Player(
    state: EpisodeUiState,
    episode: EpisodeRowUi,
    onOpenGenre: (String) -> Unit,
    controls: PlayerActions,
    onOpenNotes: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).padding(horizontal = Spacing.Space6)) {
            PlayerArtwork(episode.artworkUrl, episode.fallback, Modifier.weight(1f).fillMaxWidth())
            Details(state, episode, onOpenGenre, Modifier.padding(top = Spacing.Space6))
            state.position?.let {
                PositionBar(
                    position = it,
                    showThumb = true,
                    onSeek = controls.onSeek.takeIf { state.isCurrent },
                    modifier = Modifier.padding(top = Spacing.Space4),
                )
            }
            TransportRow(state, controls, Modifier.padding(top = Spacing.Space3))
            state.error?.let {
                Text(
                    text = it,
                    style = DamaskTextStyles.BodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = Spacing.Space2),
                )
            }
        }
        if (onOpenNotes == null) {
            Spacer(Modifier.height(Spacing.Space6))
        } else {
            NotesPeek(state.notesPreview, onOpenNotes, Modifier.padding(top = Spacing.Space6))
        }
    }
}


/** The artwork fills the space left over, square, at most [Sizes.NowPlayingArtworkMax]. */
@Composable
private fun PlayerArtwork(url: String?, fallback: ArtworkFallback, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.heightIn(min = Sizes.NowPlayingArtworkMin),
    ) {
        val glow = Sizes.AmbientGlowInset * 2
        val side = min(min(maxWidth, maxHeight - glow), Sizes.NowPlayingArtworkMax)
            .coerceAtLeast(Sizes.NowPlayingArtworkMin - glow)
        AmbientGlow(artworkUrl = url, shape = MaterialTheme.shapes.medium) {
            Artwork(url, fallback, size = side, shape = MaterialTheme.shapes.medium)
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
        state.genre?.let { GenreChip(it, episode.genreHue) }
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



/** The show notes peek at the bottom (NowPlayingPane artboard); tapping opens the notes view. */
@Composable
private fun NotesPeek(preview: String?, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(Res.string.show_notes_open)
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Space1),
        modifier = modifier
            .fillMaxWidth()
            .sheetSurface(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(enabled = preview != null, onClickLabel = label, role = Role.Button, onClick = onOpen)
            .then(if (preview != null) Modifier.sheetDrag(up = onOpen) else Modifier)
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
