package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.genreTones
import uk.co.kilfeder.damask.podcasts.ui.theme.PillShape
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** A show's genre as a small pill in its genre colour (design hand-off › EpisodeRow, NowPlayingPane). */
@Composable
internal fun GenreChip(genre: String, hue: GenreHue, modifier: Modifier = Modifier) {
    val tones = genreTones(hue)
    Text(
        text = genre,
        style = DamaskTextStyles.Label,
        color = tones.onContainer,
        maxLines = 1,
        modifier = modifier
            .background(tones.container, PillShape)
            .padding(horizontal = Spacing.Space2, vertical = Spacing.Space1 / 2),
    )
}
