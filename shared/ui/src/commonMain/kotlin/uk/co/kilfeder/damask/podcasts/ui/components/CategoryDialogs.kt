package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.cancel
import uk.co.kilfeder.damask.podcasts.ui.resources.category_delete_body
import uk.co.kilfeder.damask.podcasts.ui.resources.category_delete_empty
import uk.co.kilfeder.damask.podcasts.ui.resources.category_delete_title
import uk.co.kilfeder.damask.podcasts.ui.resources.category_name
import uk.co.kilfeder.damask.podcasts.ui.resources.manage_delete

/** Asks for a category's name: creating one ([confirm] "Create") or renaming one ("Save"). */
@Composable
internal fun CategoryNameDialog(
    title: String,
    confirm: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initial: String = "",
) {
    TextEntryDialog(
        title = title,
        label = stringResource(Res.string.category_name),
        confirm = confirm,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        modifier = modifier,
        initial = initial,
    )
}

/** Deleting a category moves its shows to Uncategorised, so it asks first and says so. */
@Composable
internal fun DeleteCategoryDialog(
    name: String,
    showCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.category_delete_title, name)) },
        text = {
            Text(
                if (showCount == 0) {
                    stringResource(Res.string.category_delete_empty)
                } else {
                    pluralStringResource(Res.plurals.category_delete_body, showCount, showCount)
                },
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.manage_delete)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.cancel)) } },
        modifier = modifier,
    )
}
