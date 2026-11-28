package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.layout.Row
import uk.co.kilfeder.damask.podcasts.ui.components.OrderChip
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        OrderChip(
            selected = order,
            options = options,
            nameOf = { orderName(it) },
            label = stringResource(Res.string.show_order_label, orderName(order)),
            onSelect = onOrderChange,
        )
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
