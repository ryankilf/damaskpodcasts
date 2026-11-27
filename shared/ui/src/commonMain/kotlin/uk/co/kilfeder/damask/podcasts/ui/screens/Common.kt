package uk.co.kilfeder.damask.podcasts.ui.screens

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

/** A centred empty state: what this screen is for, and how to fill it, with optional [actions]. */
@Composable
internal fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Space2, Alignment.CenterVertically),
        modifier = modifier.fillMaxSize().padding(Spacing.Space6),
    ) {
        Text(
            text = title,
            style = DamaskTextStyles.TitleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            text = body,
            style = DamaskTextStyles.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        actions()
    }
}

internal fun LazyListScope.sectionHeader(key: String, stringResourceKey: StringResource) {
    item(key = "header-$key") {
        Text(
            text = stringResource(stringResourceKey),
            style = DamaskTextStyles.TitleLarge,
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
