package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.selection.triStateToggleable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.TriStateCheckbox
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.importer.CategoryOptionUi
import uk.co.kilfeder.damask.podcasts.presentation.importer.ImportRowUi
import uk.co.kilfeder.damask.podcasts.presentation.importer.ImportUiState
import uk.co.kilfeder.damask.podcasts.presentation.importer.ImportViewModel
import uk.co.kilfeder.damask.podcasts.presentation.importer.matching
import uk.co.kilfeder.damask.podcasts.ui.components.Artwork
import uk.co.kilfeder.damask.podcasts.ui.components.LatestShowCard
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.category_no_match
import uk.co.kilfeder.damask.podcasts.ui.resources.category_search
import uk.co.kilfeder.damask.podcasts.ui.resources.category_sheet_title
import uk.co.kilfeder.damask.podcasts.ui.resources.import_body
import uk.co.kilfeder.damask.podcasts.ui.resources.import_category
import uk.co.kilfeder.damask.podcasts.ui.resources.import_choose
import uk.co.kilfeder.damask.podcasts.ui.resources.import_confirm
import uk.co.kilfeder.damask.podcasts.ui.resources.import_done
import uk.co.kilfeder.damask.podcasts.ui.resources.import_failed
import uk.co.kilfeder.damask.podcasts.ui.resources.import_problem
import uk.co.kilfeder.damask.podcasts.ui.resources.import_reading
import uk.co.kilfeder.damask.podcasts.ui.resources.import_review_body
import uk.co.kilfeder.damask.podcasts.ui.resources.import_review_title
import uk.co.kilfeder.damask.podcasts.ui.resources.import_start_again
import uk.co.kilfeder.damask.podcasts.ui.resources.import_all_shows
import uk.co.kilfeder.damask.podcasts.ui.resources.import_subscribing
import uk.co.kilfeder.damask.podcasts.ui.resources.import_title
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** Import: choose an OPML file, confirm or change each show's category, subscribe. */
@Composable
internal fun ImportScreen(
    graph: AppGraph,
    modifier: Modifier = Modifier,
    viewModel: ImportViewModel = viewModel { graph.importViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val picker = rememberFilePickerLauncher(type = FileKitType.File("opml", "xml")) { file ->
        if (file != null) scope.launch { viewModel.onFileChosen(file.readString()) }
    }
    ImportContent(
        state = state,
        onChooseFile = { picker.launch() },
        onCategoryChange = viewModel::onCategoryChanged,
        onSelectionChange = viewModel::onSelectionChanged,
        onSelectAllChange = viewModel::onSelectAllChanged,
        onConfirm = viewModel::onConfirm,
        onStartAgain = viewModel::onStartAgain,
        modifier = modifier,
    )
}

@Composable
private fun ImportContent(
    state: ImportUiState,
    onChooseFile: () -> Unit,
    onCategoryChange: (Int, String) -> Unit,
    onSelectionChange: (Int, Boolean) -> Unit,
    onSelectAllChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    onStartAgain: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        ImportUiState.ChooseFile -> Message(
            title = stringResource(Res.string.import_title),
            body = stringResource(Res.string.import_body),
            action = stringResource(Res.string.import_choose),
            onAction = onChooseFile,
            modifier = modifier,
        )
        is ImportUiState.Reading -> Column(modifier.fillMaxSize()) {
            LatestShowCard(state.latest, Modifier.weight(1f).fillMaxWidth().padding(Spacing.Space4))
            Progress(
                label = stringResource(Res.string.import_reading, state.done, state.total),
                done = state.done,
                total = state.total,
                modifier = Modifier.fillMaxWidth(),
                fill = false,
            )
        }
        is ImportUiState.Review -> Review(
            state = state,
            onCategoryChange = onCategoryChange,
            onSelectionChange = onSelectionChange,
            onSelectAllChange = onSelectAllChange,
            onConfirm = onConfirm,
            modifier = modifier,
        )
        is ImportUiState.Subscribing -> Column(modifier.fillMaxSize()) {
            LatestShowCard(state.latest, Modifier.weight(1f).fillMaxWidth().padding(Spacing.Space4))
            Progress(
                label = stringResource(Res.string.import_subscribing, state.done, state.total),
                done = state.done,
                total = state.total,
                modifier = Modifier.fillMaxWidth(),
                fill = false,
            )
        }
        is ImportUiState.Done -> Message(
            title = stringResource(Res.string.import_done, state.count),
            modifier = modifier,
        )
        is ImportUiState.Failed -> Message(
            title = stringResource(Res.string.import_failed),
            action = stringResource(Res.string.import_start_again),
            onAction = onStartAgain,
            modifier = modifier,
        )
    }
}

@Composable
private fun Message(
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    action: String? = null,
    onAction: () -> Unit = {},
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Space4),
        modifier = modifier.fillMaxSize().padding(Spacing.Space6),
    ) {
        Text(title, style = DamaskTextStyles.TitleLarge, modifier = Modifier.semantics { heading() })
        body?.let { Text(it, style = DamaskTextStyles.Body, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        action?.let { Button(onClick = onAction) { Text(it) } }
    }
}

/** A determinate bar once the total is known (before then, an indeterminate one). */
@Composable
private fun Progress(label: String, done: Int, total: Int, modifier: Modifier = Modifier, fill: Boolean = true) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Space4, Alignment.CenterVertically),
        modifier = (if (fill) modifier.fillMaxSize() else modifier).padding(Spacing.Space6),
    ) {
        if (total > 0) {
            LinearProgressIndicator(progress = { done.toFloat() / total }, modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        Text(label, style = DamaskTextStyles.Body)
    }
}

@Composable
private fun Review(
    state: ImportUiState.Review,
    onCategoryChange: (Int, String) -> Unit,
    onSelectionChange: (Int, Boolean) -> Unit,
    onSelectAllChange: (Boolean) -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f)) {
            item(key = "intro") {
                Column(
                    verticalArrangement = Arrangement.spacedBy(Spacing.Space2),
                    modifier = Modifier.padding(Spacing.Space4),
                ) {
                    Text(
                        text = stringResource(Res.string.import_review_title),
                        style = DamaskTextStyles.TitleLarge,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = stringResource(Res.string.import_review_body),
                        style = DamaskTextStyles.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "select-all") { SelectAll(state, onSelectAllChange) }
            items(state.rows, key = { it.index }) { row ->
                ImportRow(
                    row = row,
                    categories = state.categories,
                    onCategoryChange = { onCategoryChange(row.index, it) },
                    onSelectionChange = { onSelectionChange(row.index, it) },
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(Spacing.Space4)) {
            Button(onClick = onConfirm, enabled = state.selectedCount > 0, modifier = Modifier.fillMaxWidth()) {
                Text(pluralStringResource(Res.plurals.import_confirm, state.selectedCount, state.selectedCount))
            }
        }
    }
}

@Composable
private fun ImportRow(
    row: ImportRowUi,
    categories: List<CategoryOptionUi>,
    onCategoryChange: (String) -> Unit,
    onSelectionChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The whole row toggles; the category button inside keeps its own tap.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space2),
        modifier = modifier
            .toggleable(value = row.selected, role = Role.Checkbox, onValueChange = onSelectionChange)
            .heightIn(min = Sizes.RowMinHeight)
            .padding(start = Spacing.Space4, end = Spacing.Space4, top = Spacing.Space2, bottom = Spacing.Space2),
    ) {
        Checkbox(checked = row.selected, onCheckedChange = null)
        Artwork(row.artworkUrl)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Space1)) {
            Text(row.title, style = DamaskTextStyles.Title, maxLines = 2)
            row.problem?.let {
                Text(
                    text = stringResource(Res.string.import_problem, it),
                    style = DamaskTextStyles.BodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            CategoryPicker(row, categories, onCategoryChange)
        }
    }
}

/** Ticks every show, or unticks them all when every show is already ticked. */
@Composable
private fun SelectAll(
    state: ImportUiState.Review,
    onSelectAllChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggle = when {
        state.allSelected -> ToggleableState.On
        state.selectedCount == 0 -> ToggleableState.Off
        else -> ToggleableState.Indeterminate
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space2),
        modifier = modifier
            .fillMaxWidth()
            .triStateToggleable(
                state = toggle,
                role = Role.Checkbox,
                onClick = { onSelectAllChange(!state.allSelected) },
            )
            .heightIn(min = Sizes.MinTouchTarget)
            .padding(horizontal = Spacing.Space4),
    ) {
        TriStateCheckbox(state = toggle, onClick = null)
        Text(stringResource(Res.string.import_all_shows), style = DamaskTextStyles.Title)
    }
}

@Composable
private fun CategoryPicker(
    row: ImportRowUi,
    categories: List<CategoryOptionUi>,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val current = categories.firstOrNull { it.id == row.categoryId }?.name.orEmpty()
    val description = stringResource(Res.string.import_category, row.title, current)
    OutlinedButton(
        onClick = { open = true },
        modifier = modifier.semantics { contentDescription = description },
    ) { Text(current) }
    if (open) {
        CategorySheet(
            showTitle = row.title,
            categories = categories,
            selectedId = row.categoryId,
            onSelect = {
                open = false
                onCategoryChange(it)
            },
            onDismiss = { open = false },
        )
    }
}

/** A searchable list of categories in a bottom sheet; the current one is marked. */
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
        LazyColumn(Modifier.selectableGroup()) {
            if (matches.isEmpty()) {
                item(key = "none") { EmptyMessage(stringResource(Res.string.category_no_match, query)) }
            }
            items(matches, key = { it.id }) { option ->
                CategoryOption(option.name, option.id == selectedId, onClick = { onSelect(option.id) })
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
