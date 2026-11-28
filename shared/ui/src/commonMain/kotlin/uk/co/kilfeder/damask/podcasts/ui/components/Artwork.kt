package uk.co.kilfeder.damask.podcasts.ui.components

import uk.co.kilfeder.damask.podcasts.ui.theme.genreTones
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Box
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

/** Initials take this share of the tile's side. */
private const val INITIALS_SCALE = 0.32f

/**
 * Show or episode artwork. Decorative: the title next to it says what it is, so screen readers
 * skip it. Behind it sits the show's [fallback] tile (its initials on its genre's colour), which shows
 * while the image loads, and in its place when there's none or it fails.
 *
 * A faint hairline gives it an edge, because a lot of covers are mostly white (or, in dark mode,
 * mostly black) and would otherwise melt into the page. It's `onSurface` at low alpha, so it
 * shows only where the cover is close to the page colour.
 */
@Composable
internal fun Artwork(
    url: String?,
    fallback: ArtworkFallback,
    modifier: Modifier = Modifier,
    size: Dp = Sizes.RowArtwork,
    shape: Shape = MaterialTheme.shapes.small,
) {
    val edge = MaterialTheme.colorScheme.onSurface.copy(alpha = EDGE_ALPHA)
    val tones = genreTones(fallback.hue)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(tones.container),
    ) {
        val initialsSize = with(LocalDensity.current) { (size * INITIALS_SCALE).toSp() }
        Text(
            text = fallback.initials,
            color = tones.onContainer,
            fontSize = initialsSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().border(Sizes.ArtworkHairline, edge, shape),
        )
    }
}
