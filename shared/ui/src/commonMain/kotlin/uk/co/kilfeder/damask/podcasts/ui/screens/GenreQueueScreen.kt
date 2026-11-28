package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.ui.components.RowPlayer
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreQueueUiState
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreQueueViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.EpisodeRow
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.genre_queue_empty
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_played

@Composable
internal fun GenreQueueScreen(
    graph: AppGraph,
    categoryId: String,
    onOpenEpisode: (EpisodeId) -> Unit,
    rowPlayer: RowPlayer,
    modifier: Modifier = Modifier,
    viewModel: GenreQueueViewModel = viewModel(key = "genre-$categoryId") { graph.genreQueueViewModel(categoryId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GenreQueueContent(
        state = state,
        onDismissEpisode = viewModel::onMarkPlayed,
        onLoadMore = viewModel::onLoadMore,
        onOpenEpisode = onOpenEpisode,
        rowPlayer = rowPlayer,
        categoryId = categoryId,
        modifier = modifier,
    )
}

@Composable
private fun GenreQueueContent(
    state: GenreQueueUiState,
    onDismissEpisode: (EpisodeId) -> Unit,
    onLoadMore: () -> Unit,
    onOpenEpisode: (EpisodeId) -> Unit,
    rowPlayer: RowPlayer,
    categoryId: String,
    modifier: Modifier = Modifier,
) {
    if (state.loading) {
        Loading(modifier)
        return
    }
    val markPlayed = stringResource(Res.string.mark_played)
    LazyColumn(modifier) {
        if (state.episodes.isEmpty()) {
            item(key = "empty") { EmptyMessage(stringResource(Res.string.genre_queue_empty)) }
        }
        items(state.episodes, key = { "${it.id.showId}-${it.id.guid}" }) { row ->
            EpisodeRow(
                row = row,
                playing = rowPlayer.isPlaying(row.id),
                onClick = { onOpenEpisode(row.id) },
                onPlayPause = { rowPlayer.playPause(row.id, PlaySource.Genre(categoryId)) },
                swipeLabel = markPlayed,
                onSwipe = { onDismissEpisode(row.id) },
            )
        }
        loadMore(state.canLoadMore, onLoadMore)
    }
}
