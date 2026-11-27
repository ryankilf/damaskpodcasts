package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

/** How strong artwork's hairline edge is. */
private const val EDGE_ALPHA = 0.16f

/**
 * Show or episode artwork. Decorative: the title next to it says what it is, so screen readers
 * skip it. A faint hairline gives it an edge, because a lot of covers are mostly white (or, in
 * dark mode, mostly black) and would otherwise melt into the page. It's `onSurface` at low
 * alpha, so it shows only where the cover is close to the page colour: a light cover gets a
 * dark edge in light mode, a dark cover a light edge in dark mode, and coloured covers barely
 * change.
 */
@Composable
internal fun Artwork(
    url: String?,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.RowArtwork,
) {
    val edge = MaterialTheme.colorScheme.onSurface.copy(alpha = EDGE_ALPHA)
    AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(Sizes.ArtworkHairline, edge, MaterialTheme.shapes.small),
    )
}
