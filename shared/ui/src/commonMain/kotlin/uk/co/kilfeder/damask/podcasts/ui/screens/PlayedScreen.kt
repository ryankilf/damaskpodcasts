package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.key
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.ui.components.RowPlayer
import uk.co.kilfeder.damask.podcasts.presentation.played.PlayedUiState
import uk.co.kilfeder.damask.podcasts.presentation.played.PlayedViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.EpisodeRow
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_unplayed
import uk.co.kilfeder.damask.podcasts.ui.resources.played_empty
import uk.co.kilfeder.damask.podcasts.ui.resources.played_empty_title

@Composable
internal fun PlayedScreen(
    graph: AppGraph,
    onOpenEpisode: (EpisodeId) -> Unit,
    rowPlayer: RowPlayer,
    modifier: Modifier = Modifier,
    viewModel: PlayedViewModel = viewModel { graph.playedViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    PlayedContent(state, viewModel::onMarkUnplayed, onOpenEpisode, rowPlayer, modifier)
}

@Composable
private fun PlayedContent(
    state: PlayedUiState,
    onRestoreEpisode: (EpisodeId) -> Unit,
    onOpenEpisode: (EpisodeId) -> Unit,
    rowPlayer: RowPlayer,
    modifier: Modifier = Modifier,
) {
    if (state.loading) {
        Loading(modifier)
        return
    }
    if (state.rows.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.History,
            title = stringResource(Res.string.played_empty_title),
            body = stringResource(Res.string.played_empty),
            modifier = modifier,
        )
        return
    }
    val markUnplayed = stringResource(Res.string.mark_unplayed)
    LazyColumn(modifier) {
        items(state.rows, key = { it.episode.id.key }) { row ->
            // Only played episodes can be marked unplayed; in-progress ones are just listed.
            EpisodeRow(
                row = row.episode,
                player = rowPlayer,
                source = PlaySource.Played,
                onClick = { onOpenEpisode(row.episode.id) },
                swipeLabel = markUnplayed.takeIf { row.isPlayed },
                onSwipe = { onRestoreEpisode(row.episode.id) },
            )
        }
    }
}
