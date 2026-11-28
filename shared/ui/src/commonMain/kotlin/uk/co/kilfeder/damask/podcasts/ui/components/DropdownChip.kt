package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role

/**
 * A chip showing a current choice, with ▾, that opens a list of choices (design hand-off › Show:
 * "History ▾", "Oldest first ▾"). [description] is read out instead of the label.
 */
@Composable
internal fun DropdownChip(
    label: String,
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, contentDescription = null) },
        modifier = modifier.clearAndSetSemantics {
            contentDescription = description
            role = Role.DropdownList
        },
    )
}
