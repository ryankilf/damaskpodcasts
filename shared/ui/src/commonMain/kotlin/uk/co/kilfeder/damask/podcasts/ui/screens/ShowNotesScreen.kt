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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
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
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.show_notes
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
    modifier: Modifier = Modifier,
    viewModel: EpisodeViewModel = viewModel(key = episodeKey(id)) { graph.episodeViewModel(id) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Box(modifier) {
        EpisodeLoaded(state) { episode -> ShowNotesContent(state, episode) }
    }
}

@Composable
private fun ShowNotesContent(state: EpisodeUiState, episode: EpisodeRowUi, modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    val links = remember(uriHandler) { SafeLinks(uriHandler) }
    Column(modifier.fillMaxSize()) {
        Header(state, episode)
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Space4),
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceContainerLow,
                    RoundedCornerShape(topStart = Sizes.SheetCornerRadius, topEnd = Sizes.SheetCornerRadius),
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.Space6, vertical = Spacing.Space3),
        ) {
            SheetHandle(Modifier.align(Alignment.CenterHorizontally))
            Text(
                text = stringResource(Res.string.show_notes),
                style = DamaskTextStyles.TitleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Notes(state.notes, links)
            if (state.links.isNotEmpty()) Links(state.links, links)
        }
    }
}

@Composable
private fun Header(state: EpisodeUiState, episode: EpisodeRowUi, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = Spacing.Space3).padding(top = Spacing.Space2, bottom = Spacing.Space3)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Space3),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Spacing.Space3, end = Spacing.Space1),
        ) {
            Artwork(episode.artworkUrl, size = Sizes.NotesHeaderArtwork)
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
            PlayButton(
                size = Sizes.MinTouchTarget,
                iconSize = Sizes.Icon,
                pitch = Sizes.DamaskPitchSmall,
                enabled = false,
                onClick = {},
            )
        }
        state.position?.let {
            PositionBar(
                position = it,
                showThumb = false,
                modifier = Modifier.padding(horizontal = Spacing.Space3, vertical = Spacing.Space2),
            )
        }
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
private class SafeLinks(private val uriHandler: UriHandler) : LinkInteractionListener {
    override fun onClick(link: LinkAnnotation) {
        (link as? LinkAnnotation.Url)?.url?.let(::open)
    }

    fun open(url: String) {
        val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
        if (scheme in OPENABLE_SCHEMES) runCatching { uriHandler.openUri(url) }
    }
}
