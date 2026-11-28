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
import org.jetbrains.compose.resources.pluralStringResource
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
                        onPlayEpisode = viewModel::onPlayEpisode,
                        onOpenGenre = viewModel::onOpenGenre,
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
    val onPlayEpisode: (EpisodeId, PlaySource, Boolean) -> Unit,
    val onOpenGenre: (String) -> Unit,
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
    ShowMessages(state.message, snackbars, intents.onMessageShow)
    BackHandler(enabled = top != null) { intents.onBack() }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width = widthClassOf(maxWidth)
        val actions = ShellActions(
            onSelectTab = intents.onSelectTab,
            onNavigate = intents.onNavigate,
            onPlayEpisode = { id, source -> intents.onPlayEpisode(id, source, width != WidthClass.EXPANDED) },
            onOpenGenre = intents.onOpenGenre,
            onImportFinish = intents.onImportFinish,
            onBack = { intents.onBack() },
        )
        Row(Modifier.fillMaxSize()) {
            if (width != WidthClass.COMPACT) TabRail(state.nav.tab, intents.onSelectTab)
            Scaffold(
                topBar = {
                    TopBar(
                        title = titleOf(state),
                        showBack = top != null,
                        showSettings = top == null,
                        onBack = { intents.onBack() },
                        onSettings = { intents.onNavigate(Destination.Settings) },
                    )
                },
                bottomBar = {
                    BottomBars(
                        graph = graph,
                        tab = state.nav.tab,
                        showTabs = width == WidthClass.COMPACT,
                        playingShown = playingIsOnScreen(state, width),
                        onSelectTab = intents.onSelectTab,
                        onNavigate = intents.onNavigate,
                    )
                },
                snackbarHost = { SnackbarHost(snackbars) },
            ) { padding -> ShellContent(state, graph, width, actions, padding) }
        }
    }
}

@Composable
private fun ShowMessages(message: AppMessage?, snackbars: SnackbarHostState, onMessageShow: () -> Unit) {
    val messageShown by rememberUpdatedState(onMessageShow)
    message ?: return
    val text = when (message) {
        is AppMessage.ShowsAdded -> pluralStringResource(Res.plurals.shows_added, message.count, message.count)
    }
    LaunchedEffect(message) {
        messageShown()
        snackbars.showSnackbar(text)
    }
}
