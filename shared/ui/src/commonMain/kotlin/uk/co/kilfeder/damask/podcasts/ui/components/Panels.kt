package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * A pane's own heading on wide layouts, where there's no app bar (design hand-off ›
 * GenreListPane, GenreQueuePane, TabletHome): the title, and an optional line under it.
 */
@Composable
internal fun PaneHeader(title: String, modifier: Modifier = Modifier, subtitle: @Composable (() -> Unit)? = null) {
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.PaneHeaderHeight)
            .padding(horizontal = Spacing.Space4, vertical = Spacing.Space2),
    ) {
        Text(
            text = title,
            style = DamaskTextStyles.TitleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() },
        )
        subtitle?.invoke()
    }
}

/**
 * The primary-container card that holds In progress (design hand-off › Main, GenreQueuePane): a
 * heading, an optional count beside it, then [content].
 */
@Composable
internal fun PrimaryPanel(
    title: String,
    modifier: Modifier = Modifier,
    count: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val onPanel = MaterialTheme.colorScheme.onPrimaryContainer
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.Space3)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(vertical = Spacing.Space2),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.Space2),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Spacing.Space4, end = Spacing.Space1, top = Spacing.Space2),
        ) {
            Text(title, style = DamaskTextStyles.Title, color = onPanel, modifier = Modifier.semantics { heading() })
            count?.let { Text(it, style = DamaskTextStyles.BodySmall, color = onPanel) }
        }
        content()
    }
}
