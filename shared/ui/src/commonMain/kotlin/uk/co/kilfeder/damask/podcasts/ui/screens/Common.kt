package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.layout.Box
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
