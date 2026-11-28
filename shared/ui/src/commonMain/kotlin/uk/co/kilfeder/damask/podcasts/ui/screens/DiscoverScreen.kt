package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.LinearProgressIndicator
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

/** What a Discover row can ask for: subscribe, or open (a show you have, or one to preview). */
private class DiscoverActions(
    val onSubscribe: (key: String) -> Unit,
    val onOpen: (key: String) -> Unit,
    val onOpenShow: (showId: String) -> Unit,
)

/**
 * Discover (spec › Search): search Apple's podcast directory as you type. Subscribe adds the show
 * in Apple's genre for it, no questions; tapping a show opens its page (unsubscribed, if you
 * don't have it, to listen first).
 */
@Composable
internal fun DiscoverScreen(
    graph: AppGraph,
    onOpenShow: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiscoverViewModel = viewModel { graph.discoverViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val open by rememberUpdatedState(onOpenShow)
    // A show you don't have opens once its feed has been read.
    LaunchedEffect(state.openShowId) {
        state.openShowId?.let {
            open(it)
            viewModel.onShowOpened()
        }
    }
    Column(modifier) {
        SearchField(
            query = state.query,
            onQueryChange = viewModel::onQueryChange,
            placeholder = stringResource(Res.string.home_search),
            modifier = Modifier.padding(start = Spacing.Space4, end = Spacing.Space4, bottom = Spacing.Space3),
        )
        DiscoverBody(
            state = state,
            actions = DiscoverActions(viewModel::onSubscribe, viewModel::onOpen, onOpenShow),
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DiscoverBody(state: DiscoverUiState, actions: DiscoverActions, modifier: Modifier = Modifier) {
    when (val results = state.results) {
        DiscoverResults.Idle -> EmptyState(
            icon = Icons.Rounded.Explore,
            title = stringResource(Res.string.discover_idle_title),
            body = stringResource(Res.string.discover_idle_body),
            modifier = modifier,
        )
        DiscoverResults.Searching -> Loading(modifier)
        DiscoverResults.NoMatch ->
            EmptyMessage(stringResource(Res.string.discover_no_match, state.query.trim()), modifier)
        DiscoverResults.Busy -> EmptyMessage(stringResource(Res.string.discover_busy), modifier)
        DiscoverResults.Failed -> EmptyMessage(stringResource(Res.string.discover_failed), modifier)
        is DiscoverResults.Found -> LazyColumn(modifier) {
            items(results.rows, key = { it.key }) { DiscoverRow(it, actions) }
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
            // Opening (not subscribing): a thin bar under the name, and Subscribe stays as it is.
            if (row.state == DiscoverRowState.OPENING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = Spacing.Space1))
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
        DiscoverRowState.NEW, DiscoverRowState.OPENING -> {
            val label = stringResource(Res.string.discover_subscribe_label, row.title)
            FilledTonalButton(
                onClick = onSubscribe,
                enabled = row.state == DiscoverRowState.NEW,
                modifier = modifier.semantics { contentDescription = label },
            ) {
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
