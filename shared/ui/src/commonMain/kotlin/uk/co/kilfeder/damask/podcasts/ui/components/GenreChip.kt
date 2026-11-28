package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.PillShape
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** A show's genre as a small tertiary pill (design hand-off › EpisodeRow, NowPlayingPane). */
@Composable
internal fun GenreChip(genre: String, modifier: Modifier = Modifier) {
    Text(
        text = genre,
        style = DamaskTextStyles.Label,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        maxLines = 1,
        modifier = modifier
            .background(MaterialTheme.colorScheme.tertiaryContainer, PillShape)
            .padding(horizontal = Spacing.Space2, vertical = Spacing.Space1 / 2),
    )
}
