package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_order_label
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_order_least_recent
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_order_recent
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_order_za
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_order_az
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import uk.co.kilfeder.damask.podcasts.ui.components.OrderChip
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowsOrder
import uk.co.kilfeder.damask.podcasts.ui.components.SearchField
import uk.co.kilfeder.damask.podcasts.ui.components.SelectableListRow
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_no_match
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_filter
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowRowUi
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowsUiState
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowsViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.Artwork
import uk.co.kilfeder.damask.podcasts.ui.components.LastPlayedText
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.home_no_shows_body
import uk.co.kilfeder.damask.podcasts.ui.resources.home_no_shows_title
import uk.co.kilfeder.damask.podcasts.ui.resources.home_search
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_count
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_untitled
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * Every subscribed show, A–Z, to go straight to one (design hand-off › ShowListPane): artwork,
 * name, "Genre · Last played …", and a chevron. On wide layouts the open show is highlighted.
 */
@Composable
internal fun ShowsScreen(
    graph: AppGraph,
    onOpenShow: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    selectedId: String? = null,
    viewModel: ShowsViewModel = viewModel { graph.showsViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ShowsContent(
        state = state,
        intents = ShowsIntents(onOpenShow, onSearch, viewModel::onQueryChange, viewModel::onOrderChange),
        selectedId = selectedId,
        modifier = modifier,
    )
}

/** What Shows can ask for. */
private class ShowsIntents(
    val onOpenShow: (String) -> Unit,
    val onSearch: () -> Unit,
    val onQueryChange: (String) -> Unit,
    val onOrderChange: (ShowsOrder) -> Unit,
)

@Composable
private fun ShowsContent(
    state: ShowsUiState,
    intents: ShowsIntents,
    selectedId: String?,
    modifier: Modifier = Modifier,
) {
    Loadable(state.loading, modifier) {
        if (state.total == 0) {
            EmptyState(
                icon = Icons.Rounded.Podcasts,
                title = stringResource(Res.string.home_no_shows_title),
                body = stringResource(Res.string.home_no_shows_body),
                modifier = Modifier.fillMaxSize(),
            ) {
                Button(onClick = intents.onSearch) { Text(stringResource(Res.string.home_search)) }
            }
            return@Loadable
        }
        LazyColumn(Modifier.fillMaxSize()) {
            item(key = "filter") { ShowsFilter(state.query, intents.onQueryChange) }
            item(key = "count") { ShowsCountAndOrder(state, intents.onOrderChange) }
            items(state.shows, key = { it.showId }) { show ->
                ShowRow(show, selected = show.showId == selectedId, onClick = { intents.onOpenShow(show.showId) })
            }
        }
    }
}

@Composable
private fun ShowRow(show: ShowRowUi, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SelectableListRow(selected, onClick, Sizes.ShowRowMinHeight, modifier) {
        Artwork(show.artworkUrl, show.fallback, size = Sizes.ShowRowArtwork)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1 / 2), modifier = Modifier.weight(1f)) {
            Text(
                text = show.title.ifBlank { stringResource(Res.string.shows_untitled) },
                style = DamaskTextStyles.Title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            ShowMeta(show)
        }
    }
}

/** "History · Last played 4 days ago", the exact time on long press. */
@Composable
internal fun ShowMeta(show: ShowRowUi, modifier: Modifier = Modifier) =
    LastPlayedText(show.lastListened, modifier, before = show.genre)

/** "Search your shows" (design hand-off › ShowListPane): filters your own shows; Discover finds new ones. */
@Composable
private fun ShowsFilter(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    SearchField(
        query = query,
        onQueryChange = onQueryChange,
        placeholder = stringResource(Res.string.shows_filter),
        modifier = modifier.padding(start = Spacing.Space4, end = Spacing.Space4, bottom = Spacing.Space3),
    )
}

/** "54 shows" (or "No shows match …") and the sort chip beside it. */
@Composable
private fun ShowsCountAndOrder(
    state: ShowsUiState,
    onOrderChange: (ShowsOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = if (state.shows.isEmpty()) {
        stringResource(Res.string.shows_no_match, state.query.trim())
    } else {
        pluralStringResource(Res.plurals.shows_count, state.shows.size, state.shows.size)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.Space4, vertical = Spacing.Space1),
    ) {
        Text(
            text = text,
            style = DamaskTextStyles.BodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        OrderChip(
            selected = state.order,
            options = ShowsOrder.entries,
            nameOf = { showsOrderName(it) },
            label = stringResource(Res.string.shows_order_label, showsOrderName(state.order)),
            onSelect = onOrderChange,
        )
    }
}

@Composable
private fun showsOrderName(order: ShowsOrder): String = stringResource(
    when (order) {
        ShowsOrder.A_TO_Z -> Res.string.shows_order_az
        ShowsOrder.Z_TO_A -> Res.string.shows_order_za
        ShowsOrder.RECENTLY_PLAYED -> Res.string.shows_order_recent
        ShowsOrder.LEAST_RECENTLY_PLAYED -> Res.string.shows_order_least_recent
    },
)
