package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.ui.graphics.Color
import uk.co.kilfeder.damask.podcasts.ui.components.panelColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.key
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.home.HomeViewModel
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.ui.components.EpisodeRow
import uk.co.kilfeder.damask.podcasts.ui.components.PrimaryPanel
import uk.co.kilfeder.damask.podcasts.ui.components.RowPlayer
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.home_in_progress
import uk.co.kilfeder.damask.podcasts.ui.resources.in_progress_count
import uk.co.kilfeder.damask.podcasts.ui.resources.in_progress_see_all
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** How many in-progress episodes the Home panel shows before "See all". */
internal const val IN_PROGRESS_PREVIEW = 3

/**
 * Home's In progress panel (design hand-off › Main): a primary-container card holding the most
 * recently played unfinished episodes, the count beside the heading, and "See all" when there are
 * more than fit.
 */
@Composable
internal fun InProgressPanel(
    rows: List<EpisodeRowUi>,
    onOpenEpisode: (EpisodeId, PlaySource) -> Unit,
    onSeeAll: () -> Unit,
    rowPlayer: RowPlayer,
    modifier: Modifier = Modifier,
) {
    val onPanel = MaterialTheme.colorScheme.onPrimaryContainer
    PrimaryPanel(
        title = stringResource(Res.string.home_in_progress),
        count = stringResource(Res.string.in_progress_count, rows.size),
        modifier = modifier,
    ) {
        val panel = panelColor()
        rows.take(IN_PROGRESS_PREVIEW).forEach { row -> ResumeRow(row, onOpenEpisode, rowPlayer, background = panel) }
        if (rows.size > IN_PROGRESS_PREVIEW) {
            TextButton(onClick = onSeeAll, modifier = Modifier.align(Alignment.End).padding(end = Spacing.Space1)) {
                val label = stringResource(Res.string.in_progress_see_all, rows.size)
                Text(label, style = DamaskTextStyles.Label, color = onPanel)
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = onPanel)
            }
        }
    }
}

/** Every in-progress episode, from the panel's "See all". */
@Composable
internal fun InProgressScreen(
    graph: AppGraph,
    onOpenEpisode: (EpisodeId, PlaySource) -> Unit,
    rowPlayer: RowPlayer,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel { graph.homeViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loading) {
        Loading(modifier)
        return
    }
    LazyColumn(modifier) {
        items(state.inProgress, key = { "p-${it.id.key}" }) { row ->
            ResumeRow(row, onOpenEpisode, rowPlayer)
        }
    }
}

/** An in-progress row: plays with the context saved with it. */
@Composable
private fun ResumeRow(
    row: EpisodeRowUi,
    onOpenEpisode: (EpisodeId, PlaySource) -> Unit,
    rowPlayer: RowPlayer,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surface,
) {
    EpisodeRow(
        row = row,
        player = rowPlayer,
        source = PlaySource.Resume,
        onClick = { onOpenEpisode(row.id, PlaySource.Resume) },
        modifier = modifier,
        background = background,
    )
}
