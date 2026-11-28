package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import uk.co.kilfeder.damask.podcasts.presentation.episodes.NextUpUi
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

/** The page's cover, small, for a collapsed bar ([CollapsingTopBar]). */
@Composable
internal fun BarArtwork(url: String?, fallback: ArtworkFallback, modifier: Modifier = Modifier) {
    Artwork(url, fallback, modifier, size = Sizes.BarArtwork)
}

/**
 * The round play button in a collapsed bar, standing in for the page's big Resume / Play once it
 * has scrolled away: plays or pauses [next] in place, from [source].
 */
@Composable
internal fun BarPlayButton(next: NextUpUi, player: RowPlayer, source: PlaySource, modifier: Modifier = Modifier) {
    PlayButton(
        size = Sizes.MinTouchTarget,
        iconSize = Sizes.Icon,
        pitch = Sizes.DamaskPitchSmall,
        playing = player.isPlaying(next.id),
        enabled = true,
        onClick = { player.playPause(next.id, source) },
        modifier = modifier,
    )
}
