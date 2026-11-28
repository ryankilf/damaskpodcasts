package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.search_clear

/**
 * The pill-shaped search field (design hand-off › ShowListPane, ShowGenre): a search icon,
 * [placeholder] until you type, and a clear button once you have. Shows' "Search your shows" and
 * the genre sheet's "Find or create a category".
 */
@Composable
internal fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    TextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
        trailingIcon = if (query.isEmpty()) {
            null
        } else {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.search_clear))
                }
            }
        },
        shape = CircleShape,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = colors.surfaceContainerHighest,
            unfocusedContainerColor = colors.surfaceContainerHighest,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = modifier.fillMaxWidth().semantics { contentDescription = placeholder },
    )
}
