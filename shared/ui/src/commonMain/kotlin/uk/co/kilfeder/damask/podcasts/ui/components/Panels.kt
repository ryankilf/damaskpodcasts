package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.back
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * A pane's own heading on wide layouts, where there's no app bar (design hand-off ›
 * GenreListPane, GenreQueuePane, TabletHome): the title, and an optional line under it.
 */
@Composable
internal fun PaneHeader(
    title: String,
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    subtitle: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.PaneHeaderHeight)
            .padding(start = if (onBack == null) Spacing.Space4 else Spacing.Space1, end = Spacing.Space1)
            .padding(vertical = Spacing.Space2),
    ) {
        onBack?.let { BackButton(it) }
        Column(verticalArrangement = Arrangement.Center, modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = DamaskTextStyles.TitleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() },
            )
            subtitle?.invoke()
        }
        actions()
    }
}

/** How much of primary-container the panel's wash takes; the rest is the page. */
private const val PANEL_WASH = 0.4f

/**
 * The In progress panel's colour: a light wash of primary-container over the page, so it marks
 * the section without shouting, and a highlighted row inside it can stand out ([PrimaryPanel]).
 * Rows inside pass it as their background.
 */
@Composable
internal fun panelColor(): Color {
    val colors = MaterialTheme.colorScheme
    return colors.primaryContainer.copy(alpha = PANEL_WASH).compositeOver(colors.surface)
}

/**
 * The tinted card that holds In progress (design hand-off › Main, GenreQueuePane): a
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
            .background(panelColor())
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
