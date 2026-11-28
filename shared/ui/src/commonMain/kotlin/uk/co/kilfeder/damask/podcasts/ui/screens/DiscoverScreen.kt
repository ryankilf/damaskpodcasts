package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed
import androidx.compose.material3.TextButton
import androidx.compose.runtime.setValue
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_try_again
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_failed_body
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_busy_body
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_no_match_body
import androidx.compose.material3.Button
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.SearchOff
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_searching
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_updating
import uk.co.kilfeder.damask.podcasts.ui.theme.Motion
import uk.co.kilfeder.damask.podcasts.ui.components.SkeletonRows
import androidx.compose.ui.draw.alpha
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.layout.fillMaxWidth
import uk.co.kilfeder.damask.podcasts.ui.components.FadeThrough
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.discover.DiscoverResults
import uk.co.kilfeder.damask.podcasts.presentation.discover.DiscoverRowState
import uk.co.kilfeder.damask.podcasts.presentation.discover.DiscoverRowUi
import uk.co.kilfeder.damask.podcasts.presentation.discover.DiscoverUiState
import uk.co.kilfeder.damask.podcasts.presentation.discover.DiscoverViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.Artwork
import uk.co.kilfeder.damask.podcasts.ui.components.SearchField
import uk.co.kilfeder.damask.podcasts.ui.components.SelectableListRow
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_busy
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_failed
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_idle_body
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_idle_title
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_no_match
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_subscribe
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_subscribe_label
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_subscribed
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_subscribing
import uk.co.kilfeder.damask.podcasts.ui.resources.discover_feed_problem
import uk.co.kilfeder.damask.podcasts.ui.resources.home_search
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_untitled
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** Placeholder rows while a first search runs (a screenful). */
private const val SKELETON_ROWS = 6

/** The previous results' opacity while a refined search runs (design: 0.6). */
private const val REFRESHING_ALPHA = 0.6f

/** What a Discover row can ask for: subscribe, or open (a show you have, or one to preview). */
private class DiscoverActions(
    val onSubscribe: (key: String) -> Unit,
    val onOpen: (key: String) -> Unit,
    val onOpenShow: (showId: String) -> Unit,
    val onRetry: () -> Unit,
    /** "Add a feed by URL": for private feeds, and shows the directory doesn't have. */
    val onAddFeed: () -> Unit,
)

/**
 * Discover (spec › Search): search Apple's podcast directory as you type. Subscribe adds the show
 * in Apple's genre for it, no questions. Tapping a show you have opens it in Shows ([onOpenShow]);
 * one you don't opens its page here, unsubscribed, to listen first ([onPreviewShow]).
 */
@Composable
internal fun DiscoverScreen(
    graph: AppGraph,
    onOpenShow: (String) -> Unit,
    onPreviewShow: (String) -> Unit,
    onAddFeed: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel { graph.discoverViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val preview by rememberUpdatedState(onPreviewShow)
    // A show you don't have opens once its feed has been read.
    LaunchedEffect(state.openShowId) {
        state.openShowId?.let {
            preview(it)
            viewModel.onShowOpened()
        }
    }
    Column(modifier) {
        // The refining bar sits in the field's bottom margin, so nothing below moves when it shows.
        Box(contentAlignment = Alignment.BottomCenter) {
            SearchField(
                query = state.query,
                onQueryChange = viewModel::onQueryChange,
                placeholder = stringResource(Res.string.home_search),
                modifier = Modifier.padding(start = Spacing.Space4, end = Spacing.Space4, bottom = Spacing.Space3),
            )
            if ((state.results as? DiscoverResults.Found)?.refreshing == true) {
                val label = stringResource(Res.string.discover_updating)
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Spacing.Space8, vertical = Spacing.Space1)
                        .semantics { contentDescription = label },
                )
            }
        }
        DiscoverBody(
            state = state,
            actions = DiscoverActions(
                onSubscribe = viewModel::onSubscribe,
                onOpen = viewModel::onOpen,
                onOpenShow = onOpenShow,
                onRetry = viewModel::onRetry,
                onAddFeed = onAddFeed,
            ),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DiscoverBody(state: DiscoverUiState, actions: DiscoverActions, modifier: Modifier = Modifier) {
    // Fades between searching, results and messages, like every loading area (Loadable); new
    // rows within a set of results redraw in place.
    FadeThrough(state.results, screenOf = { it::class }, modifier = modifier.fillMaxSize()) { results ->
        val fill = Modifier.fillMaxSize()
        when (results) {
            DiscoverResults.Idle -> DiscoverIdle(actions.onAddFeed, fill) {
                EmptyState(
                    icon = Icons.Rounded.Explore,
                    title = stringResource(Res.string.discover_idle_title),
                    body = stringResource(Res.string.discover_idle_body),
                    modifier = Modifier.fillMaxSize(),
                )
            }
            DiscoverResults.Searching -> SkeletonRows(
                count = SKELETON_ROWS,
                artwork = Sizes.ShowRowArtwork,
                label = stringResource(Res.string.discover_searching),
                modifier = fill,
            )
            DiscoverResults.NoMatch -> EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = stringResource(Res.string.discover_no_match),
                body = stringResource(Res.string.discover_no_match_body),
                modifier = fill,
            ) { TextButton(onClick = actions.onAddFeed) { Text(stringResource(Res.string.add_feed)) } }
            DiscoverResults.Busy -> EmptyState(
                icon = Icons.Rounded.HourglassEmpty,
                title = stringResource(Res.string.discover_busy),
                body = stringResource(Res.string.discover_busy_body),
                modifier = fill,
            ) { Button(onClick = actions.onRetry) { Text(stringResource(Res.string.discover_try_again)) } }
            DiscoverResults.Failed -> EmptyState(
                icon = Icons.Rounded.CloudOff,
                title = stringResource(Res.string.discover_failed),
                body = stringResource(Res.string.discover_failed_body),
                modifier = fill,
            ) {
                Button(onClick = actions.onRetry) { Text(stringResource(Res.string.discover_try_again)) }
                TextButton(onClick = actions.onAddFeed) { Text(stringResource(Res.string.add_feed)) }
            }
            is DiscoverResults.Found -> {
                // Refining: the previous results stay, dimmed, until the new ones arrive.
                val alpha by animateFloatAsState(
                    targetValue = if (results.refreshing) REFRESHING_ALPHA else 1f,
                    animationSpec = tween(Motion.FADE_IN_MS),
                    label = "refreshing",
                )
                LazyColumn(fill.alpha(alpha)) {
                    items(results.rows, key = { it.key }) { DiscoverRow(it, actions) }
                }
            }
        }
    }
}

/** A result: artwork, name and host; Subscribe, a spinner while its feed is read, or Subscribed. */
@Composable
private fun DiscoverRow(row: DiscoverRowUi, actions: DiscoverActions, modifier: Modifier = Modifier) {
    val open = { row.showId?.let(actions.onOpenShow) ?: actions.onOpen(row.key) }
    SelectableListRow(selected = false, onClick = open, minHeight = Sizes.ShowRowMinHeight, modifier = modifier) {
        Artwork(row.artworkUrl, row.fallback, size = Sizes.ShowRowArtwork)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1 / 2), modifier = Modifier.weight(1f)) {
            Text(
                text = row.title.ifBlank { stringResource(Res.string.shows_untitled) },
                style = DamaskTextStyles.Title,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (row.author.isNotBlank()) {
                Text(
                    text = row.author,
                    style = DamaskTextStyles.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            row.problem?.let {
                Text(
                    text = stringResource(Res.string.discover_feed_problem, it),
                    style = DamaskTextStyles.BodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        SubscribeAction(row, onSubscribe = { actions.onSubscribe(row.key) })
    }
}

@Composable
private fun SubscribeAction(row: DiscoverRowUi, onSubscribe: () -> Unit, modifier: Modifier = Modifier) {
    when (row.state) {
        DiscoverRowState.NEW -> {
            val label = stringResource(Res.string.discover_subscribe_label, row.title)
            FilledTonalButton(onClick = onSubscribe, modifier = modifier.semantics { contentDescription = label }) {
                Text(stringResource(Res.string.discover_subscribe))
            }
        }
        DiscoverRowState.SUBSCRIBING -> {
            val label = stringResource(Res.string.discover_subscribing, row.title)
            Box(
                contentAlignment = Alignment.Center,
                modifier = modifier.size(Sizes.MinTouchTarget).semantics { contentDescription = label },
            ) {
                CircularProgressIndicator(
                    strokeWidth = Sizes.ProgressRingStroke,
                    modifier = Modifier.size(Sizes.IconRipple * 2),
                )
            }
        }
        DiscoverRowState.SUBSCRIBED -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Space1),
            modifier = modifier.padding(end = Spacing.Space2),
        ) {
            val colour = MaterialTheme.colorScheme.onSurfaceVariant
            Icon(Icons.Rounded.Check, contentDescription = null, tint = colour)
            Text(stringResource(Res.string.discover_subscribed), style = DamaskTextStyles.BodySmall, color = colour)
        }
    }
}
