package uk.co.kilfeder.damask.podcasts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.AppUiState
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Destination
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.ui.components.FadeThrough
import uk.co.kilfeder.damask.podcasts.ui.components.MiniPlayer
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.genre_pick_body
import uk.co.kilfeder.damask.podcasts.ui.resources.genre_pick_title
import uk.co.kilfeder.damask.podcasts.ui.resources.now_playing_empty_body
import uk.co.kilfeder.damask.podcasts.ui.resources.now_playing_empty_title
import uk.co.kilfeder.damask.podcasts.ui.resources.show_pick_body
import uk.co.kilfeder.damask.podcasts.ui.resources.show_pick_title
import uk.co.kilfeder.damask.podcasts.ui.screens.EmptyState
import uk.co.kilfeder.damask.podcasts.ui.screens.GenreQueueScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.ShowScreen
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

/** The right-hand pane beside Home and Played: whatever's playing, at most [Sizes.PlayerPaneMaxWidth]. */
@Composable
internal fun PlayingPane(playing: EpisodeId?, graph: AppGraph, actions: ShellActions, modifier: Modifier = Modifier) {
    if (playing == null) {
        EmptyState(
            icon = Icons.Rounded.PlayCircle,
            title = stringResource(Res.string.now_playing_empty_title),
            body = stringResource(Res.string.now_playing_empty_body),
            modifier = modifier,
        )
        return
    }
    Box(contentAlignment = Alignment.TopCenter, modifier = modifier) {
        EpisodeScreen(playing, graph, actions, Modifier.widthIn(max = Sizes.PlayerPaneMaxWidth).fillMaxSize())
    }
}

/** The right-hand pane beside Shows: the selected show's page. */
@Composable
internal fun ShowPane(showId: String?, graph: AppGraph, actions: ShellActions, modifier: Modifier = Modifier) {
    if (showId == null) {
        EmptyState(
            icon = Icons.Rounded.Podcasts,
            title = stringResource(Res.string.show_pick_title),
            body = stringResource(Res.string.show_pick_body),
            modifier = modifier,
        )
        return
    }
    Box(contentAlignment = Alignment.TopStart, modifier = modifier) {
        ShowScreen(
            graph = graph,
            showId = showId,
            onOpenEpisode = actions.onPlayEpisode,
            rowPlayer = actions.rowPlayer,
            modifier = Modifier.widthIn(max = Sizes.MaxContentWidth).fillMaxSize(),
            background = MaterialTheme.colorScheme.surfaceContainerLow,
        )
    }
}

/** The right-hand pane beside Genres: the selected genre's queue, with the mini-player at its foot. */
@Composable
internal fun GenrePane(
    categoryId: String?,
    state: AppUiState,
    graph: AppGraph,
    actions: ShellActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        // The queue fades when the genre changes; the mini-player under it stays put.
        FadeThrough(categoryId, { it }, Modifier.weight(1f).fillMaxWidth()) { shown ->
            GenreDetail(shown, actions, graph)
        }
        if (state.playing != null) {
            MiniPlayer(graph, extendUnderGestureBar = false, onOpen = { actions.onNavigate(Destination.Episode(it)) })
        }
    }
}

@Composable
private fun GenreDetail(categoryId: String?, actions: ShellActions, graph: AppGraph, modifier: Modifier = Modifier) {
    if (categoryId == null) {
        EmptyState(
            icon = Icons.Rounded.Category,
            title = stringResource(Res.string.genre_pick_title),
            body = stringResource(Res.string.genre_pick_body),
            modifier = modifier.fillMaxSize(),
        )
        return
    }
    Box(contentAlignment = Alignment.TopStart, modifier = modifier.fillMaxSize()) {
        GenreQueueScreen(
            graph = graph,
            categoryId = categoryId,
            onOpenEpisode = { actions.onPlayEpisode(it, PlaySource.Genre(categoryId)) },
            rowPlayer = actions.rowPlayer,
            modifier = Modifier.widthIn(max = Sizes.MaxContentWidth).fillMaxSize(),
            showHeader = true,
        )
    }
}
