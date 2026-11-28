package uk.co.kilfeder.damask.podcasts.ui

import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed
import uk.co.kilfeder.damask.podcasts.ui.components.ManageGenresButton
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_genres
import uk.co.kilfeder.damask.podcasts.ui.components.SettingsButton
import uk.co.kilfeder.damask.podcasts.ui.components.BackButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.AppUiState
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Destination
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Tab
import uk.co.kilfeder.damask.podcasts.ui.components.MiniPlayer
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.back
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_title
import uk.co.kilfeder.damask.podcasts.ui.resources.home_in_progress
import uk.co.kilfeder.damask.podcasts.ui.resources.import_screen_title
import uk.co.kilfeder.damask.podcasts.ui.resources.settings
import uk.co.kilfeder.damask.podcasts.ui.resources.show_notes
import uk.co.kilfeder.damask.podcasts.ui.resources.tab_genres
import uk.co.kilfeder.damask.podcasts.ui.resources.tab_home
import uk.co.kilfeder.damask.podcasts.ui.resources.tab_played
import uk.co.kilfeder.damask.podcasts.ui.resources.tab_discover
import uk.co.kilfeder.damask.podcasts.ui.resources.tab_shows
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * The phone's heading (design hand-off › Main, Settings): left-aligned, with a back arrow on inner
 * screens and the screen's one action at the end (the cog on Home, + on Shows).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TopBar(
    title: StringResource,
    showBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    TopAppBar(
        modifier = modifier,
        // It sits inside the shell's content, which already clears the status bar.
        windowInsets = WindowInsets(0),
        title = { Text(stringResource(title)) },
        navigationIcon = {
            if (showBack) {
                BackButton(onBack)
            }
        },
        actions = { action?.invoke() },
    )
}

/**
 * The mini-player (while something's loaded and not already on screen) above the bottom tabs.
 * Rail layouts have no bottom tabs, just the mini-player, kept clear of the system's gesture bar.
 */
@Composable
internal fun BottomBars(
    graph: AppGraph,
    tab: Tab,
    showTabs: Boolean,
    playingShown: Boolean,
    onSelectTab: (Tab) -> Unit,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The tab bar keeps clear of the system's gesture bar by itself. Without it (rail layouts) the
    // mini-player's colour runs down behind the gesture bar; with no mini-player, a plain gap keeps
    // the screen's bottom edge clear of it.
    val underGestureBar = !showTabs
    Column(modifier) {
        if (playingShown) {
            if (underGestureBar) Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        } else {
            MiniPlayer(
                graph = graph,
                extendUnderGestureBar = underGestureBar,
                onOpen = { onNavigate(Destination.Episode(it)) },
            )
        }
        if (showTabs) TabBar(tab, onSelectTab)
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

/** The tabs down the side from medium width, with Settings at the foot (design hand-off › NavRail). */
@Composable
internal fun TabRail(selected: Tab, onSelectTab: (Tab) -> Unit, onSettings: () -> Unit, modifier: Modifier = Modifier) {
    NavigationRail(modifier) {
        Tab.entries.forEach { tab ->
            val label = stringResource(tab.label)
            NavigationRailItem(
                selected = tab == selected,
                onClick = { onSelectTab(tab) },
                icon = { Icon(imageVector = tab.icon, contentDescription = null) },
                label = { Text(label) },
            )
        }
        Spacer(Modifier.weight(1f))
        SettingsButton(onSettings, Modifier.padding(bottom = Spacing.Space4))
    }
}

internal fun titleOf(state: AppUiState): StringResource = when (state.nav.top) {
    Destination.Settings -> Res.string.settings
    Destination.ManageGenres -> Res.string.manage_genres
    Destination.Import -> Res.string.import_screen_title
    Destination.AddFeed -> Res.string.add_feed
    Destination.InProgress -> Res.string.home_in_progress
    is Destination.GenreQueue -> Res.string.tab_genres
    is Destination.Show -> Res.string.tab_shows
    is Destination.Episode -> Res.string.episode_title
    is Destination.ShowNotes -> Res.string.show_notes
    null -> state.nav.tab.label
}

internal val Tab.label: StringResource
    get() = when (this) {
        Tab.HOME -> Res.string.tab_home
        Tab.GENRES -> Res.string.tab_genres
        Tab.SHOWS -> Res.string.tab_shows
        Tab.PLAYED -> Res.string.tab_played
        Tab.SEARCH -> Res.string.tab_discover
    }

private val Tab.icon: ImageVector
    get() = when (this) {
        Tab.HOME -> Icons.Rounded.Home
        Tab.GENRES -> Icons.Rounded.Category
        Tab.SHOWS -> Icons.Rounded.Podcasts
        Tab.PLAYED -> Icons.Rounded.History
        Tab.SEARCH -> Icons.Rounded.Explore
    }

/**
 * Screens that draw their own heading with a back arrow: a genre's queue (its name and when it
 * was last played) and a show's page (back and its ⋮ menu).
 */
internal fun hasOwnHeading(state: AppUiState): Boolean =
    state.nav.top is Destination.GenreQueue || state.nav.top is Destination.Show

/** The one action in a top-level screen's heading: the cog on Home (phones; the rail has it otherwise). */
internal fun topLevelAction(
    state: AppUiState,
    compact: Boolean,
    onSettings: () -> Unit,
    onManageGenres: () -> Unit,
): (@Composable () -> Unit)? {
    if (state.nav.top != null) return null
    return when {
        state.nav.tab == Tab.HOME && compact -> {
            { SettingsButton(onSettings) }
        }
        state.nav.tab == Tab.GENRES -> {
            { ManageGenresButton(onManageGenres) }
        }
        else -> null
    }
}
