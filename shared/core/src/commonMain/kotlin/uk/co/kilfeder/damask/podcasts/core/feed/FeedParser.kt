package uk.co.kilfeder.damask.podcasts.core.feed

class FeedParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Shown to users when a show's address doesn't lead to a podcast. */
private const val NOT_A_FEED = "This isn’t a podcast feed"

/** RSS 2.0 + iTunes podcast tags. Lenient: real-world feeds are messy. */
object FeedParser {

    fun parse(xml: String): FeedDocument {
        val root = try {
            Xml.parse(xml)
        } catch (e: XmlParseException) {
            throw FeedParseException(NOT_A_FEED, e)
        }
        val channel = root.child("channel") ?: throw FeedParseException(NOT_A_FEED)
        return FeedDocument(
            title = channel.childText("title").orEmpty(),
            imageUrl = channel.child("image", Namespace.ITUNES)?.attr("href")
                ?: channel.child("image")?.childText("url"),
            itunesType = channel.childText("type", Namespace.ITUNES),
            categories = channel.children("category", Namespace.ITUNES).mapNotNull { it.toCategory() },
            language = channel.childText("language"),
            newFeedUrl = channel.childText("new-feed-url", Namespace.ITUNES),
            episodes = channel.children("item").mapNotNull { it.toEpisode() }.distinctBy { it.guid },
        )
    }

    private fun XmlNode.toCategory(): FeedCategory? {
        val name = attr("text") ?: return null
        val sub = child("category", Namespace.ITUNES)?.attr("text").orEmpty()
        return FeedCategory(name, sub)
    }

    /** Items without an enclosure aren't episodes (no audio) and are skipped. */
    private fun XmlNode.toEpisode(): FeedEpisode? {
        val enclosure = child("enclosure")?.attr("url") ?: return null
        val title = childText("title").orEmpty()
        val published = childText("pubDate")?.let(FeedDates::parse)
        // guid is optional in RSS; fall back to the enclosure URL, which is stable for most hosts.
        val guid = childText("guid") ?: enclosure
        return FeedEpisode(
            guid = guid,
            title = title,
            publishedAt = published,
            enclosureUrl = enclosure,
            imageUrl = child("image", Namespace.ITUNES)?.attr("href"),
            durationMs = childText("duration", Namespace.ITUNES)?.let(ItunesDuration::parseMs),
        )
    }
}
