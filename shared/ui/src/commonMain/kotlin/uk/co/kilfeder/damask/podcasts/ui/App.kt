package uk.co.kilfeder.damask.podcasts.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.ui.components.LocalShowMessage
import uk.co.kilfeder.damask.podcasts.ui.components.RowPlayer
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.ui.resources.show_unsubscribed
import uk.co.kilfeder.damask.podcasts.ui.resources.show_unsubscribed_untitled
import uk.co.kilfeder.damask.podcasts.ui.resources.undo
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.AppMessage
import uk.co.kilfeder.damask.podcasts.presentation.app.AppUiState
import uk.co.kilfeder.damask.podcasts.presentation.app.AppViewModel
import uk.co.kilfeder.damask.podcasts.presentation.app.Stage
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Destination
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Tab
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_added
import uk.co.kilfeder.damask.podcasts.ui.screens.Loading
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTheme

/** The whole app. Each platform entry point creates one [AppGraph] and passes it here. */
@Composable
fun App(
    graph: AppGraph,
    modifier: Modifier = Modifier,
    viewModel: AppViewModel = viewModel { graph.appViewModel() },
) {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(graph.imageHttpClient)) }
            // Artwork fades in everywhere once it arrives (not for memory-cache hits, which are instant).
            .crossfade(true)
            .build()
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DamaskTheme(useWallpaperColours = state.useWallpaperColours) {
        Surface(modifier.fillMaxSize()) {
            when (state.stage) {
                Stage.LOADING -> Loading()
                Stage.MAIN -> Shell(
                    state = state,
                    graph = graph,
                    intents = AppIntents(
                        onSelectTab = viewModel::onSelectTab,
                        onNavigate = viewModel::onNavigate,
                        onOpenEpisode = viewModel::onOpenEpisode,
                        onRowPlayPause = viewModel::onRowPlayPause,
                        onOpenGenre = viewModel::onOpenGenre,
                        onOpenShow = viewModel::onOpenShow,
                        onUnsubscribe = viewModel::onUnsubscribe,
                        onUndoUnsubscribe = viewModel::onUndoUnsubscribe,
                        onBack = viewModel::onBack,
                        onImportFinish = viewModel::onImportFinished,
                        onMessageShow = viewModel::onMessageShown,
                    ),
                )
            }
        }
    }
}

/** Everything the shell can ask of [AppViewModel]. */
private class AppIntents(
    val onSelectTab: (Tab) -> Unit,
    val onNavigate: (Destination) -> Unit,
    val onOpenEpisode: (EpisodeId, PlaySource?) -> Unit,
    val onRowPlayPause: (EpisodeId, PlaySource) -> Unit,
    val onOpenGenre: (String) -> Unit,
    val onOpenShow: (String) -> Unit,
    val onUnsubscribe: (String, String) -> Unit,
    val onUndoUnsubscribe: (String) -> Unit,
    val onBack: () -> Boolean,
    val onImportFinish: (Int) -> Unit,
    val onMessageShow: () -> Unit,
)

/**
 * Navigation and panes for every window width (spec › Tablets and window sizes): bottom tabs on
 * phones, a rail from [WidthClass.MEDIUM], and two panes from [WidthClass.EXPANDED].
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun Shell(state: AppUiState, graph: AppGraph, intents: AppIntents, modifier: Modifier = Modifier) {
    val top = state.nav.top
    val snackbars = remember { SnackbarHostState() }
    ShowMessages(state.message, snackbars, intents.onMessageShow, intents.onUndoUnsubscribe)
    BackHandler(enabled = top != null) { intents.onBack() }
    val scope = rememberCoroutineScope()
    val showMessage: (String) -> Unit = remember(snackbars, scope) {
        { text -> scope.launch { snackbars.showSnackbar(text) } }
    }
    CompositionLocalProvider(LocalShowMessage provides showMessage) {
        ShellLayout(state, graph, intents, snackbars, modifier)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShellLayout(
    state: AppUiState,
    graph: AppGraph,
    intents: AppIntents,
    snackbars: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val top = state.nav.top
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width = widthClassOf(maxWidth)
        val actions = ShellActions(
            onSelectTab = intents.onSelectTab,
            onNavigate = intents.onNavigate,
            onOpenEpisode = intents.onOpenEpisode,
            onOpenGenre = intents.onOpenGenre,
            onOpenShow = intents.onOpenShow,
            onUnsubscribe = intents.onUnsubscribe,
            onImportFinish = intents.onImportFinish,
            onBack = { intents.onBack() },
            rowPlayer = RowPlayer(state.playing, state.playingNow, intents.onRowPlayPause),
        )
        Row(Modifier.fillMaxSize()) {
            val compact = width == WidthClass.COMPACT
            val onSettings = { intents.onNavigate(Destination.Settings) }
            if (!compact) TabRail(state.nav.tab, intents.onSelectTab, onSettings)
            // Two-pane layouts have no app bar: each pane has its own heading (TabletGenres artboard).
            val panes = isTwoPaneLayout(state, width)
            Scaffold(
                bottomBar = {
                    if (!panes) {
                        BottomBars(
                            graph = graph,
                            tab = state.nav.tab,
                            showTabs = compact,
                            playingShown = playingIsOnScreen(state, width),
                            onSelectTab = intents.onSelectTab,
                            onNavigate = intents.onNavigate,
                        )
                    }
                },
                snackbarHost = { SnackbarHost(snackbars) },
            ) { padding -> ShellContent(state, graph, width, actions, padding) }
        }
    }
}

@Composable
private fun ShowMessages(
    message: AppMessage?,
    snackbars: SnackbarHostState,
    onMessageShow: () -> Unit,
    onUndoUnsubscribe: (String) -> Unit,
) {
    val messageShown by rememberUpdatedState(onMessageShow)
    val undoUnsubscribe by rememberUpdatedState(onUndoUnsubscribe)
    message ?: return
    val text = when (message) {
        is AppMessage.ShowsAdded -> pluralStringResource(Res.plurals.shows_added, message.count, message.count)
        is AppMessage.Unsubscribed -> if (message.title.isBlank()) {
            stringResource(Res.string.show_unsubscribed_untitled)
        } else {
            stringResource(Res.string.show_unsubscribed, message.title)
        }
    }
    val undo = stringResource(Res.string.undo)
    LaunchedEffect(message) {
        messageShown()
        when (message) {
            is AppMessage.ShowsAdded -> snackbars.showSnackbar(text)
            is AppMessage.Unsubscribed -> {
                val result = snackbars.showSnackbar(text, actionLabel = undo, duration = SnackbarDuration.Long)
                if (result == SnackbarResult.ActionPerformed) undoUnsubscribe(message.showId)
            }
        }
    }
}

/** What the shell's screens can ask for. */
internal class ShellActions(
    val onSelectTab: (Tab) -> Unit,
    val onNavigate: (Destination) -> Unit,
    /** Opens an episode's screen without playing it. */
    val onOpenEpisode: (EpisodeId, PlaySource?) -> Unit,
    val onOpenGenre: (String) -> Unit,
    val onOpenShow: (String) -> Unit,
    val onUnsubscribe: (String, String) -> Unit,
    val onImportFinish: (Int) -> Unit,
    val onBack: () -> Unit,
    val rowPlayer: RowPlayer,
)
