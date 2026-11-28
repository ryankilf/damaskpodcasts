package uk.co.kilfeder.damask.podcasts.presentation.format

import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue

/**
 * What to show where a show has no artwork (none in its feed, still loading, or it failed): its
 * initials on its genre's colour (the user's call: placeholders match the genre colours), so a
 * show's tile matches the chips and stripes around it.
 */
data class ArtworkFallback(val initials: String, val hue: GenreHue) {
    companion object {
        /**
         * The initials of [title]'s first two significant words, skipping small words like
         * "the" and "of" ("Empires of Dust" is ED, "The Rest Is History" RH, "Half-Life FM" HL),
         * on [hue].
         */
        fun of(title: String, hue: GenreHue): ArtworkFallback = ArtworkFallback(initials(title), hue)

        private fun initials(title: String): String = title
            .split(' ', '\t', '-', ':')
            .filter { word -> word.any { it.isLetterOrDigit() } }
            .let { words -> words.filter { it.lowercase() !in SMALL_WORDS }.ifEmpty { words } }
            .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
            .take(MAX_INITIALS)
            .joinToString("")
            .uppercase()

        private const val MAX_INITIALS = 2
        private val SMALL_WORDS = setOf("the", "a", "an", "of", "and", "is", "in", "on", "to", "for", "with", "at")
    }
}
