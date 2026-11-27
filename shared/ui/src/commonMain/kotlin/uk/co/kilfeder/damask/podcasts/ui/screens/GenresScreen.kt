package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreRowUi
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenresUiState
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenresViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.ExactValueText
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_empty_body
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_empty_title
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_last
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_never
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_shows
import uk.co.kilfeder.damask.podcasts.ui.resources.row_meta
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

@Composable
internal fun GenresScreen(
    graph: AppGraph,
    onOpenGenre: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GenresViewModel = viewModel { graph.genresViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    GenresContent(state, onOpenGenre, modifier)
}

@Composable
private fun GenresContent(state: GenresUiState, onOpenGenre: (String) -> Unit, modifier: Modifier = Modifier) {
    if (state.loading) {
        Loading(modifier)
        return
    }
    if (state.genres.isEmpty()) {
        EmptyState(
            title = stringResource(Res.string.genres_empty_title),
            body = stringResource(Res.string.genres_empty_body),
            modifier = modifier,
        )
        return
    }
    LazyColumn(modifier) {
        items(state.genres, key = { it.categoryId }) { genre ->
            GenreRow(genre, onClick = { onOpenGenre(genre.categoryId) })
        }
    }
}

@Composable
private fun GenreRow(genre: GenreRowUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Space1),
        modifier = modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = Sizes.RowMinHeight)
            .padding(horizontal = Spacing.Space4, vertical = Spacing.Space2),
    ) {
        Text(genre.name, style = DamaskTextStyles.Title, color = MaterialTheme.colorScheme.onSurface)
        val shows = pluralStringResource(Res.plurals.genres_shows, genre.showCount, genre.showCount)
        val last = genre.lastListened
        if (last == null) {
            Text(
                text = stringResource(Res.string.row_meta, shows, stringResource(Res.string.genres_never)),
                style = DamaskTextStyles.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ExactValueText(
                value = last,
                display = stringResource(
                    Res.string.row_meta,
                    shows,
                    stringResource(Res.string.genres_last, last.summary),
                ),
                style = DamaskTextStyles.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
