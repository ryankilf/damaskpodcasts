package uk.co.kilfeder.damask.podcasts.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Tiles for shows without artwork (design hand-off › EpisodeRow and ShowListPane samples): muted,
 * earthy colours that sit with the Damask palette in light and dark, each dark enough for white
 * initials (ColorContrastTest). A show's colour is picked from its permanent ID.
 */
val ArtworkFallbackColors: List<Color> = listOf(
    Color(0xFF6B3F2A),
    Color(0xFF7A5A26),
    Color(0xFF3F6B4A),
    Color(0xFF6B5A3F),
    Color(0xFF3A5C4F),
    Color(0xFF6B2F3A),
    Color(0xFF55586B),
    Color(0xFF2F5A6B),
    Color(0xFF7A4B6E),
    Color(0xFF2D5D7B),
)

/** Initials on those tiles. */
val ArtworkFallbackText: Color = Color.White
