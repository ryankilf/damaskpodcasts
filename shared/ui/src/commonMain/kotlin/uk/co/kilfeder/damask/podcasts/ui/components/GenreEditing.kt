package uk.co.kilfeder.damask.podcasts.ui.components

import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import androidx.compose.ui.unit.DpSize
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue
import uk.co.kilfeder.damask.podcasts.presentation.genres.ManagedGenreUi
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.category_new_title
import uk.co.kilfeder.damask.podcasts.ui.resources.category_rename_title
import uk.co.kilfeder.damask.podcasts.ui.resources.create
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_colour
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_delete
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_options
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_rename
import uk.co.kilfeder.damask.podcasts.ui.resources.save

/** Which genre sheet or dialog is open. */
internal sealed interface GenreEdit {
    data class Colour(val genre: ManagedGenreUi) : GenreEdit
    data class Rename(val genre: ManagedGenreUi) : GenreEdit
    data class Delete(val genre: ManagedGenreUi) : GenreEdit
    data class New(val hue: GenreHue) : GenreEdit
}

/** What's being edited on a screen; [GenreEditDialogs] shows it. */
@Stable
internal class GenreEditState {
    var open: GenreEdit? by mutableStateOf(null)
}

@Composable
internal fun rememberGenreEditState(): GenreEditState = remember { GenreEditState() }

/**
 * A genre's ⋮ (Manage genres rows, a genre's own page): Change colour, Rename, Delete. Not for
 * Uncategorised, which has none of these.
 */
@Composable
internal fun GenreMenu(
    genre: ManagedGenreUi,
    state: GenreEditState,
    modifier: Modifier = Modifier,
    target: DpSize = DpSize(Sizes.MinTouchTarget, Sizes.MinTouchTarget),
) {
    var expanded by remember { mutableStateOf(false) }
    val pick = { edit: GenreEdit ->
        expanded = false
        state.open = edit
    }
    Box(modifier) {
        RoomyIconButton(
            onClick = { expanded = true },
            label = stringResource(Res.string.manage_options, genre.name),
            size = target,
        ) { Icon(Icons.Rounded.MoreVert, contentDescription = null) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.manage_colour)) },
                onClick = { pick(GenreEdit.Colour(genre)) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.manage_rename)) },
                onClick = { pick(GenreEdit.Rename(genre)) },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.manage_delete)) },
                onClick = { pick(GenreEdit.Delete(genre)) },
            )
        }
    }
}

/** What the genre sheet and dialogs ask for (a ViewModel's GenreEdits intents). */
internal class GenreEditIntents(
    val onColourChange: (id: String, hue: GenreHue) -> Unit,
    val onRename: (id: String, name: String) -> Unit,
    val onDelete: (id: String) -> Unit,
    val onCreate: (name: String, hue: GenreHue) -> Unit,
    /** After a delete: the genre's own page closes. */
    val onLeave: () -> Unit = {},
)

/** The colour sheet, name dialog or delete dialog [state] has open, wired to [editor]. */
@Composable
internal fun GenreEditDialogs(state: GenreEditState, editor: GenreEditIntents) {
    val close = { state.open = null }
    when (val open = state.open) {
        is GenreEdit.Colour -> GenreColourSheet(
            name = open.genre.name,
            selected = open.genre.hue,
            onDone = {
                editor.onColourChange(open.genre.id, it)
                close()
            },
            onDismiss = close,
        )
        is GenreEdit.Rename -> CategoryNameDialog(
            title = stringResource(Res.string.category_rename_title, open.genre.name),
            confirm = stringResource(Res.string.save),
            initial = open.genre.name,
            onConfirm = {
                editor.onRename(open.genre.id, it)
                close()
            },
            onDismiss = close,
        )
        is GenreEdit.Delete -> DeleteCategoryDialog(
            name = open.genre.name,
            showCount = open.genre.showCount,
            onConfirm = {
                editor.onDelete(open.genre.id)
                close()
                editor.onLeave()
            },
            onDismiss = close,
        )
        is GenreEdit.New -> CategoryNameDialog(
            title = stringResource(Res.string.category_new_title),
            confirm = stringResource(Res.string.create),
            onConfirm = {
                editor.onCreate(it, open.hue)
                close()
            },
            onDismiss = close,
        )
        null -> Unit
    }
}
