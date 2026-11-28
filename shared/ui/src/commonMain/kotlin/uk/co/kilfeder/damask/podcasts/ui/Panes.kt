package uk.co.kilfeder.damask.podcasts.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.AppUiState
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Destination
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Tab
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.genre_pick_body
import uk.co.kilfeder.damask.podcasts.ui.resources.genre_pick_title
import uk.co.kilfeder.damask.podcasts.ui.resources.now_playing_empty_body
import uk.co.kilfeder.damask.podcasts.ui.resources.now_playing_empty_title
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
import uk.co.kilfeder.damask.podcasts.ui.screens.ShowNotesScreen
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

/** Material's window size classes, by width. */
internal enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

internal fun widthClassOf(width: Dp): WidthClass = when {
    width >= Sizes.ExpandedWidth -> WidthClass.EXPANDED
    width >= Sizes.MediumWidth -> WidthClass.MEDIUM
    else -> WidthClass.COMPACT
}

/** What the shell's screens can ask for. */
internal class ShellActions(
    val onSelectTab: (Tab) -> Unit,
    val onNavigate: (Destination) -> Unit,
    val onPlayEpisode: (EpisodeId, PlaySource) -> Unit,
    val onOpenGenre: (String) -> Unit,
    val onImportFinish: (Int) -> Unit,
    val onBack: () -> Unit,
)

/** On wide layouts Home and Played show the playing episode beside the list. */
private fun Tab.pairsWithPlayer() = this == Tab.HOME || this == Tab.PLAYED

/** Whether the playing episode is already on screen (so the mini-player would only repeat it). */
internal fun playingIsOnScreen(state: AppUiState, width: WidthClass): Boolean {
    val top = state.nav.top
    return (top is Destination.Episode && top.id == state.playing) ||
        (top is Destination.ShowNotes && top.id == state.playing) ||
        (width == WidthClass.EXPANDED && top == null && state.nav.tab.pairsWithPlayer())
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
    val wide = width == WidthClass.EXPANDED
    when {
        wide && top == null && tab.pairsWithPlayer() -> TwoPanes(
            left = { TabContent(tab, graph, actions, it) },
            right = { PlayingPane(state.playing, graph, actions, it) },
            modifier = modifier,
        )
        // The notes peek in that pane opens the notes in the same pane; back returns to the player.
        wide && top is Destination.ShowNotes && tab.pairsWithPlayer() -> TwoPanes(
            left = { TabContent(tab, graph, actions, it) },
            right = { ShowNotesScreen(graph, top.id, onClose = actions.onBack, modifier = it) },
            modifier = modifier,
        )
        wide && tab == Tab.GENRES && (top == null || top is Destination.GenreQueue) -> TwoPanes(
            left = { GenresScreen(graph, onOpenGenre = actions.onOpenGenre, modifier = it) },
            right = { GenrePane((top as? Destination.GenreQueue)?.categoryId, graph, actions, it) },
            modifier = modifier,
        )
        else -> SinglePane(state, graph, actions, modifier)
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
                modifier = readable,
            )
            is Destination.Episode -> EpisodeScreen(top.id, graph, actions, Modifier.fillMaxSize())
            Destination.InProgress ->
                InProgressScreen(graph, onOpenEpisode = actions.onPlayEpisode, modifier = readable)
            is Destination.ShowNotes -> ShowNotesScreen(graph, top.id, onClose = actions.onBack, modifier = readable)
            Destination.Settings ->
                SettingsScreen(graph, onImport = { actions.onNavigate(Destination.Import) }, modifier = readable)
            Destination.Import -> ImportScreen(graph, onFinish = actions.onImportFinish, modifier = readable)
            null -> TabContent(state.nav.tab, graph, actions, readable)
        }
    }
}

@Composable
private fun TwoPanes(
    left: @Composable (Modifier) -> Unit,
    right: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier) {
        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.weight(1f).fillMaxSize()) {
            left(Modifier.widthIn(max = Sizes.MaxContentWidth).fillMaxSize())
        }
        VerticalDivider()
        Box(Modifier.weight(1f).fillMaxSize()) { right(Modifier.fillMaxSize()) }
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
            modifier = modifier,
        )
        Tab.GENRES -> GenresScreen(graph, onOpenGenre = actions.onOpenGenre, modifier = modifier)
        Tab.PLAYED -> PlayedScreen(
            graph = graph,
            onOpenEpisode = { actions.onPlayEpisode(it, PlaySource.Played) },
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

/** The right-hand pane beside Home and Played: whatever's playing. */
@Composable
private fun PlayingPane(playing: EpisodeId?, graph: AppGraph, actions: ShellActions, modifier: Modifier = Modifier) {
    if (playing == null) {
        EmptyState(
            title = stringResource(Res.string.now_playing_empty_title),
            body = stringResource(Res.string.now_playing_empty_body),
            modifier = modifier,
        )
    } else {
        EpisodeScreen(playing, graph, actions, modifier)
    }
}

/** The right-hand pane beside Genres: the selected genre's queue. */
@Composable
private fun GenrePane(categoryId: String?, graph: AppGraph, actions: ShellActions, modifier: Modifier = Modifier) {
    if (categoryId == null) {
        EmptyState(
            title = stringResource(Res.string.genre_pick_title),
            body = stringResource(Res.string.genre_pick_body),
            modifier = modifier,
        )
    } else {
        GenreQueueScreen(
            graph = graph,
            categoryId = categoryId,
            onOpenEpisode = { actions.onPlayEpisode(it, PlaySource.Genre(categoryId)) },
            modifier = modifier,
        )
    }
}
