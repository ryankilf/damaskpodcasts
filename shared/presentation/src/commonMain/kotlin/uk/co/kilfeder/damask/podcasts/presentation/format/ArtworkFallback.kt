package uk.co.kilfeder.damask.podcasts.presentation.format

/**
 * What to show where a show has no artwork (none in its feed, still loading, or it failed): its
 * initials on a coloured tile (design hand-off › EpisodeRow samples). [seed] picks the colour from
 * the theme's palette, so the same show always gets the same colour on every screen and device.
 */
data class ArtworkFallback(val initials: String, val seed: Int) {
    companion object {
        /**
         * For the show whose permanent ID is [stableId] (its UUID, or its feed URL before it has
         * one): the initials of [title]'s first two significant words, skipping small words like
         * "the" and "of" ("Empires of Dust" is ED, "The Rest Is History" RH, "Half-Life FM" HL).
         */
        fun of(stableId: String, title: String): ArtworkFallback =
            ArtworkFallback(initials(title), stableHash(stableId))

        private fun initials(title: String): String = title
            .split(' ', '\t', '-', ':')
            .filter { word -> word.any { it.isLetterOrDigit() } }
            .let { words -> words.filter { it.lowercase() !in SMALL_WORDS }.ifEmpty { words } }
            .mapNotNull { word -> word.firstOrNull { it.isLetterOrDigit() } }
            .take(MAX_INITIALS)
            .joinToString("")
            .uppercase()

        /** FNV-1a: the same value on every platform and every run, unlike some hash codes. */
        private fun stableHash(text: String): Int {
            var hash = FNV_OFFSET
            text.forEach { char ->
                hash = hash xor char.code
                hash *= FNV_PRIME
            }
            return hash and Int.MAX_VALUE
        }

        private const val MAX_INITIALS = 2
        private val SMALL_WORDS = setOf("the", "a", "an", "of", "and", "is", "in", "on", "to", "for", "with", "at")
        private const val FNV_OFFSET = -0x7ee3623b // 2166136261 as a signed Int
        private const val FNV_PRIME = 16777619
    }
}
