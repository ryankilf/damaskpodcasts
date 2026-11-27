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

    /** How far each cover behind the front one peeks out (genre rows' stacked covers). */
    val CoverStackPeek = 8.dp

    /** The edge drawn round artwork, so white or black covers don't melt into the page. */
    val ArtworkHairline = 1.dp

    /** Minimum episode row height. */
    val RowMinHeight = 72.dp

    /** The progress ring around a row's play control. */
    val ProgressRingStroke = 3.dp

    /** Ambient light around artwork (NowPlayingPane artboard): how far it spreads, and its blur. */
    val AmbientGlowInset = 40.dp
    val AmbientGlowBlur = 40.dp

    /** Now Playing artwork, at most (NowPlayingPane artboard); it shrinks on small screens. */
    val NowPlayingArtworkMax = 340.dp

    /** Now Playing artwork, at least, before the screen scrolls instead. */
    val NowPlayingArtworkMin = 160.dp

    /** Now Playing's back 10 / forward 30 buttons, and their icons. */
    val NowPlayingSkipButton = 56.dp
    val NowPlayingSkipIcon = 32.dp

    /** The icon inside the 80dp play button. */
    val NowPlayingPlayIcon = 40.dp

    /** The damask diaper's pitch on the 80dp play button, and on 36–48dp ones. */
    val DamaskPitchLarge = 14.dp
    val DamaskPitchSmall = 9.dp

    /** The position bar's thumb (NowPlayingPane artboard). */
    val ScrubberThumbWidth = 4.dp
    val ScrubberThumbHeight = 20.dp

    /** The show notes sheet's top corners, and its drag handle. */
    val SheetCornerRadius = 28.dp
    val SheetHandleWidth = 32.dp

    /** Artwork in the show notes header (NowPlayingNotes artboard). */
    val NotesHeaderArtwork = 64.dp

    /** The import's latest-show cover card, at most. */
    val ImportCoverMax = 280.dp

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
