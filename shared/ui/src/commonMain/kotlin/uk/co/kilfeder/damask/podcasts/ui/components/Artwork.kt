package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

/**
 * Show or episode artwork. Decorative: the title next to it says what it is, so screen readers
 * skip it.
 */
@Composable
internal fun Artwork(
    url: String?,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.RowArtwork,
) {
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
    )
}
