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

    /** space-8: the empty state's side margins (EmptyState artboard). */
    val Space8 = 32.dp
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

    /** The filled pause button inside a playing row's ring (EpisodeRow artboard). */
    val RowPauseButton = 36.dp

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

    /** The show page's Resume / Play button (design hand-off › Show). */
    val DamaskPitchMedium = 12.dp

    /** The empty state's damask tile, its icon circle and icon, and its text width (EmptyState artboard). */
    val DamaskPitchPanel = 16.dp
    val EmptyStateTile = 144.dp
    val EmptyStateIconCircle = 72.dp
    val EmptyStateIcon = 36.dp
    val EmptyStateTextMaxWidth = 300.dp

    /** The position bar's thumb (NowPlayingPane artboard). */
    val ScrubberThumbWidth = 4.dp
    val ScrubberThumbHeight = 20.dp

    /** The show notes sheet's top corners, and its drag handle. */
    val SheetCornerRadius = 28.dp
    val SheetHandleWidth = 32.dp

    /** How far a sheet's handle must be dragged to open or close it. */
    val SheetDragThreshold = 24.dp

    /** The mini-player above the tabs (MiniPlayer artboard): its height, artwork and progress bar. */
    val MiniPlayerHeight = 64.dp
    val MiniPlayerArtwork = 44.dp

    /** The cover in a collapsed page bar (show and genre pages). */
    val BarArtwork = 32.dp
    val MiniPlayerProgressHeight = 3.dp

    /** Artwork in the show notes header (NowPlayingNotes artboard). */
    val NotesHeaderArtwork = 64.dp

    /** The import's latest-show cover card, at most. */
    val ImportCoverMax = 280.dp

    /** Window width bands (Material window size classes): rail from medium, two panes from expanded. */
    val MediumWidth = 600.dp
    val ExpandedWidth = 840.dp

    /** An episode screen this wide shows its notes beside the player. */
    val NotesBesideMinWidth = 720.dp

    /** The list pane on wide layouts (TabletHome, TabletGenres artboards), and a pane's heading. */
    val ListPaneWidth = 440.dp
    val PaneHeaderHeight = 64.dp

    /** A show row in Shows (ShowListPane artboard), and its artwork. */
    val ShowRowMinHeight = 64.dp
    val ShowRowArtwork = 48.dp

    /** The artwork at the top of a show's page. */
    val ShowHeaderArtwork = 112.dp

    /** A genre row in the list (GenreListPane artboard). */
    val GenreRowMinHeight = 80.dp

    /** A genre-queue row's leading stripe in the genre colour (design hand-off › EpisodeRow). */
    val GenreStripeWidth = 4.dp
    val GenreStripeInsetStart = 4.dp
    val GenreStripeInsetVertical = 12.dp

    /** The bar beside a genre's name on its page. */
    val GenreTitleBarHeight = 22.dp

    /** The ring round a selected genre row on tablets. */
    val GenreRingWidth = 2.dp

    /** A colour swatch's dot on Manage categories, and the check circle on a picked swatch. */
    val SwatchDot = 28.dp

    /** A genre's colour beside its name in the genre sheet. */
    val GenreSquare = 16.dp
    val SwatchCheck = 24.dp

    /** No space (a padding that's only there sometimes). */
    val None = 0.dp

    /** The playing episode's width in the right-hand pane (TabletHome artboard). */
    val PlayerPaneMaxWidth = 520.dp

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
