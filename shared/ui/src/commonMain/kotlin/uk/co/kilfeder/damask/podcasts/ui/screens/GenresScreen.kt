package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.ui.theme.genreTones
import androidx.compose.ui.graphics.compositeOver
import uk.co.kilfeder.damask.podcasts.ui.components.SelectableListRow
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.semantics.selected
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.genres.COVER_COUNT
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreRowUi
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenresUiState
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenresViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.CoverStack
import uk.co.kilfeder.damask.podcasts.ui.components.LastPlayedText
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_empty_body
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_empty_title
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

/** How much of its genre colour a Genres row is tinted with (design hand-off › GenreListPane). */
private const val GENRE_ROW_TINT = 0.45f

@Composable
private fun GenreRow(genre: GenreRowUi, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tones = genreTones(genre.hue)
    SelectableListRow(
        selected = selected,
        onClick = onClick,
        minHeight = Sizes.GenreRowMinHeight,
        modifier = modifier.padding(vertical = Spacing.Space1 / 2),
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(horizontal = Spacing.Space3, vertical = Spacing.Space2),
        // Tinted with the genre's colour; the open one (tablets) full strength with a ring.
        background = tones.container.copy(alpha = GENRE_ROW_TINT).compositeOver(MaterialTheme.colorScheme.surface),
        selectedBackground = tones.container,
        selectedRing = tones.stripe,
    ) {
        CoverStack(genre.covers, maxCovers = COVER_COUNT)
        GenreText(genre, Modifier.weight(1f))
    }
}

@Composable
private fun GenreText(genre: GenreRowUi, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1), modifier = modifier) {
        Text(genre.name, style = DamaskTextStyles.Title, color = MaterialTheme.colorScheme.onSurface)
        LastPlayedText(genre.lastListened)
    }
}
