package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.genres.ManageCategoriesUiState
import uk.co.kilfeder.damask.podcasts.presentation.genres.ManageCategoriesViewModel
import uk.co.kilfeder.damask.podcasts.presentation.genres.ManagedCategoryUi
import uk.co.kilfeder.damask.podcasts.ui.components.CategoryNameDialog
import uk.co.kilfeder.damask.podcasts.ui.components.DeleteCategoryDialog
import uk.co.kilfeder.damask.podcasts.ui.components.GenreChip
import uk.co.kilfeder.damask.podcasts.ui.components.GenreColourSheet
import uk.co.kilfeder.damask.podcasts.ui.components.hueName
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.category_new_title
import uk.co.kilfeder.damask.podcasts.ui.resources.category_rename_title
import uk.co.kilfeder.damask.podcasts.ui.resources.colour_change
import uk.co.kilfeder.damask.podcasts.ui.resources.create
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_shows
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_always_grey
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_delete
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_new
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_options
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_rename
import uk.co.kilfeder.damask.podcasts.ui.resources.row_meta
import uk.co.kilfeder.damask.podcasts.ui.resources.save
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing
import uk.co.kilfeder.damask.podcasts.ui.theme.genreTones

/** What a category row can ask for. */
private class CategoryActions(
    val onColour: (ManagedCategoryUi) -> Unit,
    val onRename: (ManagedCategoryUi) -> Unit,
    val onDelete: (ManagedCategoryUi) -> Unit,
)

/** Which sheet or dialog is open. */
private sealed interface Editing {
    data class Colour(val category: ManagedCategoryUi) : Editing
    data class Rename(val category: ManagedCategoryUi) : Editing
    data class Delete(val category: ManagedCategoryUi) : Editing
    data object New : Editing
}

/**
 * Manage categories (design hand-off › ManageCategories): each genre's colour (tap to change),
 * its chip and show count, and a ⋮ with Rename and Delete; Uncategorised is always grey with no
 * menu. "New category" sets one up ahead of time, in the least-used colour.
 */
@Composable
internal fun ManageCategoriesScreen(
    graph: AppGraph,
    modifier: Modifier = Modifier,
    viewModel: ManageCategoriesViewModel = viewModel { graph.manageCategoriesViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Editing?>(null) }
    ManageCategoriesContent(
        state = state,
        actions = CategoryActions(
            onColour = { editing = Editing.Colour(it) },
            onRename = { editing = Editing.Rename(it) },
            onDelete = { editing = Editing.Delete(it) },
        ),
        onNew = { editing = Editing.New },
        modifier = modifier,
    )
    val close = { editing = null }
    when (val open = editing) {
        is Editing.Colour -> GenreColourSheet(
            name = open.category.name,
            selected = open.category.hue,
            onDone = {
                viewModel.onColourChange(open.category.id, it)
                close()
            },
            onDismiss = close,
        )
        is Editing.Rename -> CategoryNameDialog(
            title = stringResource(Res.string.category_rename_title, open.category.name),
            confirm = stringResource(Res.string.save),
            initial = open.category.name,
            onConfirm = {
                viewModel.onRename(open.category.id, it)
                close()
            },
            onDismiss = close,
        )
        is Editing.Delete -> DeleteCategoryDialog(
            name = open.category.name,
            showCount = open.category.showCount,
            onConfirm = {
                viewModel.onDelete(open.category.id)
                close()
            },
            onDismiss = close,
        )
        Editing.New -> CategoryNameDialog(
            title = stringResource(Res.string.category_new_title),
            confirm = stringResource(Res.string.create),
            onConfirm = {
                viewModel.onCreate(it, state.nextHue)
                close()
            },
            onDismiss = close,
        )
        null -> Unit
    }
}

@Composable
private fun ManageCategoriesContent(
    state: ManageCategoriesUiState,
    actions: CategoryActions,
    onNew: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.loading) {
        Loading(modifier)
        return
    }
    LazyColumn(modifier) {
        items(state.categories, key = { it.id }) { CategoryRow(it, actions) }
        item(key = "new") {
            val padding = Modifier.padding(horizontal = Spacing.Space4, vertical = Spacing.Space2)
            OutlinedButton(onClick = onNew, modifier = padding) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(Res.string.manage_new), modifier = Modifier.padding(start = Spacing.Space2))
            }
        }
    }
}

@Composable
private fun CategoryRow(category: ManagedCategoryUi, actions: CategoryActions, modifier: Modifier = Modifier) {
    val shows = pluralStringResource(Res.plurals.genres_shows, category.showCount, category.showCount)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space3),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.ShowRowMinHeight)
            .padding(start = Spacing.Space2, end = Spacing.Space1),
    ) {
        ColourButton(category, enabled = !category.isSystem, onClick = { actions.onColour(category) })
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1 / 2), modifier = Modifier.weight(1f)) {
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
        if (category.isSystem) Spacer(Modifier.size(Sizes.MinTouchTarget)) else CategoryMenu(category, actions)
    }
}

/** The colour dot, a 48dp button that opens the colour sheet. */
@Composable
private fun ColourButton(
    category: ManagedCategoryUi,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = stringResource(Res.string.colour_change, category.name, hueName(category.hue))
    val described = modifier.semantics { contentDescription = label }
    IconButton(onClick = onClick, enabled = enabled, modifier = described) {
        Box(Modifier.size(Sizes.SwatchDot).background(genreTones(category.hue).stripe, CircleShape))
    }
}

@Composable
private fun CategoryMenu(category: ManagedCategoryUi, actions: CategoryActions, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(Res.string.manage_options, category.name))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(stringResource(Res.string.manage_rename)) }, onClick = {
                open = false
                actions.onRename(category)
            })
            DropdownMenuItem(text = { Text(stringResource(Res.string.manage_delete)) }, onClick = {
                open = false
                actions.onDelete(category)
            })
        }
    }
}
