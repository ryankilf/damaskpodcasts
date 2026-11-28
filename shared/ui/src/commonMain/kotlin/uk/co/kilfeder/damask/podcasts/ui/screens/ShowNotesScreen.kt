package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Forward30
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeViewModel
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeUiState
import uk.co.kilfeder.damask.podcasts.presentation.episodes.NotesBlock
import uk.co.kilfeder.damask.podcasts.presentation.episodes.NotesLink
import uk.co.kilfeder.damask.podcasts.ui.components.Artwork
import uk.co.kilfeder.damask.podcasts.ui.components.PlayButton
import uk.co.kilfeder.damask.podcasts.ui.components.PositionBar
import uk.co.kilfeder.damask.podcasts.ui.components.SkipButton
import uk.co.kilfeder.damask.podcasts.ui.components.rememberSafeIntent
import uk.co.kilfeder.damask.podcasts.ui.components.SpeedButton
import uk.co.kilfeder.damask.podcasts.ui.components.sheetDrag
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_no_notes
import uk.co.kilfeder.damask.podcasts.ui.resources.playback_speed
import uk.co.kilfeder.damask.podcasts.ui.resources.show_notes
import uk.co.kilfeder.damask.podcasts.ui.resources.skip_back
import uk.co.kilfeder.damask.podcasts.ui.resources.skip_forward
import uk.co.kilfeder.damask.podcasts.ui.resources.show_notes_close
import uk.co.kilfeder.damask.podcasts.ui.resources.show_notes_links
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** Link schemes show notes may open; anything else (javascript:, intent:, file:) is ignored. */
private val OPENABLE_SCHEMES = setOf("http", "https", "mailto")

/** Starts a bulleted item in show notes. Not user-facing copy: a typographic mark. */
private const val BULLET = "•  "

/**
 * An episode's show notes (design hand-off › NowPlayingNotes), pushed from its screen: a compact header
 * (artwork, title, show, play) with a slim position bar, then the notes on a sheet, then every
 * link listed under Links as a full-size touch target. Links in the text open too.
 */
@Composable
internal fun ShowNotesScreen(
    graph: AppGraph,
    id: EpisodeId,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EpisodeViewModel = viewModel(key = episodeKey(id)) { graph.episodeViewModel(id) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val controls = PlayerActions(
        onPlayPause = viewModel::onPlayPause,
        onSkipBack = viewModel::onSkipBack,
        onSkipForward = viewModel::onSkipForward,
        onCycleSpeed = viewModel::onCycleSpeed,
        onSeek = viewModel::onSeek,
        onPlayedChange = { played -> if (played) viewModel.onMarkPlayed() else viewModel.onMarkUnplayed() },
    )
    Box(modifier) {
        EpisodeLoaded(state) { episode -> ShowNotesContent(state, episode, controls, onClose) }
    }
}

@Composable
private fun ShowNotesContent(
    state: EpisodeUiState,
    episode: EpisodeRowUi,
    controls: PlayerActions,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Header(state, episode, controls)
        NotesSheet(state, onClose, Modifier.weight(1f))
    }
}

/**
 * The show notes on their sheet: heading, notes, then Links. Used by the show notes screen, and
 * beside the player on wide episode screens. With [onClose] (the notes screen), a down arrow by
 * the heading, or dragging the heading down, closes it, mirroring the peek's up arrow. No handle:
 * the open sheet isn't dragged around, so a handle would only suggest it could be.
 */
@Composable
internal fun NotesSheet(state: EpisodeUiState, onClose: (() -> Unit)?, modifier: Modifier = Modifier) {
    val open = rememberSafeIntent()
    val links = remember(open) { SafeLinks(open) }
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Space4),
        modifier = modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerLow,
                RoundedCornerShape(topStart = Sizes.SheetCornerRadius, topEnd = Sizes.SheetCornerRadius),
            )
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Space6, vertical = Spacing.Space3),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (onClose != null) Modifier.sheetDrag(down = onClose) else Modifier),
        ) {
            Text(
                text = stringResource(Res.string.show_notes),
                style = DamaskTextStyles.TitleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).semantics { heading() },
            )
            if (onClose != null) {
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Rounded.ExpandMore,
                        contentDescription = stringResource(Res.string.show_notes_close),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (state.notes.isEmpty()) {
            Text(
                text = stringResource(Res.string.episode_no_notes),
                style = DamaskTextStyles.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Notes(state.notes, links)
        }
        if (state.links.isNotEmpty()) Links(state.links, links)
    }
}

@Composable
private fun Header(
    state: EpisodeUiState,
    episode: EpisodeRowUi,
    controls: PlayerActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier.padding(horizontal = Spacing.Space3).padding(top = Spacing.Space2, bottom = Spacing.Space3)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Space3),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Spacing.Space3, end = Spacing.Space1),
        ) {
            Artwork(episode.artworkUrl, episode.fallback, size = Sizes.NotesHeaderArtwork)
            Column(Modifier.weight(1f)) {
                Text(
                    text = episode.title,
                    style = DamaskTextStyles.Title,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = episode.showTitle,
                    style = DamaskTextStyles.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        state.position?.let {
            PositionBar(
                position = it,
                showThumb = false,
                onSeek = controls.onSeek.takeIf { state.isCurrent },
                modifier = Modifier.padding(horizontal = Spacing.Space3),
            )
        }
        CompactControls(state, controls, Modifier.padding(horizontal = Spacing.Space3))
    }
}

/** Speed, back 10, play/pause, forward 30 and mark played: the full player's row, smaller. */
@Composable
private fun CompactControls(state: EpisodeUiState, controls: PlayerActions, modifier: Modifier = Modifier) {
    val live = state.playbackAvailable && state.isCurrent
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        SpeedButton(state.speed, stringResource(Res.string.playback_speed, state.speed), live, controls.onCycleSpeed)
        SkipButton(
            icon = Icons.Rounded.Replay10,
            label = stringResource(Res.string.skip_back),
            enabled = live,
            onClick = controls.onSkipBack,
            size = Sizes.MinTouchTarget,
            iconSize = Sizes.Icon,
        )
        PlayButton(
            size = Sizes.MinTouchTarget,
            iconSize = Sizes.Icon,
            pitch = Sizes.DamaskPitchSmall,
            playing = state.isPlaying,
            enabled = state.playbackAvailable,
            onClick = controls.onPlayPause,
        )
        SkipButton(
            icon = Icons.Rounded.Forward30,
            label = stringResource(Res.string.skip_forward),
            enabled = live,
            onClick = controls.onSkipForward,
            size = Sizes.MinTouchTarget,
            iconSize = Sizes.Icon,
        )
        PlayedToggle(state.isPlayed, controls.onPlayedChange)
    }
}

@Composable
private fun Notes(blocks: List<NotesBlock>, links: LinkInteractionListener, modifier: Modifier = Modifier) {
    val linkStyles = TextLinkStyles(
        SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
    )
    SelectionContainer(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space3)) {
            blocks.forEach { block ->
                val text = remember(block, linkStyles) { block.toAnnotatedString(linkStyles, links) }
                Text(text = text, style = DamaskTextStyles.Body, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

/** Every link from the notes as a full-size row (NowPlayingNotes artboard › Links). */
@Composable
private fun Links(items: List<NotesLink>, links: SafeLinks, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1), modifier = modifier) {
        Text(
            text = stringResource(Res.string.show_notes_links),
            style = DamaskTextStyles.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.semantics { heading() }.padding(bottom = Spacing.Space1),
        )
        items.forEach { link ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(Spacing.Space2),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Sizes.MinTouchTarget)
                    .clickable(role = Role.Button) { links.open(link.url) },
            ) {
                val colors = MaterialTheme.colorScheme
                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, tint = colors.primary)
                Column(Modifier.weight(1f)) {
                    Text(link.label, style = DamaskTextStyles.Body, color = colors.primary, maxLines = 2)
                    Text(link.host, style = DamaskTextStyles.BodySmall, color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

private fun NotesBlock.toAnnotatedString(linkStyles: TextLinkStyles, listener: LinkInteractionListener) =
    buildAnnotatedString {
        if (bullet) append(BULLET)
        spans.forEach { span ->
            val style = SpanStyle(
                fontWeight = if (span.bold) FontWeight.Bold else null,
                fontStyle = if (span.italic) FontStyle.Italic else null,
            )
            withStyle(style) {
                val link = span.link
                if (link == null) {
                    append(span.text)
                } else {
                    withLink(LinkAnnotation.Url(link, linkStyles, listener)) { append(span.text) }
                }
            }
        }
    }

/** Opens show-notes links. Presentation only passes http, https and mailto; check again here. */
private class SafeLinks(private val openLink: (String) -> Unit) : LinkInteractionListener {
    override fun onClick(link: LinkAnnotation) {
        (link as? LinkAnnotation.Url)?.url?.let(::open)
    }

    fun open(url: String) {
        val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
        if (scheme in OPENABLE_SCHEMES) openLink(url)
    }
}
