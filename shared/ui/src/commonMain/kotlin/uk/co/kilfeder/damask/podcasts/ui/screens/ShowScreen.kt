package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.ui.components.BarPlayButton
import uk.co.kilfeder.damask.podcasts.ui.components.BarArtwork
import uk.co.kilfeder.damask.podcasts.ui.components.collapsingTitle
import uk.co.kilfeder.damask.podcasts.ui.components.collapsingListTop
import uk.co.kilfeder.damask.podcasts.ui.components.rememberCollapsingTitleState
import uk.co.kilfeder.damask.podcasts.ui.components.CollapsingTopBar
import uk.co.kilfeder.damask.podcasts.ui.components.CollapsingTitleState
import uk.co.kilfeder.damask.podcasts.ui.resources.show_episodes_heading
import uk.co.kilfeder.damask.podcasts.presentation.episodes.ListOrder
import uk.co.kilfeder.damask.podcasts.ui.components.BackButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowPageUi
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowUiState
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.EpisodeRow
import uk.co.kilfeder.damask.podcasts.ui.components.LocalShowMessage
import uk.co.kilfeder.damask.podcasts.ui.components.RowPlayer
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.back
import uk.co.kilfeder.damask.podcasts.ui.resources.episode_gone_body
import uk.co.kilfeder.damask.podcasts.ui.resources.show_copy_feed
import uk.co.kilfeder.damask.podcasts.ui.resources.show_feed_copied
import uk.co.kilfeder.damask.podcasts.ui.resources.show_options
import uk.co.kilfeder.damask.podcasts.ui.resources.show_unsubscribe
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_untitled
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** Items above the first episode: details, the button and description, the Episodes heading. */
private const val ITEMS_BEFORE_EPISODES = 3

/**
 * A show's page (design hand-off › Show, ShowMenu): back and a ⋮ menu (Copy feed URL,
 * Unsubscribe); artwork, name, maker, how often it publishes, and its genre; a button that
 * resumes or plays it; its description; then every episode in the show's order, which is also the
 * order it plays in. The episode the button plays is highlighted. Playing from here keeps the
 * show's context, so autoplay carries on through it.
 */
@Composable
internal fun ShowScreen(
    graph: AppGraph,
    showId: String,
    actions: ShowPageActions,
    rowPlayer: RowPlayer,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surface,
    viewModel: ShowViewModel = viewModel(key = "show-$showId") { graph.showViewModel(showId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val intents = ShowIntents(
        onGenreChange = viewModel::onGenreChange,
        onOrderChange = viewModel::onOrderChange,
        onSubscribe = viewModel::onSubscribe,
    )
    ShowContent(state, actions, intents, rowPlayer, background, modifier)
}

@Composable
private fun ShowContent(
    state: ShowUiState,
    actions: ShowPageActions,
    intents: ShowIntents,
    rowPlayer: RowPlayer,
    background: Color,
    modifier: Modifier = Modifier,
) {
    val show = state.show
    val titleState = rememberCollapsingTitleState()
    Column(modifier) {
        ShowTopBar(state, actions, titleState, background, rowPlayer)
        when {
            state.loading -> Loading(Modifier.weight(1f))
            show == null -> EmptyState(
                icon = Icons.Rounded.Podcasts,
                title = stringResource(Res.string.shows_untitled),
                body = stringResource(Res.string.episode_gone_body),
                modifier = Modifier.weight(1f),
            )
            else -> ShowList(
                state = state,
                show = show,
                actions = actions,
                intents = intents,
                rowPlayer = rowPlayer,
                background = background,
                titleState = titleState,
                modifier = Modifier.weight(1f).collapsingListTop(titleState),
            )
        }
    }
}

@Composable
private fun ShowList(
    state: ShowUiState,
    show: ShowPageUi,
    actions: ShowPageActions,
    intents: ShowIntents,
    rowPlayer: RowPlayer,
    background: Color,
    titleState: CollapsingTitleState,
    modifier: Modifier = Modifier,
) {
    val list = rememberLazyListState()
    OpenAtFirstUnplayed(state.startAt, list)
    val source = PlaySource.Show(show.showId)
    val nextId = state.nextUp?.id
    LazyColumn(state = list, modifier = modifier) {
        item(key = "details") {
            ShowDetails(show, state.genres, intents.onGenreChange, titleModifier = Modifier.collapsingTitle(titleState))
        }
        item(key = "actions") {
            val next = state.nextUp
            ShowActions(
                show = show,
                nextUp = next,
                playing = next != null && rowPlayer.isPlaying(next.id),
                onPlayPause = { rowPlayer.playPause(it, source) },
                onSubscribe = intents.onSubscribe,
            )
        }
        item(key = "heading") {
            OrderedHeading(
                title = stringResource(Res.string.show_episodes_heading),
                order = state.order,
                options = listOf(ListOrder.NEWEST_FIRST, ListOrder.OLDEST_FIRST),
                onOrderChange = intents.onOrderChange,
            )
        }
        items(state.episodes, key = { it.id.guid }) { row ->
            val next = row.id == nextId
            EpisodeRow(
                row = row,
                player = rowPlayer,
                source = source,
                onClick = { actions.onOpenEpisode(row.id, source) },
                background = background,
                dimPlayed = true,
                playsNext = next,
            )
        }
    }
}

/**
 * Oldest first, the list opens at the first unplayed episode, with the played ones above it
 * (design hand-off › Show). Once per visit, so it doesn't jump while you scroll.
 */
@Composable
private fun OpenAtFirstUnplayed(startAt: Int?, list: LazyListState) {
    var done by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(startAt) {
        if (!done && startAt != null) list.scrollToItem(ITEMS_BEFORE_EPISODES + startAt)
        done = true
    }
}

/** Back (on its own screen) and the show's ⋮ menu; its cover, name and play join them once scrolled. */
@Composable
private fun ShowTopBar(
    state: ShowUiState,
    actions: ShowPageActions,
    titleState: CollapsingTitleState,
    background: Color,
    rowPlayer: RowPlayer,
    modifier: Modifier = Modifier,
) {
    val show = state.show
    val next = state.nextUp
    CollapsingTopBar(
        state = titleState,
        title = show?.title.orEmpty(),
        modifier = modifier,
        background = background,
        onBack = actions.onBack,
        art = show?.let { { BarArtwork(it.artworkUrl, it.fallback) } },
        collapsedAction = if (show != null && next != null) {
            { BarPlayButton(next, rowPlayer, PlaySource.Show(show.showId)) }
        } else {
            null
        },
    ) {
        if (show != null && show.subscribed) ShowMenu(show, actions.onUnsubscribe)
    }
}

/** Copy feed URL, then Unsubscribe (design hand-off › ShowMenu). */
@Composable
private fun ShowMenu(show: ShowPageUi, onUnsubscribe: (String, String) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    @Suppress("DEPRECATION") // LocalClipboard needs a platform ClipEntry; plain text is all this needs.
    val clipboard = LocalClipboardManager.current
    val showMessage = LocalShowMessage.current
    val copied = stringResource(Res.string.show_feed_copied)
    Box(modifier) {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(Res.string.show_options))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.show_copy_feed)) },
                leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
                onClick = {
                    open = false
                    clipboard.setText(AnnotatedString(show.feedUrl))
                    showMessage(copied)
                },
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.show_unsubscribe)) },
                leadingIcon = { Icon(Icons.Rounded.RemoveCircleOutline, contentDescription = null) },
                onClick = {
                    open = false
                    onUnsubscribe(show.showId, show.title)
                },
            )
        }
    }
}
