package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.search_coming

/** Placeholder until the search gateway (backend branch) is merged. */
@Composable
internal fun SearchScreen(modifier: Modifier = Modifier) {
    EmptyMessage(stringResource(Res.string.search_coming), modifier)
}
