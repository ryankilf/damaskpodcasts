package uk.co.kilfeder.damask.podcasts.ui

import uk.co.kilfeder.damask.podcasts.ui.components.ManageGenresButton
import uk.co.kilfeder.damask.podcasts.ui.screens.ManageGenresScreen
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.AppUiState
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Destination
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Tab
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.ui.components.FadeThrough
import uk.co.kilfeder.damask.podcasts.ui.screens.GenreQueueScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.GenresScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.HomeScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.ImportScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.InProgressScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.NowPlayingScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.PlayedScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.SearchScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.SettingsScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.ShowPageActions
import uk.co.kilfeder.damask.podcasts.ui.screens.ShowScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.ShowsScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.ShowNotesScreen
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

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
        top is Destination.ShowNotes || top is Destination.Episode -> tab.pairsWithPlayer()
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
    // One pane: every screen change fades. Two panes: changing tab fades the whole layout, and the
    // detail pane fades on its own when the selection changes.
    FadeThrough(
        state = state,
        screenOf = { if (isTwoPaneLayout(it, width)) it.nav.tab else it.nav.top ?: it.nav.tab },
        modifier = Modifier.padding(padding).fillMaxSize(),
    ) { shown ->
        if (isTwoPaneLayout(shown, width)) {
            PanesFor(shown, graph, actions, Modifier.fillMaxSize())
        } else {
            // The heading fades with its screen, so the old screen never jumps when it changes.
            Column(Modifier.fillMaxSize()) {
                if (!hasOwnHeading(shown)) {
                    TopBar(
                        title = titleOf(shown),
                        showBack = shown.nav.top != null,
                        onBack = actions.onBack,
                        action = topLevelAction(
                            state = shown,
                            compact = width == WidthClass.COMPACT,
                            onSettings = { actions.onNavigate(Destination.Settings) },
                            onManageGenres = { actions.onNavigate(Destination.ManageGenres) },
                        ),
                    )
                }
                SinglePane(shown, graph, actions, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PanesFor(state: AppUiState, graph: AppGraph, actions: ShellActions, modifier: Modifier = Modifier) {
    val top = state.nav.top
    val tab = state.nav.tab
    val title = stringResource(tab.label)
    when {
        tab == Tab.GENRES -> {
            val genre = (top as? Destination.GenreQueue)?.categoryId
            TwoPanes(
                left = {
                    val manage = { actions.onNavigate(Destination.ManageGenres) }
                    ListPane(title, it, actions = { ManageGenresButton(manage) }) { m ->
                        GenresScreen(graph, actions.onOpenGenre, m, selectedId = genre)
                    }
                },
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
                right = { m ->
                    FadeThrough(show, { it }, m) { ShowPane(it, graph, actions, Modifier.fillMaxSize()) }
                },
                modifier = modifier,
            )
        }
        // The notes peek in the player pane opens the notes in the same pane; back returns to it.
        // Notes and the player swap in the right pane; both are keyed on the episode, so changing
        // episode fades too.
        else -> TwoPanes(
            left = { ListPane(title, it) { m -> TabContent(tab, graph, actions, m) } },
            right = { m ->
                // An opened episode, its notes, or else whatever's playing.
                val shownTop = top as? Destination.ShowNotes ?: top as? Destination.Episode
                FadeThrough(shownTop ?: state.playing, { it }, m) { shown ->
                    val fill = Modifier.fillMaxSize()
                    when (shown) {
                        is Destination.ShowNotes -> ShowNotesScreen(graph, shown.id, actions.onBack, fill)
                        is Destination.Episode -> PlayingPane(shown.id, graph, actions, fill, shown.source)
                        is EpisodeId -> PlayingPane(shown, graph, actions, fill)
                        else -> PlayingPane(null, graph, actions, fill)
                    }
                }
            },
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
                onOpenEpisode = { actions.onOpenEpisode(it, PlaySource.Genre(top.categoryId)) },
                rowPlayer = actions.rowPlayer,
                modifier = readable,
                showHeader = true,
                onBack = actions.onBack,
                onLeave = actions.onBack,
            )
            is Destination.Episode -> EpisodeScreen(top.id, graph, actions, Modifier.fillMaxSize(), top.source)
            is Destination.Show -> ShowScreen(
                graph = graph,
                showId = top.showId,
                actions = ShowPageActions(actions.onOpenEpisode, actions.onUnsubscribe, onBack = actions.onBack),
                rowPlayer = actions.rowPlayer,
                modifier = readable,
            )
            Destination.InProgress ->
                InProgressScreen(graph, actions.onOpenEpisode, actions.rowPlayer, modifier = readable)
            is Destination.ShowNotes -> ShowNotesScreen(graph, top.id, onClose = actions.onBack, modifier = readable)
            Destination.Settings ->
                SettingsScreen(graph, onImport = { actions.onNavigate(Destination.Import) }, modifier = readable)
            Destination.Import -> ImportScreen(graph, onFinish = actions.onImportFinish, modifier = readable)
            Destination.ManageGenres -> ManageGenresScreen(graph, modifier = readable)
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
            onOpenEpisode = actions.onOpenEpisode,
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
            onOpenEpisode = { actions.onOpenEpisode(it, PlaySource.Played) },
            rowPlayer = actions.rowPlayer,
            modifier = modifier,
        )
        Tab.SEARCH -> SearchScreen(modifier)
    }
}

@Composable
internal fun EpisodeScreen(
    id: EpisodeId,
    graph: AppGraph,
    actions: ShellActions,
    modifier: Modifier = Modifier,
    source: PlaySource? = null,
) {
    NowPlayingScreen(
        graph = graph,
        id = id,
        onOpenGenre = actions.onOpenGenre,
        onOpenNotes = { actions.onNavigate(Destination.ShowNotes(id)) },
        modifier = modifier,
        source = source,
    )
}
