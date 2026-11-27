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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.readString
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.importer.CategoryOptionUi
import uk.co.kilfeder.damask.podcasts.presentation.importer.ImportRowUi
import uk.co.kilfeder.damask.podcasts.presentation.importer.ImportUiState
import uk.co.kilfeder.damask.podcasts.presentation.importer.ImportViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.Artwork
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
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
        is ImportUiState.Reading -> Progress(
            label = stringResource(Res.string.import_reading, state.done, state.total),
            done = state.done,
            total = state.total,
            modifier = modifier,
        )
        is ImportUiState.Review -> Review(state, onCategoryChange, onConfirm, modifier)
        is ImportUiState.Subscribing -> Progress(
            label = stringResource(Res.string.import_subscribing, state.done, state.total),
            done = state.done,
            total = state.total,
            modifier = modifier,
        )
        is ImportUiState.Done -> Message(
            title = stringResource(Res.string.import_done, state.count),
            modifier = modifier,
        )
        is ImportUiState.Failed -> Message(
            title = stringResource(Res.string.import_failed, state.message),
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
private fun Progress(label: String, done: Int, total: Int, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Space4, Alignment.CenterVertically),
        modifier = modifier.fillMaxSize().padding(Spacing.Space6),
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
            items(state.rows, key = { it.index }) { row ->
                ImportRow(row, state.categories, onCategoryChange = { onCategoryChange(row.index, it) })
            }
        }
        Box(Modifier.fillMaxWidth().padding(Spacing.Space4)) {
            Button(onClick = onConfirm, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.import_confirm, state.rows.size))
            }
        }
    }
}

@Composable
private fun ImportRow(
    row: ImportRowUi,
    categories: List<CategoryOptionUi>,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space3),
        modifier = modifier
            .heightIn(min = Sizes.RowMinHeight)
            .padding(horizontal = Spacing.Space4, vertical = Spacing.Space2),
    ) {
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

@Composable
private fun CategoryPicker(
    row: ImportRowUi,
    categories: List<CategoryOptionUi>,
    onCategoryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val current = categories.firstOrNull { it.id == row.categoryId }?.name.orEmpty()
    val description = stringResource(Res.string.import_category, row.title, current)
    Box(modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.semantics { contentDescription = description },
        ) { Text(current) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.name) },
                    onClick = {
                        expanded = false
                        onCategoryChange(option.id)
                    },
                )
            }
        }
    }
}
