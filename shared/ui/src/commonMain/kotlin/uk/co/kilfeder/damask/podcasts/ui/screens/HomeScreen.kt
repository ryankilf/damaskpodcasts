package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.home.HomeUiState
import uk.co.kilfeder.damask.podcasts.presentation.home.HomeViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.EpisodeRow
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.home_empty
import uk.co.kilfeder.damask.podcasts.ui.resources.home_in_progress
import uk.co.kilfeder.damask.podcasts.ui.resources.home_up_next
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_played

@Composable
internal fun HomeScreen(
    graph: AppGraph,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel { graph.homeViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onRefresh = viewModel::onRefresh,
        onDismissEpisode = viewModel::onMarkPlayed,
        onLoadMore = viewModel::onLoadMore,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onDismissEpisode: (EpisodeId) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.loading) {
        Loading(modifier)
        return
    }
    val markPlayed = stringResource(Res.string.mark_played)
    PullToRefreshBox(isRefreshing = state.refreshing, onRefresh = onRefresh, modifier = modifier) {
        LazyColumn {
            if (state.inProgress.isNotEmpty()) {
                sectionHeader("in-progress", stringResourceKey = Res.string.home_in_progress)
                items(state.inProgress, key = { "p-${it.id.showId}-${it.id.guid}" }) { row ->
                    EpisodeRow(row)
                }
            }
            sectionHeader("up-next", stringResourceKey = Res.string.home_up_next)
            if (state.upNext.isEmpty()) {
                item(key = "empty") { EmptyMessage(stringResource(Res.string.home_empty)) }
            }
            items(state.upNext, key = { "u-${it.id.showId}-${it.id.guid}" }) { row ->
                EpisodeRow(row, swipeLabel = markPlayed, onSwipe = { onDismissEpisode(row.id) })
            }
            loadMore(state.canLoadMore, onLoadMore)
        }
    }
}
