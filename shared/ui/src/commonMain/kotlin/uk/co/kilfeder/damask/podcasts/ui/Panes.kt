package uk.co.kilfeder.damask.podcasts.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.AppUiState
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Destination
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Tab
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.ui.components.MiniPlayer
import uk.co.kilfeder.damask.podcasts.ui.components.PaneHeader
import uk.co.kilfeder.damask.podcasts.ui.components.RowPlayer
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.genre_pick_body
import uk.co.kilfeder.damask.podcasts.ui.resources.genre_pick_title
import uk.co.kilfeder.damask.podcasts.ui.resources.now_playing_empty_body
import uk.co.kilfeder.damask.podcasts.ui.resources.now_playing_empty_title
import uk.co.kilfeder.damask.podcasts.ui.resources.show_pick_body
import uk.co.kilfeder.damask.podcasts.ui.resources.show_pick_title
import uk.co.kilfeder.damask.podcasts.ui.screens.EmptyState
import uk.co.kilfeder.damask.podcasts.ui.screens.GenreQueueScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.GenresScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.HomeScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.ImportScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.InProgressScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.NowPlayingScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.PlayedScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.SearchScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.SettingsScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.ShowScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.ShowsScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.ShowNotesScreen
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** On wide layouts Home and Played show the playing episode beside the list. */
private fun Tab.pairsWithPlayer() = this == Tab.HOME || this == Tab.PLAYED

/**
 * Wide layouts with a list and a detail pane (TabletHome, TabletGenres artboards). These have no
 * app bar: each pane has its own heading, and Settings is at the bottom of the rail.
 */
internal fun isTwoPaneLayout(state: AppUiState, width: WidthClass): Boolean {
    if (width != WidthClass.EXPANDED) return false
    val top = state.nav.top
    val tab = state.nav.tab
    return when {
        top == null -> tab.pairsWithPlayer() || tab == Tab.GENRES || tab == Tab.SHOWS
        top is Destination.GenreQueue -> tab == Tab.GENRES
        top is Destination.Show -> tab == Tab.SHOWS
        top is Destination.ShowNotes -> tab.pairsWithPlayer()
        else -> false
    }
}

/** Whether the playing episode is already on screen (so the mini-player would only repeat it). */
internal fun playingIsOnScreen(state: AppUiState, width: WidthClass): Boolean {
    val top = state.nav.top
    return (top is Destination.Episode && top.id == state.playing) ||
        (top is Destination.ShowNotes && top.id == state.playing) ||
        // Two-pane layouts show it in a pane, or (Genres) put the mini-player in the detail card.
        isTwoPaneLayout(state, width)
}

/**
 * The content under the top bar. Expanded layouts pair a list with a detail pane (spec › Tablets
 * and window sizes): Home and Played with the playing episode (or its show notes), Genres with
 * the selected queue.
 * Everything else is one pane: lists and forms at a readable width, the episode screen as wide as
 * it likes (its notes move beside the player).
 */
@Composable
internal fun ShellContent(
    state: AppUiState,
    graph: AppGraph,
    width: WidthClass,
    actions: ShellActions,
    padding: PaddingValues,
) {
    val top = state.nav.top
    val tab = state.nav.tab
    val modifier = Modifier.padding(padding).fillMaxSize()
    if (!isTwoPaneLayout(state, width)) {
        SinglePane(state, graph, actions, modifier)
        return
    }
    val title = stringResource(tab.label)
    when {
        tab == Tab.GENRES -> {
            val genre = (top as? Destination.GenreQueue)?.categoryId
            TwoPanes(
                left = { ListPane(title, it) { m -> GenresScreen(graph, actions.onOpenGenre, m, selectedId = genre) } },
                right = { GenrePane(genre, state, graph, actions, it) },
                modifier = modifier,
            )
        }
        tab == Tab.SHOWS -> {
            val show = (top as? Destination.Show)?.showId
            TwoPanes(
                left = {
                    ListPane(title, it) { m ->
                        val search = { actions.onSelectTab(Tab.SEARCH) }
                        ShowsScreen(graph, actions.onOpenShow, search, m, selectedId = show)
                    }
                },
                right = { ShowPane(show, graph, actions, it) },
                modifier = modifier,
            )
        }
        // The notes peek in the player pane opens the notes in the same pane; back returns to it.
        top is Destination.ShowNotes -> TwoPanes(
            left = { ListPane(title, it) { m -> TabContent(tab, graph, actions, m) } },
            right = { ShowNotesScreen(graph, top.id, onClose = actions.onBack, modifier = it) },
            modifier = modifier,
        )
        else -> TwoPanes(
            left = { ListPane(title, it) { m -> TabContent(tab, graph, actions, m) } },
            right = { PlayingPane(state.playing, graph, actions, it) },
            modifier = modifier,
        )
    }
}

@Composable
private fun SinglePane(state: AppUiState, graph: AppGraph, actions: ShellActions, modifier: Modifier = Modifier) {
    val top = state.nav.top
    Box(contentAlignment = Alignment.TopCenter, modifier = modifier) {
        val readable = Modifier.widthIn(max = Sizes.MaxContentWidth).fillMaxSize()
        when (top) {
            is Destination.GenreQueue -> GenreQueueScreen(
                graph = graph,
                categoryId = top.categoryId,
                onOpenEpisode = { actions.onPlayEpisode(it, PlaySource.Genre(top.categoryId)) },
                rowPlayer = actions.rowPlayer,
                modifier = readable,
                showHeader = true,
                onBack = actions.onBack,
            )
            is Destination.Episode -> EpisodeScreen(top.id, graph, actions, Modifier.fillMaxSize())
            is Destination.Show -> ShowScreen(
                graph = graph,
                showId = top.showId,
                onOpenEpisode = actions.onPlayEpisode,
                rowPlayer = actions.rowPlayer,
                modifier = readable,
            )
            Destination.InProgress ->
                InProgressScreen(graph, actions.onPlayEpisode, actions.rowPlayer, modifier = readable)
            is Destination.ShowNotes -> ShowNotesScreen(graph, top.id, onClose = actions.onBack, modifier = readable)
            Destination.Settings ->
                SettingsScreen(graph, onImport = { actions.onNavigate(Destination.Import) }, modifier = readable)
            Destination.Import -> ImportScreen(graph, onFinish = actions.onImportFinish, modifier = readable)
            null -> TabContent(state.nav.tab, graph, actions, readable)
        }
    }
}

@Composable
private fun TabContent(tab: Tab, graph: AppGraph, actions: ShellActions, modifier: Modifier = Modifier) {
    when (tab) {
        Tab.HOME -> HomeScreen(
            graph = graph,
            onSearch = { actions.onSelectTab(Tab.SEARCH) },
            onImport = { actions.onNavigate(Destination.Import) },
            onOpenEpisode = actions.onPlayEpisode,
            onSeeAllInProgress = { actions.onNavigate(Destination.InProgress) },
            rowPlayer = actions.rowPlayer,
            modifier = modifier,
        )
        Tab.GENRES -> GenresScreen(graph, onOpenGenre = actions.onOpenGenre, modifier = modifier)
        Tab.SHOWS -> ShowsScreen(
            graph = graph,
            onOpenShow = actions.onOpenShow,
            onSearch = { actions.onSelectTab(Tab.SEARCH) },
            modifier = modifier,
        )
        Tab.PLAYED -> PlayedScreen(
            graph = graph,
            onOpenEpisode = { actions.onPlayEpisode(it, PlaySource.Played) },
            rowPlayer = actions.rowPlayer,
            modifier = modifier,
        )
        Tab.SEARCH -> SearchScreen(modifier)
    }
}

@Composable
private fun EpisodeScreen(id: EpisodeId, graph: AppGraph, actions: ShellActions, modifier: Modifier = Modifier) {
    NowPlayingScreen(
        graph = graph,
        id = id,
        onOpenGenre = actions.onOpenGenre,
        onOpenNotes = { actions.onNavigate(Destination.ShowNotes(id)) },
        modifier = modifier,
    )
}

/** The right-hand pane beside Home and Played: whatever's playing, at most [Sizes.PlayerPaneMaxWidth]. */
@Composable
private fun PlayingPane(playing: EpisodeId?, graph: AppGraph, actions: ShellActions, modifier: Modifier = Modifier) {
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
private fun ShowPane(showId: String?, graph: AppGraph, actions: ShellActions, modifier: Modifier = Modifier) {
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
private fun GenrePane(
    categoryId: String?,
    state: AppUiState,
    graph: AppGraph,
    actions: ShellActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        val content = Modifier.weight(1f).fillMaxWidth()
        if (categoryId == null) {
            EmptyState(
                icon = Icons.Rounded.Category,
                title = stringResource(Res.string.genre_pick_title),
                body = stringResource(Res.string.genre_pick_body),
                modifier = content,
            )
        } else {
            Box(contentAlignment = Alignment.TopStart, modifier = content) {
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
        if (state.playing != null) {
            MiniPlayer(graph, extendUnderGestureBar = false, onOpen = { actions.onNavigate(Destination.Episode(it)) })
        }
    }
}
