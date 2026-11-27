package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.LinkInteractionListener
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeUiState
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeViewModel
import uk.co.kilfeder.damask.podcasts.presentation.episodes.NotesBlock
import uk.co.kilfeder.damask.podcasts.ui.components.AmbientGlow
import uk.co.kilfeder.damask.podcasts.ui.components.ExactValueText
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_gone_body
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_gone_title
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_no_notes
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_played
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_unplayed
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** Link schemes show notes may open; anything else (javascript:, intent:, file:) is ignored. */
private val OPENABLE_SCHEMES = setOf("http", "https", "mailto")

/** Starts a bulleted item in show notes. Not user-facing copy: a typographic mark. */
private const val BULLET = "\u2022  "

/**
 * One episode: its artwork with the ambient glow, show, title, date and length, Mark played or
 * unplayed, then the show notes with working links. Playback controls join it with Now Playing.
 */
@Composable
internal fun EpisodeScreen(
    graph: AppGraph,
    id: EpisodeId,
    modifier: Modifier = Modifier,
    viewModel: EpisodeViewModel = viewModel(key = "${id.showId}/${id.guid}") { graph.episodeViewModel(id) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    EpisodeContent(
        state = state,
        onPlayedChange = { played -> if (played) viewModel.onMarkPlayed() else viewModel.onMarkUnplayed() },
        modifier = modifier,
    )
}

@Composable
private fun EpisodeContent(
    state: EpisodeUiState,
    onPlayedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.loading) {
        Loading(modifier)
        return
    }
    val episode = state.episode
    if (episode == null) {
        EmptyState(
            title = stringResource(Res.string.episode_gone_title),
            body = stringResource(Res.string.episode_gone_body),
            modifier = modifier,
        )
        return
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Space4),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.Space4, vertical = Spacing.Space6),
    ) {
        AmbientGlow(artworkUrl = episode.artworkUrl, shape = MaterialTheme.shapes.medium) {
            AsyncImage(
                model = episode.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(Sizes.EpisodeArtwork),
            )
        }
        Header(episode, Modifier.fillMaxWidth())
        FilledTonalButton(onClick = { onPlayedChange(!state.isPlayed) }) {
            Text(stringResource(if (state.isPlayed) Res.string.mark_unplayed else Res.string.mark_played))
        }
        Notes(state.notes, Modifier.fillMaxWidth())
    }
}

@Composable
private fun Header(episode: EpisodeRowUi, modifier: Modifier = Modifier) {
    val variant = MaterialTheme.colorScheme.onSurfaceVariant
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space2), modifier = modifier) {
        Text(episode.showTitle, style = DamaskTextStyles.Label, color = MaterialTheme.colorScheme.primary)
        Text(
            text = episode.title,
            style = DamaskTextStyles.TitleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        ExactValueText(value = episode.published, style = DamaskTextStyles.Time, color = variant)
        episode.length?.let { ExactValueText(value = it, style = DamaskTextStyles.Time, color = variant) }
        episode.progress?.let { progress ->
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun Notes(blocks: List<NotesBlock>, modifier: Modifier = Modifier) {
    if (blocks.isEmpty()) {
        Text(
            text = stringResource(Res.string.episode_no_notes),
            style = DamaskTextStyles.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier,
        )
        return
    }
    val uriHandler = LocalUriHandler.current
    val linkStyles = TextLinkStyles(
        SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline),
    )
    val listener = remember(uriHandler) { SafeLinks(uriHandler) }
    SelectionContainer(modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space3)) {
            blocks.forEach { block ->
                val text = remember(block, linkStyles) { block.toAnnotatedString(linkStyles, listener) }
                Text(text = text, style = DamaskTextStyles.Body, color = MaterialTheme.colorScheme.onSurface)
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

/** Opens a link from show notes. Presentation only passes http, https and mailto; check again. */
private class SafeLinks(private val uriHandler: UriHandler) : LinkInteractionListener {
    override fun onClick(link: LinkAnnotation) {
        val url = (link as? LinkAnnotation.Url)?.url ?: return
        val scheme = url.substringBefore(':', missingDelimiterValue = "").lowercase()
        if (scheme in OPENABLE_SCHEMES) runCatching { uriHandler.openUri(url) }
    }
}
