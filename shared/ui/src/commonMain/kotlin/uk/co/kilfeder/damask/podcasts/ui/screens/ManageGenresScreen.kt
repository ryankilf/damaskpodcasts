package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import uk.co.kilfeder.damask.podcasts.ui.components.RoomyIconButton
import uk.co.kilfeder.damask.podcasts.ui.theme.genreSwatch
import uk.co.kilfeder.damask.podcasts.ui.components.rememberGenreEditState
import uk.co.kilfeder.damask.podcasts.ui.components.GenreMenu
import uk.co.kilfeder.damask.podcasts.ui.components.GenreEditDialogs
import uk.co.kilfeder.damask.podcasts.ui.components.GenreEditIntents
import uk.co.kilfeder.damask.podcasts.ui.components.GenreEditState
import uk.co.kilfeder.damask.podcasts.ui.components.GenreEdit
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.genres.ManageGenresUiState
import uk.co.kilfeder.damask.podcasts.presentation.genres.ManageGenresViewModel
import uk.co.kilfeder.damask.podcasts.presentation.genres.ManagedGenreUi
import uk.co.kilfeder.damask.podcasts.ui.components.GenreChip
import uk.co.kilfeder.damask.podcasts.ui.components.hueName
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.colour_change
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_shows
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_always_grey
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_new
import uk.co.kilfeder.damask.podcasts.ui.resources.row_meta
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing
import uk.co.kilfeder.damask.podcasts.ui.theme.genreTones

/**
 * Manage genres (design hand-off › ManageCategories): each genre's colour (tap the dot, or
 * Change colour in its ⋮), its chip and show count, and Rename and Delete in the ⋮; Uncategorised
 * is always grey with no menu. "New genre" sets one up ahead of time, in the least-used colour.
 */
@Composable
internal fun ManageGenresScreen(
    graph: AppGraph,
    modifier: Modifier = Modifier,
    viewModel: ManageGenresViewModel = viewModel { graph.manageGenresViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editing = rememberGenreEditState()
    ManageGenresContent(state, editing, modifier)
    GenreEditDialogs(
        state = editing,
        editor = GenreEditIntents(
            onColourChange = viewModel::onColourChange,
            onRename = viewModel::onRename,
            onDelete = viewModel::onDelete,
            onCreate = viewModel::onCreate,
        ),
    )
}

@Composable
private fun ManageGenresContent(state: ManageGenresUiState, editing: GenreEditState, modifier: Modifier = Modifier) {
    Loadable(state.loading, modifier) {
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.categories, key = { it.id }) { CategoryRow(it, editing) }
            item(key = "new") {
                val padding = Modifier.padding(horizontal = Spacing.Space4, vertical = Spacing.Space2)
                OutlinedButton(onClick = { editing.open = GenreEdit.New(state.nextHue) }, modifier = padding) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text(stringResource(Res.string.manage_new), modifier = Modifier.padding(start = Spacing.Space2))
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(category: ManagedGenreUi, editing: GenreEditState, modifier: Modifier = Modifier) {
    val shows = pluralStringResource(Res.plurals.genres_shows, category.showCount, category.showCount)
    // The dot's and ⋮'s touch targets fill the row's height and run to the screen edges.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().heightIn(min = Sizes.ShowRowMinHeight),
    ) {
        ColourButton(category, enabled = !category.isSystem, onClick = { editing.open = GenreEdit.Colour(category) })
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Space1 / 2),
            modifier = Modifier.weight(1f).padding(start = Spacing.Space1, end = Spacing.Space2),
        ) {
            GenreChip(category.name, category.hue)
            Text(
                text = if (category.isSystem) {
                    stringResource(Res.string.row_meta, shows, stringResource(Res.string.manage_always_grey))
                } else {
                    shows
                },
                style = DamaskTextStyles.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (category.isSystem) {
            Spacer(Modifier.size(Sizes.ManageMenuTarget))
        } else {
            GenreMenu(category, editing, target = Sizes.ManageMenuTarget)
        }
    }
}

/** The colour dot, which opens the colour sheet; tappable across the row's whole left end. */
@Composable
private fun ColourButton(
    category: ManagedGenreUi,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.colour_change, category.name, hueName(category.hue))
    RoomyIconButton(onClick, label, Sizes.ManageDotTarget, modifier, enabled) {
        val edge = MaterialTheme.colorScheme.outlineVariant
        Box(Modifier.size(Sizes.SwatchDot).genreSwatch(genreTones(category.hue), edge, CircleShape))
    }
}
