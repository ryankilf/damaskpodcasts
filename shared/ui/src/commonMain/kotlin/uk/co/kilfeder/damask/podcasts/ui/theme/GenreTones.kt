package uk.co.kilfeder.damask.podcasts.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue

/**
 * One genre colour's tones (design hand-off › GenrePalette, `tokens.json`): [stripe] for the
 * queue stripe, title bar, swatches and selected ring; [container] for chips, row tints and
 * artwork placeholders; [onContainer] for text on it (AA in both themes, ColorContrastTest).
 */
data class GenreTones(val stripe: Color, val container: Color, val onContainer: Color)

/** The tones of [hue] for the current theme, light or dark. */
@Composable
fun genreTones(hue: GenreHue): GenreTones {
    // Fixed colours: they follow light and dark, not Material You.
    return (if (isDarkTheme()) GenreTonesDark else GenreTonesLight).getValue(hue)
}

/** Light theme: stripe, container (tone 90), on-container (tone 10). Brown is 85/25. */
val GenreTonesLight: Map<GenreHue, GenreTones> = mapOf(
    GenreHue.RED to GenreTones(Color(0xFFC85045), Color(0xFFFFDAD6), Color(0xFF410002)),
    GenreHue.ORANGE to GenreTones(Color(0xFFFF9651), Color(0xFFFFDBC8), Color(0xFF321300)),
    GenreHue.AMBER to GenreTones(Color(0xFFA67200), Color(0xFFFFDEAE), Color(0xFF281900)),
    GenreHue.YELLOW to GenreTones(Color(0xFFB49A17), Color(0xFFFFE261), Color(0xFF221B00)),
    GenreHue.LIME to GenreTones(Color(0xFF566500), Color(0xFFD9EC80), Color(0xFF181E00)),
    GenreHue.GREEN to GenreTones(Color(0xFF7DB375), Color(0xFFB8F1AD), Color(0xFF002203)),
    GenreHue.TEAL to GenreTones(Color(0xFF006054), Color(0xFF9FF2E0), Color(0xFF00201B)),
    GenreHue.CYAN to GenreTones(Color(0xFF61ACB8), Color(0xFFA4EEFC), Color(0xFF001F24)),
    GenreHue.BLUE to GenreTones(Color(0xFF3B74B7), Color(0xFFD4E3FF), Color(0xFF001C39)),
    GenreHue.INDIGO to GenreTones(Color(0xFF8F95DF), Color(0xFFE0E0FF), Color(0xFF0A1057)),
    GenreHue.VIOLET to GenreTones(Color(0xFF4D388B), Color(0xFFE8DDFF), Color(0xFF21005E)),
    GenreHue.PURPLE to GenreTones(Color(0xFF794C91), Color(0xFFF6D9FF), Color(0xFF300148)),
    GenreHue.MAGENTA to GenreTones(Color(0xFF732969), Color(0xFFFFD7F2), Color(0xFF390034)),
    GenreHue.PINK to GenreTones(Color(0xFF9B506D), Color(0xFFFFD9E3), Color(0xFF3C031F)),
    GenreHue.BROWN to GenreTones(Color(0xFF6B4836), Color(0xFFFCCAB2), Color(0xFF2F1507)),
    GenreHue.SLATE to GenreTones(Color(0xFF74838F), Color(0xFFD5E4F2), Color(0xFF0F1D27)),
    GenreHue.GREY to GenreTones(Color(0xFF7E7576), Color(0xFFEBE0E1), Color(0xFF201A1B)),
)

/** Dark theme: stripe, container (tone 30), on-container (tone 90). */
val GenreTonesDark: Map<GenreHue, GenreTones> = mapOf(
    GenreHue.RED to GenreTones(Color(0xFFDC5E53), Color(0xFF87201B), Color(0xFFFFDAD6)),
    GenreHue.ORANGE to GenreTones(Color(0xFFFF9651), Color(0xFF753400), Color(0xFFFFDBC8)),
    GenreHue.AMBER to GenreTones(Color(0xFFC48C23), Color(0xFF604100), Color(0xFFFFDEAE)),
    GenreHue.YELLOW to GenreTones(Color(0xFFEED14E), Color(0xFF534600), Color(0xFFFFE261)),
    GenreHue.LIME to GenreTones(Color(0xFFC9DB71), Color(0xFF404C00), Color(0xFFD9EC80)),
    GenreHue.GREEN to GenreTones(Color(0xFF97CF8E), Color(0xFF1E511E), Color(0xFFB8F1AD)),
    GenreHue.TEAL to GenreTones(Color(0xFF68BAA9), Color(0xFF005046), Color(0xFF9FF2E0)),
    GenreHue.CYAN to GenreTones(Color(0xFF45919D), Color(0xFF004F58), Color(0xFFA4EEFC)),
    GenreHue.BLUE to GenreTones(Color(0xFF689DE3), Color(0xFF004883), Color(0xFFD4E3FF)),
    GenreHue.INDIGO to GenreTones(Color(0xFFB0B6FF), Color(0xFF3A4084), Color(0xFFE0E0FF)),
    GenreHue.VIOLET to GenreTones(Color(0xFFDED0FF), Color(0xFF4D388B), Color(0xFFE8DDFF)),
    GenreHue.PURPLE to GenreTones(Color(0xFFA473BC), Color(0xFF603477), Color(0xFFF6D9FF)),
    GenreHue.MAGENTA to GenreTones(Color(0xFFDA83C8), Color(0xFF732969), Color(0xFFFFD7F2)),
    GenreHue.PINK to GenreTones(Color(0xFFFFB0CB), Color(0xFF73304B), Color(0xFFFFD9E3)),
    GenreHue.BROWN to GenreTones(Color(0xFFB38772), Color(0xFF543423), Color(0xFFFFDBCB)),
    GenreHue.SLATE to GenreTones(Color(0xFFD5E4F2), Color(0xFF3A4853), Color(0xFFD5E4F2)),
    GenreHue.GREY to GenreTones(Color(0xFFB3A9AA), Color(0xFF4C4546), Color(0xFFEBE0E1)),
)
