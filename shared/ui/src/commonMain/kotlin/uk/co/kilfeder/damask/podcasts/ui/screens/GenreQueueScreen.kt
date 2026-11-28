package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.components.PlayNextButton
import uk.co.kilfeder.damask.podcasts.ui.components.CoverStack
import uk.co.kilfeder.damask.podcasts.ui.components.BackButton
import uk.co.kilfeder.damask.podcasts.presentation.genres.COVER_COUNT
import uk.co.kilfeder.damask.podcasts.presentation.episodes.ListOrder
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.key
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.ui.components.RowPlayer
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreQueueUiState
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreQueueViewModel
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.ui.components.EpisodeRow
import uk.co.kilfeder.damask.podcasts.ui.components.PrimaryPanel
import uk.co.kilfeder.damask.podcasts.ui.components.LastPlayedText
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.genre_queue_empty_body
import uk.co.kilfeder.damask.podcasts.ui.resources.genre_queue_empty_title
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_shows
import uk.co.kilfeder.damask.podcasts.ui.resources.home_in_progress
import uk.co.kilfeder.damask.podcasts.ui.resources.home_up_next
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing
import uk.co.kilfeder.damask.podcasts.ui.resources.mark_played

@Composable
internal fun GenreQueueScreen(
    graph: AppGraph,
    categoryId: String,
    onOpenEpisode: (EpisodeId) -> Unit,
    rowPlayer: RowPlayer,
    modifier: Modifier = Modifier,
    showHeader: Boolean = false,
    onBack: (() -> Unit)? = null,
    viewModel: GenreQueueViewModel = viewModel(key = "genre-$categoryId") { graph.genreQueueViewModel(categoryId) },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GenreQueueContent(
        state = state,
        onDismissEpisode = viewModel::onMarkPlayed,
        onLoadMore = viewModel::onLoadMore,
        onOrderChange = viewModel::onOrderChange,
        onOpenEpisode = onOpenEpisode,
        rowPlayer = rowPlayer,
        categoryId = categoryId,
        showHeader = showHeader,
        onBack = onBack,
        modifier = modifier,
    )
}

@Composable
private fun GenreQueueContent(
    state: GenreQueueUiState,
    onDismissEpisode: (EpisodeId) -> Unit,
    onLoadMore: () -> Unit,
    onOrderChange: (ListOrder) -> Unit,
    onOpenEpisode: (EpisodeId) -> Unit,
    rowPlayer: RowPlayer,
    categoryId: String,
    showHeader: Boolean,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    if (state.loading) {
        Loading(modifier)
        return
    }
    val markPlayed = stringResource(Res.string.mark_played)
    // On wide layouts the queue sits on the tinted detail card (a heading, no back); rows match it.
    val colors = MaterialTheme.colorScheme
    val listBackground = if (showHeader && onBack == null) colors.surfaceContainerLow else colors.surface
    val source = PlaySource.Genre(categoryId)
    // Marks what the Resume / Play button will play, so you can see it before pressing.
    val nextId = state.nextUp?.id
    val row: @Composable (EpisodeRowUi, Color) -> Unit = { episode, background ->
        EpisodeRow(
            row = episode,
            player = rowPlayer,
            source = source,
            onClick = { onOpenEpisode(episode.id) },
            swipeLabel = markPlayed,
            onSwipe = { onDismissEpisode(episode.id) },
            background = background,
            playsNext = episode.id == nextId,
        )
    }
    LazyColumn(modifier) {
        if (showHeader) genreHeader(state, onBack, rowPlayer, source)
        if (state.inProgress.isNotEmpty()) {
            item(key = "in-progress") {
                PrimaryPanel(stringResource(Res.string.home_in_progress), Modifier.padding(top = Spacing.Space2)) {
                    state.inProgress.forEach { row(it, MaterialTheme.colorScheme.primaryContainer) }
                }
            }
        }
        if (state.upNext.isEmpty() && state.inProgress.isEmpty()) {
            item(key = "empty") { EmptyQueue(state, Modifier.fillParentMaxHeight(EMPTY_HEIGHT_SHARE)) }
        }
        if (state.upNext.isNotEmpty()) {
            item(key = "up-next") {
                OrderedHeading(
                    title = stringResource(Res.string.home_up_next),
                    order = state.order,
                    options = ListOrder.entries,
                    onOrderChange = onOrderChange,
                    modifier = Modifier.padding(top = Spacing.Space4, bottom = Spacing.Space1),
                )
            }
        }
        items(state.upNext, key = { it.id.key }) { row(it, listBackground) }
        loadMore(state.canLoadMore, onLoadMore)
    }
}

/** What part of the screen an empty queue's message fills, below the header. */
private const val EMPTY_HEIGHT_SHARE = 0.8f

/** Nothing left to play in this genre (design hand-off › Empty · genre queue). */
@Composable
private fun EmptyQueue(state: GenreQueueUiState, modifier: Modifier = Modifier) {
    val last = state.lastListened
    EmptyState(
        icon = Icons.Rounded.Category,
        title = stringResource(Res.string.genre_queue_empty_title, state.name),
        body = stringResource(Res.string.genre_queue_empty_body, state.name),
        modifier = modifier,
        meta = last?.let { { LastPlayedText(it) } },
    )
}

/** The header, then the button that plays through the genre from the start of its queue. */
private fun LazyListScope.genreHeader(
    state: GenreQueueUiState,
    onBack: (() -> Unit)?,
    rowPlayer: RowPlayer,
    source: PlaySource,
) {
    item(key = "header") { GenreHeader(state, onBack) }
    val next = state.nextUp ?: return
    item(key = "play") {
        PlayNextButton(
            next = next,
            playing = rowPlayer.isPlaying(next.id),
            onPlayPause = { rowPlayer.playPause(it, source) },
            modifier = Modifier.padding(Spacing.Space4),
        )
    }
}

/**
 * The genre's page header (like a show's): its next shows' covers stacked, its name, and "Last
 * played today · 4 shows". Back on phones; beside the list on wide layouts there's none.
 */
@Composable
private fun GenreHeader(state: GenreQueueUiState, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    val shows = pluralStringResource(Res.plurals.genres_shows, state.showCount, state.showCount)
    Column(modifier.fillMaxWidth()) {
        onBack?.let { BackButton(it, Modifier.padding(start = Spacing.Space1, top = Spacing.Space2)) }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Space4),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Spacing.Space4, end = Spacing.Space4, top = Spacing.Space4),
        ) {
            CoverStack(state.covers, COVER_COUNT, size = Sizes.ShowHeaderArtwork, shape = MaterialTheme.shapes.medium)
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1), modifier = Modifier.weight(1f)) {
                Text(
                    text = state.name,
                    style = DamaskTextStyles.TitleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                LastPlayedText(state.lastListened, after = shows)
            }
        }
    }
}
