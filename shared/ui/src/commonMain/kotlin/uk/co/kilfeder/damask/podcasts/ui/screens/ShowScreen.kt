package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowRowUi
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowUiState
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.Artwork
import uk.co.kilfeder.damask.podcasts.ui.components.EpisodeRow
import uk.co.kilfeder.damask.podcasts.ui.components.GenreChip
import uk.co.kilfeder.damask.podcasts.ui.components.RowPlayer
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_gone_body
import uk.co.kilfeder.damask.podcasts.ui.resources.show_episodes
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_untitled
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * A show's page: its artwork, name and genre, then every episode in the show's own order. Playing
 * from here keeps the show's context, so autoplay carries on through it. A first version, until
 * the design hand-off has a Show page (per-show settings will live here).
 */
@Composable
internal fun ShowScreen(
    graph: AppGraph,
    showId: String,
    onOpenEpisode: (EpisodeId, PlaySource) -> Unit,
    rowPlayer: RowPlayer,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surface,
    viewModel: ShowViewModel = viewModel(key = "show-$showId") { graph.showViewModel(showId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShowContent(state, showId, onOpenEpisode, rowPlayer, background, modifier)
}

@Composable
private fun ShowContent(
    state: ShowUiState,
    showId: String,
    onOpenEpisode: (EpisodeId, PlaySource) -> Unit,
    rowPlayer: RowPlayer,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val show = state.show
    when {
        state.loading -> Loading(modifier)
        show == null -> EmptyState(
            icon = Icons.Rounded.Podcasts,
            title = stringResource(Res.string.shows_untitled),
            body = stringResource(Res.string.episode_gone_body),
            modifier = modifier,
        )
        else -> LazyColumn(modifier) {
            item(key = "header") { ShowHeader(show, state.episodes.size) }
            items(state.episodes, key = { it.id.guid }) { row ->
                val source = PlaySource.Show(showId)
                EpisodeRow(
                    row = row,
                    playing = rowPlayer.isPlaying(row.id),
                    onClick = { onOpenEpisode(row.id, source) },
                    onPlayPause = { rowPlayer.playPause(row.id, source) },
                    background = background,
                )
            }
        }
    }
}

@Composable
private fun ShowHeader(show: ShowRowUi, episodeCount: Int, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space4),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(Spacing.Space4),
    ) {
        Artwork(show.artworkUrl, show.fallback, size = Sizes.ShowHeaderArtwork)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1)) {
            show.genre?.let { GenreChip(it) }
            Text(
                text = show.title.ifBlank { stringResource(Res.string.shows_untitled) },
                style = DamaskTextStyles.TitleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = pluralStringResource(Res.plurals.show_episodes, episodeCount, episodeCount),
                style = DamaskTextStyles.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            // The chip above already names the genre.
            ShowMeta(show.copy(genre = null))
        }
    }
}
