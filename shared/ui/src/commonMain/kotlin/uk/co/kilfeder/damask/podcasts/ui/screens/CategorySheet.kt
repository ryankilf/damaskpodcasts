package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.ui.theme.genreSwatch
import uk.co.kilfeder.damask.podcasts.ui.theme.genreTones
import uk.co.kilfeder.damask.podcasts.ui.resources.category_new_colour
import uk.co.kilfeder.damask.podcasts.ui.resources.category_find_or_create
import uk.co.kilfeder.damask.podcasts.ui.resources.category_create
import uk.co.kilfeder.damask.podcasts.ui.components.hueName
import uk.co.kilfeder.damask.podcasts.ui.components.SearchField
import uk.co.kilfeder.damask.podcasts.ui.components.GenreColourSheet
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.rounded.Add
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.clickable
import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.genres.CategoryOptionUi
import uk.co.kilfeder.damask.podcasts.presentation.genres.matching
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.category_no_match
import uk.co.kilfeder.damask.podcasts.ui.resources.category_search
import uk.co.kilfeder.damask.podcasts.ui.resources.category_sheet_title
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * Something that opens the genre sheet ([trigger] gets the call that opens it), and the sheet.
 * Import's genre button and the show page's genre chip both use it; the show page also passes
 * [create], which adds the "Create “…”" row.
 */
@Composable
internal fun CategoryPicker(
    showTitle: String,
    categories: List<CategoryOptionUi>,
    selectedId: String,
    onSelect: (String) -> Unit,
    create: CreateGenre? = null,
    trigger: @Composable (open: () -> Unit) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    trigger { open = true }
    if (open) {
        CategorySheet(
            showTitle = showTitle,
            categories = categories,
            selectedId = selectedId,
            onSelect = {
                open = false
                onSelect(it)
            },
            create = create?.let { c ->
                CreateGenre(c.nextHue) { name, hue ->
                    open = false
                    c.onCreate(name, hue)
                }
            },
            onDismiss = { open = false },
        )
    }
}

/**
 * Choosing a genre (design hand-off › ShowGenre; also Discover's Subscribe): a full-height sheet, "Genre for <show>", a
 * search field, then every genre A–Z with a radio, its name and a square in its colour. With
 * [create], typing a name no genre has offers "Create “<name>”" at the top, with its colour
 * (the least used, or one picked from the swatches) beside it; creating moves the show there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CategorySheet(
    showTitle: String,
    categories: List<CategoryOptionUi>,
    selectedId: String,
    onSelect: (String) -> Unit,
    create: CreateGenre?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val matches = categories.matching(query)
    val name = query.trim()
    val offerCreate = create != null && name.isNotEmpty() && categories.none { it.name.equals(name, ignoreCase = true) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberBottomSheetState(SheetValue.Hidden, setOf(SheetValue.Hidden, SheetValue.Expanded)),
        modifier = modifier,
    ) {
        // Always full height: the list scrolls inside a fixed area, so filtering changes what's
        // listed but never the sheet's size. imePadding keeps the list above the keyboard.
        Column(Modifier.fillMaxHeight().imePadding()) {
            Text(
                text = stringResource(Res.string.category_sheet_title, showTitle),
                style = DamaskTextStyles.TitleLarge,
                maxLines = 2,
                modifier = Modifier.padding(horizontal = Spacing.Space4).semantics { heading() },
            )
            SearchField(
                query = query,
                onQueryChange = { query = it },
                placeholder = stringResource(
                    if (create != null) Res.string.category_find_or_create else Res.string.category_search,
                ),
                modifier = Modifier.padding(Spacing.Space4),
            )
            LazyColumn(Modifier.weight(1f).selectableGroup()) {
                if (offerCreate && create != null) item(key = "create") { CreateRow(name, create) }
                if (matches.isEmpty() && !offerCreate) {
                    item(key = "none") { EmptyMessage(stringResource(Res.string.category_no_match, query)) }
                }
                items(matches, key = { it.id }) { option ->
                    CategoryOption(option, option.id == selectedId, onClick = { onSelect(option.id) })
                }
            }
        }
    }
}

/** "Create “Irish History”", and its colour as a separate button that opens the swatches. */
@Composable
private fun CreateRow(name: String, create: CreateGenre, modifier: Modifier = Modifier) {
    var hue by remember { mutableStateOf(create.nextHue) }
    var picking by remember { mutableStateOf(false) }
    val primary = MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(end = Spacing.Space2),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Space4),
            modifier = Modifier
                .weight(1f)
                .clickable(role = Role.Button) { create.onCreate(name, hue) }
                .heightIn(min = Sizes.MinTouchTarget)
                .padding(horizontal = Spacing.Space4),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = primary)
            Text(stringResource(Res.string.category_create, name), style = DamaskTextStyles.Body, color = primary)
        }
        val label = stringResource(Res.string.category_new_colour, hueName(hue))
        IconButton(onClick = { picking = true }, modifier = Modifier.semantics { contentDescription = label }) {
            GenreSquare(hue, ringed = true)
        }
    }
    if (picking) {
        GenreColourSheet(name, hue, onDone = {
            hue = it
            picking = false
        }, onDismiss = { picking = false })
    }
}

@Composable
private fun CategoryOption(
    option: CategoryOptionUi,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space4),
        modifier = modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = Sizes.MinTouchTarget)
            .padding(horizontal = Spacing.Space4),
    ) {
        // The row is the touch target; the radio button only shows the state.
        RadioButton(selected = selected, onClick = null)
        Text(option.name, style = DamaskTextStyles.Body, modifier = Modifier.weight(1f))
        GenreSquare(option.hue)
    }
}

/** A genre's colour as a small rounded square (the sheet's rows and its create row). */
@Composable
private fun GenreSquare(hue: GenreHue, modifier: Modifier = Modifier, ringed: Boolean = false) {
    val colors = MaterialTheme.colorScheme
    // The create row's square is a button, so it gets the stronger outline.
    val edge = if (ringed) colors.outline else colors.outlineVariant
    Box(modifier.size(Sizes.GenreSquare).genreSwatch(genreTones(hue), edge, MaterialTheme.shapes.extraSmall))
}
