package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import uk.co.kilfeder.damask.podcasts.presentation.home.HomeUiState
import uk.co.kilfeder.damask.podcasts.presentation.home.HomeViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.EpisodeRow
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.home_empty
import uk.co.kilfeder.damask.podcasts.ui.resources.home_in_progress
import uk.co.kilfeder.damask.podcasts.ui.resources.home_no_shows_body
import uk.co.kilfeder.damask.podcasts.ui.resources.home_no_shows_title
import uk.co.kilfeder.damask.podcasts.ui.resources.home_search
import uk.co.kilfeder.damask.podcasts.ui.resources.home_caught_up_body
import uk.co.kilfeder.damask.podcasts.ui.resources.home_caught_up_title
import uk.co.kilfeder.damask.podcasts.ui.resources.home_check_now
import uk.co.kilfeder.damask.podcasts.ui.resources.import_subscriptions
import uk.co.kilfeder.damask.podcasts.ui.resources.home_up_next
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_played
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

@Composable
internal fun HomeScreen(
    graph: AppGraph,
    onSearch: () -> Unit,
    onImport: () -> Unit,
    onOpenEpisode: (EpisodeId, PlaySource) -> Unit,
    onSeeAllInProgress: () -> Unit,
    rowPlayer: RowPlayer,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel { graph.homeViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onRefresh = viewModel::onRefresh,
        onDismissEpisode = viewModel::onMarkPlayed,
        onLoadMore = viewModel::onLoadMore,
        onSearch = onSearch,
        onImport = onImport,
        onOpenEpisode = onOpenEpisode,
        onSeeAllInProgress = onSeeAllInProgress,
        rowPlayer = rowPlayer,
        modifier = modifier,
    )
}

/**
 * Home with nothing to show (design hand-off › Empty states): no shows yet (search, or import),
 * or every episode played (check for new ones).
 */
@Composable
private fun HomeEmpty(
    hasShows: Boolean,
    onSearch: () -> Unit,
    onImport: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (hasShows) {
        EmptyState(
            icon = Icons.Rounded.CheckCircle,
            title = stringResource(Res.string.home_caught_up_title),
            body = stringResource(Res.string.home_caught_up_body),
            modifier = modifier,
        ) {
            TextButton(onClick = onRefresh) { Text(stringResource(Res.string.home_check_now)) }
        }
    } else {
        EmptyState(
            icon = Icons.Rounded.Podcasts,
            title = stringResource(Res.string.home_no_shows_title),
            body = stringResource(Res.string.home_no_shows_body),
            modifier = modifier,
        ) {
            Button(onClick = onSearch) { Text(stringResource(Res.string.home_search)) }
            TextButton(onClick = onImport) { Text(stringResource(Res.string.import_subscriptions)) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onDismissEpisode: (EpisodeId) -> Unit,
    onLoadMore: () -> Unit,
    onSearch: () -> Unit,
    onImport: () -> Unit,
    onOpenEpisode: (EpisodeId, PlaySource) -> Unit,
    onSeeAllInProgress: () -> Unit,
    rowPlayer: RowPlayer,
    modifier: Modifier = Modifier,
) {
    if (state.loading) {
        Loading(modifier)
        return
    }
    if (!state.hasSubscriptions || (state.inProgress.isEmpty() && state.upNext.isEmpty())) {
        HomeEmpty(state.hasSubscriptions, onSearch, onImport, onRefresh, modifier)
        return
    }
    val markPlayed = stringResource(Res.string.mark_played)
    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn {
            if (state.inProgress.isNotEmpty()) {
                item(key = "in-progress") {
                    InProgressPanel(
                        rows = state.inProgress,
                        onOpenEpisode = onOpenEpisode,
                        onSeeAll = onSeeAllInProgress,
                        rowPlayer = rowPlayer,
                        modifier = Modifier.padding(top = Spacing.Space4),
                    )
                }
            }
            sectionHeader("up-next", stringResourceKey = Res.string.home_up_next)
            if (state.upNext.isEmpty()) {
                item(key = "empty") { EmptyMessage(stringResource(Res.string.home_empty)) }
            }
            items(state.upNext, key = { "u-${it.id.showId}-${it.id.guid}" }) { row ->
                EpisodeRow(
                    row = row,
                    playing = rowPlayer.isPlaying(row.id),
                    onClick = { onOpenEpisode(row.id, PlaySource.UpNext) },
                    onPlayPause = { rowPlayer.playPause(row.id, PlaySource.UpNext) },
                    swipeLabel = markPlayed,
                    onSwipe = { onDismissEpisode(row.id) },
                )
            }
            loadMore(state.canLoadMore, onLoadMore)
        }
    }
}
