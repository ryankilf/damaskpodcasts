package uk.co.kilfeder.damask.podcasts.core.sync

/**
 * `feed_url_norm`: the form used to spot two show IDs subscribed to the same feed
 * (spec › Sync › Duplicate shows). Applies exactly these steps:
 *
 * - scheme and host lowercased;
 * - default ports removed;
 * - `http` and `https` treated as equal (both normalise to `https`);
 * - a trailing slash on the path removed.
 *
 * The query string is **never** touched: premium feed tokens live in it. Unparseable input is
 * returned trimmed but otherwise unchanged.
 */
object FeedUrl {
    private val URL = Regex("""^([a-zA-Z][a-zA-Z0-9+.\-]*)://([^/?#]*)([^?#]*)(.*)$""")
    private val DEFAULT_PORTS = setOf("80", "443")
    private const val SCHEME = 1
    private const val AUTHORITY = 2
    private const val PATH = 3
    private const val REST = 4

    fun normalise(url: String): String {
        val trimmed = url.trim()
        val match = URL.matchEntire(trimmed) ?: return trimmed
        val groups = match.groupValues
        val authority = groups[AUTHORITY]
        val path = groups[PATH]
        val rest = groups[REST] // query and fragment, untouched
        val scheme = groups[SCHEME].lowercase().let { if (it == "http") "https" else it }
        return "$scheme://${normaliseAuthority(authority)}${path.removeSuffix("/")}$rest"
    }

    /** `[userinfo@]host[:port]`. User info is kept as written (it may be a credential). */
    private fun normaliseAuthority(authority: String): String {
        val at = authority.lastIndexOf('@')
        val userInfo = if (at >= 0) authority.substring(0, at + 1) else ""
        val hostPort = authority.substring(at + 1)
        // A port follows the last ':' unless that colon is inside an IPv6 literal ([::1]).
        val colon = hostPort.lastIndexOf(':')
        val hasPort = colon > hostPort.lastIndexOf(']')
        val host = if (hasPort) hostPort.substring(0, colon) else hostPort
        val port = if (hasPort) hostPort.substring(colon + 1) else ""
        val keptPort = if (port.isEmpty() || port in DEFAULT_PORTS) "" else ":$port"
        return userInfo + host.lowercase() + keptPort
    }
}
