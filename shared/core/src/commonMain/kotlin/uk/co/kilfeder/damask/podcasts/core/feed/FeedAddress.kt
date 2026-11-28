package uk.co.kilfeder.damask.podcasts.core.feed

/**
 * Turns what someone pasted into a feed URL, or null if it isn't a link (spec › Premium feeds ›
 * Add by URL). Forgiving about what people paste: surrounding spaces, no scheme
 * ("example.com/feed"), and podcast-app schemes (`feed://`, `podcast://`, `itpc://`, `pcast://`),
 * which mean the same address over the web. Plain `http` stays `http` here; fetching upgrades it.
 * The query string is kept as it is: private feeds' tokens live there.
 */
object FeedAddress {
    private val APP_SCHEMES = setOf("feed", "podcast", "podcasts", "itpc", "pcast")
    private val WEB_SCHEMES = setOf("http", "https")
    private val SCHEME = Regex("^([a-zA-Z][a-zA-Z0-9+.\\-]*):(?://)?(.*)$")
    private val HOST = Regex("^[^/?#\\s]+\\.[^/?#\\s]+")

    fun parse(text: String): String? {
        val trimmed = text.trim()
        val (scheme, rest) = split(trimmed)?.takeUnless { trimmed.any(Char::isWhitespace) } ?: return null
        // feed:https://example.com/rss nests the real address.
        return when {
            rest.contains("://") -> parse(rest)
            HOST.containsMatchIn(rest) -> "$scheme://$rest"
            else -> null
        }
    }

    /** The scheme to fetch with and the rest of the address; null for a scheme that isn't a feed's. */
    private fun split(text: String): Pair<String, String>? {
        val match = SCHEME.matchEntire(text)
        val scheme = match?.groupValues?.get(1)?.lowercase()
        val rest = match?.groupValues?.get(2).orEmpty()
        return when {
            text.isEmpty() -> null
            match == null -> "https" to text
            scheme in WEB_SCHEMES -> scheme!! to rest
            scheme in APP_SCHEMES -> "https" to rest
            // "example.com:8080/feed" has no scheme, just a port.
            rest.firstOrNull()?.isDigit() == true -> "https" to text
            else -> null
        }
    }
}
