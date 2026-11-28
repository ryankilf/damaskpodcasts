package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.ui.components.DropdownChip
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.show_order_label
import uk.co.kilfeder.damask.podcasts.ui.resources.show_order_newest
import uk.co.kilfeder.damask.podcasts.ui.resources.show_order_oldest
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing
import uk.co.kilfeder.damask.podcasts.presentation.episodes.ListOrder
import uk.co.kilfeder.damask.podcasts.ui.resources.order_variety

/**
 * A list's heading with its order chip ("Oldest first ▾"), which is also the order it plays in.
 * [options] are the orders this list offers: a show has newest and oldest first, a genre variety too.
 */
@Composable
internal fun OrderedHeading(
    title: String,
    order: ListOrder,
    options: List<ListOrder>,
    onOrderChange: (ListOrder) -> Unit,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    val name = orderName(order)
    val label = stringResource(Res.string.show_order_label, name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(start = Spacing.Space4, end = Spacing.Space4),
    ) {
        Text(
            text = title,
            style = DamaskTextStyles.Title,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Box {
            DropdownChip(name, Icons.Rounded.SwapVert, label, onClick = { open = true })
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(orderName(option)) },
                        trailingIcon = if (option == order) {
                            { Icon(Icons.Rounded.Check, contentDescription = null) }
                        } else {
                            null
                        },
                        onClick = {
                            open = false
                            onOrderChange(option)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun orderName(order: ListOrder): String = stringResource(
    when (order) {
        ListOrder.VARIETY -> Res.string.order_variety
        ListOrder.NEWEST_FIRST -> Res.string.show_order_newest
        ListOrder.OLDEST_FIRST -> Res.string.show_order_oldest
    },
)
