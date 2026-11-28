package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.ui.components.damaskFill
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.load_more
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

@Composable
internal fun Loading(modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center, modifier = modifier.fillMaxSize()) {
        CircularProgressIndicator()
    }
}

@Composable
internal fun EmptyMessage(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = DamaskTextStyles.Body,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(Spacing.Space4),
    )
}

/**
 * The shared empty state (design hand-off › EmptyState): a damask tile (the tone-on-tone pattern
 * on surface-container-low) holding an icon, then a plain title, one line of why, an optional
 * [meta] line, and [actions]: at most one filled button, and a text button. No illustrations.
 */
@Composable
internal fun EmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    meta: (@Composable () -> Unit)? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Space4, Alignment.CenterVertically),
        modifier = modifier.fillMaxSize().padding(horizontal = Spacing.Space8, vertical = Spacing.Space6),
    ) {
        DamaskTile(icon)
        Text(
            text = title,
            style = DamaskTextStyles.TitleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = Spacing.Space2).semantics { heading() },
        )
        Text(
            text = body,
            style = DamaskTextStyles.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = Sizes.EmptyStateTextMaxWidth),
        )
        meta?.invoke()
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Space2),
            modifier = Modifier.padding(top = Spacing.Space2),
            content = actions,
        )
    }
}

/** The empty state's damask tile, with the icon in a surface-coloured circle. Decorative. */
@Composable
private fun DamaskTile(icon: ImageVector, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(Sizes.EmptyStateTile)
            .clip(MaterialTheme.shapes.medium)
            .background(colors.surfaceContainerLow)
            .damaskFill(colors.onSurfaceVariant, Sizes.DamaskPitchPanel, alpha = PANEL_PATTERN_ALPHA),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(Sizes.EmptyStateIconCircle).clip(CircleShape).background(colors.surface),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSurfaceVariant,
                modifier = Modifier.size(Sizes.EmptyStateIcon),
            )
        }
    }
}

/** The empty-state tile's pattern is a little lighter than the play button's. */
private const val PANEL_PATTERN_ALPHA = 0.14f

internal fun LazyListScope.sectionHeader(key: String, stringResourceKey: StringResource) {
    item(key = "header-$key") {
        Text(
            text = stringResource(stringResourceKey),
            // The design's section headings ("Up next") use the title style, like the panel's.
            style = DamaskTextStyles.Title,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(start = Spacing.Space4, end = Spacing.Space4, top = Spacing.Space6, bottom = Spacing.Space2)
                .semantics { heading() },
        )
    }
}

internal fun LazyListScope.loadMore(visible: Boolean, onLoadMore: () -> Unit) {
    if (!visible) return
    item(key = "load-more") {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().padding(Spacing.Space2)) {
            TextButton(onClick = onLoadMore) { Text(stringResource(Res.string.load_more)) }
        }
    }
}
