package uk.co.kilfeder.damask.podcasts.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// The only place (with the rest of ui.theme) where dp literals are allowed; see the
// NoHardcodedDimensions detekt rule. Values come from design-system/tokens.json and README.md.
// Need a new size? Add a named token here rather than an inline literal.

/** design-system `spacing` tokens. */
object Spacing {
    /** space-1: between a title and its meta line. */
    val Space1 = 4.dp

    /** space-2: inside chips; between artwork and text. */
    val Space2 = 8.dp

    /** Between artwork and text in episode rows (EpisodeRow artboard; not yet in tokens.json). */
    val Space3 = 12.dp

    /** space-4: screen edge inset and row padding. */
    val Space4 = 16.dp

    /** space-6: between sections (In Progress, Up Next). */
    val Space6 = 24.dp
}

/** design-system `radius` tokens. */
object Radius {
    /** radius-sm: artwork thumbnails in rows, chips. */
    val Small = 8.dp

    /** radius-md: cards, the mini-player, bottom sheets, Now Playing artwork. */
    val Medium = 16.dp
}

/** Fixed component sizes from the design-system README. */
object Sizes {
    /** Minimum touch target, both dimensions (Material / Android accessibility guidance). */
    val MinTouchTarget = 48.dp

    /** Material Symbols Rounded icons. */
    val Icon = 24.dp

    /** Progress bars: half of space-2. */
    val ProgressBarHeight = 4.dp

    /** Row artwork (EpisodeRow artboard). */
    val RowArtwork = 56.dp

    /** Minimum episode row height. */
    val RowMinHeight = 72.dp

    /** The progress ring around a row's play control. */
    val ProgressRingStroke = 3.dp

    /** Import grid tiles: as big as fit, between these. */
    val ImportTileMax = 64.dp
    val ImportTileMin = 32.dp

    /** Readable content width on wide windows. */
    val MaxContentWidth = 640.dp

    /** The Now Playing play/pause button. */
    val NowPlayingPlayButton = 80.dp
}

internal val DamaskShapes = Shapes(
    small = RoundedCornerShape(Radius.Small),
    medium = RoundedCornerShape(Radius.Medium),
)

/** radius-full: progress bars, the play button, category chips. */
val PillShape = CircleShape
