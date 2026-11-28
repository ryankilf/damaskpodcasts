package uk.co.kilfeder.damask.podcasts.ui.screens

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
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeRowUi
import uk.co.kilfeder.damask.podcasts.presentation.home.HomeViewModel
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.ui.components.EpisodeRow
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
    modifier: Modifier = Modifier,
) {
    val onPanel = MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Space3)
            .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.medium)
            .padding(vertical = Spacing.Space2),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Space2),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Spacing.Space4, end = Spacing.Space1),
        ) {
            Text(
                text = stringResource(Res.string.home_in_progress),
                style = DamaskTextStyles.Title,
                color = onPanel,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(Res.string.in_progress_count, rows.size),
                style = DamaskTextStyles.BodySmall,
                color = onPanel,
            )
        }
        rows.take(IN_PROGRESS_PREVIEW).forEach { row ->
            EpisodeRow(row, onClick = { onOpenEpisode(row.id, PlaySource.Resume) })
        }
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
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel { graph.homeViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    if (state.loading) {
        Loading(modifier)
        return
    }
    LazyColumn(modifier) {
        items(state.inProgress, key = { "p-${it.id.showId}-${it.id.guid}" }) { row ->
            EpisodeRow(row, onClick = { onOpenEpisode(row.id, PlaySource.Resume) })
        }
    }
}
