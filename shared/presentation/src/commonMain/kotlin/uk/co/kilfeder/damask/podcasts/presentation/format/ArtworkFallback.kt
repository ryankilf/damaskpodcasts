package uk.co.kilfeder.damask.podcasts.presentation.format

import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue

/**
 * What to show where artwork is missing (none in the feed, still loading, or it failed): its
 * show's initials on its genre's colour (the user's call: placeholders match the genre colours),
 * so a tile matches the chips and stripes around it. For an episode, [imageUrl] is its show's
 * cover, shown over the initials while the episode's own art loads (or if it fails).
 */
data class ArtworkFallback(val initials: String, val hue: GenreHue, val imageUrl: String? = null) {
    companion object {
        /**
         * The initials of [title]'s first two significant words, skipping small words like
         * "the" and "of" ("Empires of Dust" is ED, "The Rest Is History" RH, "Half-Life FM" HL),
         * on [hue], with [imageUrl] (the show's cover) over them for an episode's art.
         */
        fun of(title: String, hue: GenreHue, imageUrl: String? = null): ArtworkFallback =
            ArtworkFallback(initials(title), hue, imageUrl)

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
