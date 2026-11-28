package uk.co.kilfeder.damask.podcasts.ui.screens

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
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.ui.components.RowPlayer
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreQueueUiState
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreQueueViewModel
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.ui.components.EpisodeRow
import uk.co.kilfeder.damask.podcasts.ui.components.ExactValueText
import uk.co.kilfeder.damask.podcasts.ui.components.PaneHeader
import uk.co.kilfeder.damask.podcasts.ui.components.PrimaryPanel
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.genre_queue_empty_body
import uk.co.kilfeder.damask.podcasts.ui.resources.genre_queue_empty_title
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_last
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_never
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_shows
import uk.co.kilfeder.damask.podcasts.ui.resources.home_in_progress
import uk.co.kilfeder.damask.podcasts.ui.resources.home_up_next
import uk.co.kilfeder.damask.podcasts.ui.resources.row_meta
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
    val row: @Composable (EpisodeRowUi, Color) -> Unit = { episode, background ->
        EpisodeRow(
            row = episode,
            playing = rowPlayer.isPlaying(episode.id),
            onClick = { onOpenEpisode(episode.id) },
            onPlayPause = { rowPlayer.playPause(episode.id, PlaySource.Genre(categoryId)) },
            swipeLabel = markPlayed,
            onSwipe = { onDismissEpisode(episode.id) },
            background = background,
        )
    }
    LazyColumn(modifier) {
        if (showHeader) item(key = "header") { QueueHeader(state, onBack) }
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
        if (state.upNext.isNotEmpty()) sectionHeader("up-next", stringResourceKey = Res.string.home_up_next)
        items(state.upNext, key = { "${it.id.showId}-${it.id.guid}" }) { row(it, listBackground) }
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
        meta = last?.let {
            {
                ExactValueText(
                    value = it,
                    display = stringResource(Res.string.genres_last, it.summary),
                    style = DamaskTextStyles.BodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

/** The genre's name with "Last played today · 4 shows" (design hand-off › GenreQueuePane). */
@Composable
private fun QueueHeader(state: GenreQueueUiState, onBack: (() -> Unit)?, modifier: Modifier = Modifier) {
    val shows = pluralStringResource(Res.plurals.genres_shows, state.showCount, state.showCount)
    val last = state.lastListened
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    PaneHeader(state.name, modifier, onBack = onBack) {
        if (last == null) {
            val text = stringResource(Res.string.row_meta, stringResource(Res.string.genres_never), shows)
            Text(text, style = DamaskTextStyles.BodySmall, color = color)
        } else {
            val lastPlayed = stringResource(Res.string.genres_last, last.summary)
            ExactValueText(
                value = last,
                display = stringResource(Res.string.row_meta, lastPlayed, shows),
                style = DamaskTextStyles.BodySmall,
                color = color,
            )
        }
    }
}
