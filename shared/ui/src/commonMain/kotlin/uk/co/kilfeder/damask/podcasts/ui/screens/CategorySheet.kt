package uk.co.kilfeder.damask.podcasts.ui.screens

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
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
 * Import's genre button and the show page's genre chip both use it.
 */
@Composable
internal fun CategoryPicker(
    showTitle: String,
    categories: List<CategoryOptionUi>,
    selectedId: String,
    onSelect: (String) -> Unit,
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
            onDismiss = { open = false },
        )
    }
}

/**
 * Choosing a genre: a searchable list in a full-height bottom sheet, the current one marked.
 * Used wherever a show's genre is picked (Import's review, a show's page).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategorySheet(
    showTitle: String,
    categories: List<CategoryOptionUi>,
    selectedId: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by remember { mutableStateOf("") }
    val matches = categories.matching(query)
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
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text(stringResource(Res.string.category_search)) },
                leadingIcon = { Icon(imageVector = Icons.Rounded.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth().padding(Spacing.Space4),
            )
            LazyColumn(Modifier.weight(1f).selectableGroup()) {
                if (matches.isEmpty()) {
                    item(key = "none") { EmptyMessage(stringResource(Res.string.category_no_match, query)) }
                }
                items(matches, key = { it.id }) { option ->
                    CategoryOption(option.name, option.id == selectedId, onClick = { onSelect(option.id) })
                }
            }
        }
    }
}

@Composable
private fun CategoryOption(name: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
        Text(name, style = DamaskTextStyles.Body)
    }
}
