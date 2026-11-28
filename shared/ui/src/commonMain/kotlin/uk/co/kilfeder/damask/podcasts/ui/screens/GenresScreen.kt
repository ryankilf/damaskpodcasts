package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.ui.components.SelectableListRow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.genres.COVER_COUNT
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreRowUi
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenresUiState
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenresViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.CoverStack
import uk.co.kilfeder.damask.podcasts.ui.components.ExactValueText
import uk.co.kilfeder.damask.podcasts.ui.components.GenreChip
import uk.co.kilfeder.damask.podcasts.ui.components.LastPlayedText
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_empty_body
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_empty_title
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_last
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_longest_unplayed
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_never
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * Genres, least recently played first (design hand-off › GenreListPane). On wide layouts it's the
 * list pane, with the genre open beside it highlighted ([selectedId]).
 */
@Composable
internal fun GenresScreen(
    graph: AppGraph,
    onOpenGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedId: String? = null,
    viewModel: GenresViewModel = viewModel { graph.genresViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GenresContent(state, onOpenGenre, selectedId, modifier)
}

@Composable
private fun GenresContent(
    state: GenresUiState,
    onOpenGenre: (String) -> Unit,
    selectedId: String?,
    modifier: Modifier = Modifier,
) {
    if (state.loading) {
        Loading(modifier)
        return
    }
    if (state.genres.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.Category,
            title = stringResource(Res.string.genres_empty_title),
            body = stringResource(Res.string.genres_empty_body),
            modifier = modifier,
        )
        return
    }
    LazyColumn(modifier) {
        items(state.genres, key = { it.categoryId }) { genre ->
            GenreRow(genre, selected = genre.categoryId == selectedId, onClick = { onOpenGenre(genre.categoryId) })
        }
    }
}

@Composable
private fun GenreRow(genre: GenreRowUi, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    SelectableListRow(
        selected = selected,
        onClick = onClick,
        minHeight = Sizes.GenreRowMinHeight,
        modifier = modifier.padding(vertical = Spacing.Space1 / 2),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = Spacing.Space3, vertical = Spacing.Space2),
    ) {
        CoverStack(genre.covers, maxCovers = COVER_COUNT)
        GenreText(genre, Modifier.weight(1f))
    }
}

@Composable
private fun GenreText(genre: GenreRowUi, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1), modifier = modifier) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Space2),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            Text(genre.name, style = DamaskTextStyles.Title, color = MaterialTheme.colorScheme.onSurface)
            if (genre.stalest) GenreChip(stringResource(Res.string.genres_longest_unplayed))
        }
        LastPlayedText(genre.lastListened)
    }
}
