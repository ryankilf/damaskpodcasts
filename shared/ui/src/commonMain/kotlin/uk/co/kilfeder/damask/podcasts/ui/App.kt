package uk.co.kilfeder.damask.podcasts.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import coil3.network.ktor3.KtorNetworkFetcherFactory
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.AppMessage
import uk.co.kilfeder.damask.podcasts.presentation.app.AppUiState
import uk.co.kilfeder.damask.podcasts.presentation.app.AppViewModel
import uk.co.kilfeder.damask.podcasts.presentation.app.Stage
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Destination
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Tab
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.app_name
import uk.co.kilfeder.damask.podcasts.ui.resources.back
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_title
import uk.co.kilfeder.damask.podcasts.ui.resources.import_screen_title
import uk.co.kilfeder.damask.podcasts.ui.resources.settings
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_added
import uk.co.kilfeder.damask.podcasts.ui.resources.tab_genres
import uk.co.kilfeder.damask.podcasts.ui.resources.tab_home
import uk.co.kilfeder.damask.podcasts.ui.resources.tab_played
import uk.co.kilfeder.damask.podcasts.ui.resources.tab_search
import uk.co.kilfeder.damask.podcasts.ui.screens.EpisodeScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.GenreQueueScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.GenresScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.HomeScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.ImportScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.Loading
import uk.co.kilfeder.damask.podcasts.ui.screens.PlayedScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.SearchScreen
import uk.co.kilfeder.damask.podcasts.ui.screens.SettingsScreen
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTheme
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

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
                    onSelectTab = viewModel::onSelectTab,
                    onNavigate = viewModel::onNavigate,
                    onBack = viewModel::onBack,
                    onImportFinish = viewModel::onImportFinished,
                    onMessageShow = viewModel::onMessageShown,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
private fun Shell(
    state: AppUiState,
    graph: AppGraph,
    onSelectTab: (Tab) -> Unit,
    onNavigate: (Destination) -> Unit,
    onBack: () -> Boolean,
    onImportFinish: (Int) -> Unit,
    onMessageShow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val top = state.nav.top
    val snackbars = remember { SnackbarHostState() }
    val messageShown by rememberUpdatedState(onMessageShow)
    state.message?.let { message ->
        val text = when (message) {
            is AppMessage.ShowsAdded -> pluralStringResource(Res.plurals.shows_added, message.count, message.count)
        }
        LaunchedEffect(message) {
            messageShown()
            snackbars.showSnackbar(text)
        }
    }
    BackHandler(enabled = top != null) { onBack() }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                title = titleOf(state),
                showBack = top != null,
                showSettings = top == null,
                onBack = { onBack() },
                onSettings = { onNavigate(Destination.Settings) },
            )
        },
        bottomBar = { TabBar(state.nav.tab, onSelectTab) },
        snackbarHost = { SnackbarHost(snackbars) },
    ) { padding ->
        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.padding(padding).fillMaxSize()) {
            val content = Modifier.widthIn(max = Sizes.MaxContentWidth).fillMaxSize()
            when {
                top is Destination.GenreQueue -> GenreQueueScreen(
                    graph = graph,
                    categoryId = top.categoryId,
                    onOpenEpisode = { onNavigate(Destination.Episode(it)) },
                    modifier = content,
                )
                top is Destination.Episode -> EpisodeScreen(graph, top.id, content)
                top == Destination.Settings ->
                    SettingsScreen(graph, onImport = { onNavigate(Destination.Import) }, content)
                top == Destination.Import -> ImportScreen(graph, onFinish = onImportFinish, modifier = content)
                else -> TabContent(state.nav.tab, graph, onNavigate, onSelectTab, content)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopBar(
    title: StringResource,
    showBack: Boolean,
    showSettings: Boolean,
    onBack: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        title = { Text(stringResource(title)) },
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(Res.string.back),
                    )
                }
            }
        },
        actions = {
            if (showSettings) {
                IconButton(onClick = onSettings) {
                    Icon(imageVector = Icons.Rounded.Settings, contentDescription = stringResource(Res.string.settings))
                }
            }
        },
    )
}

@Composable
private fun TabContent(
    tab: Tab,
    graph: AppGraph,
    onNavigate: (Destination) -> Unit,
    onSelectTab: (Tab) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (tab) {
        Tab.HOME -> HomeScreen(
            graph = graph,
            onSearch = { onSelectTab(Tab.SEARCH) },
            onImport = { onNavigate(Destination.Import) },
            onOpenEpisode = { onNavigate(Destination.Episode(it)) },
            modifier = modifier,
        )
        Tab.GENRES -> GenresScreen(graph, onOpenGenre = { onNavigate(Destination.GenreQueue(it)) }, modifier)
        Tab.PLAYED -> PlayedScreen(graph, onOpenEpisode = { onNavigate(Destination.Episode(it)) }, modifier)
        Tab.SEARCH -> SearchScreen(modifier)
    }
}

@Composable
private fun TabBar(selected: Tab, onSelectTab: (Tab) -> Unit, modifier: Modifier = Modifier) {
    NavigationBar(modifier) {
        Tab.entries.forEach { tab ->
            val label = stringResource(tab.label)
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelectTab(tab) },
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = { Text(label) },
            )
        }
    }
}

private fun titleOf(state: AppUiState): StringResource = when (state.nav.top) {
    Destination.Settings -> Res.string.settings
    Destination.Import -> Res.string.import_screen_title
    is Destination.GenreQueue -> Res.string.tab_genres
    is Destination.Episode -> Res.string.episode_title
    null -> if (state.nav.tab == Tab.HOME) Res.string.app_name else state.nav.tab.label
}

private val Tab.label: StringResource
    get() = when (this) {
        Tab.HOME -> Res.string.tab_home
        Tab.GENRES -> Res.string.tab_genres
        Tab.PLAYED -> Res.string.tab_played
        Tab.SEARCH -> Res.string.tab_search
    }

private val Tab.icon: ImageVector
    get() = when (this) {
        Tab.HOME -> Icons.Rounded.Home
        Tab.GENRES -> Icons.Rounded.Category
        Tab.PLAYED -> Icons.Rounded.History
        Tab.SEARCH -> Icons.Rounded.Search
    }
