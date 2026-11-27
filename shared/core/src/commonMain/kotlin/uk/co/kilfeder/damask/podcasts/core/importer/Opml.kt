package uk.co.kilfeder.damask.podcasts.core.importer

import uk.co.kilfeder.damask.podcasts.core.feed.FeedParseException
import uk.co.kilfeder.damask.podcasts.core.feed.Xml
import uk.co.kilfeder.damask.podcasts.core.feed.XmlParseException
import uk.co.kilfeder.damask.podcasts.core.sync.FeedUrl

/** A subscription listed in an OPML file. */
data class OpmlEntry(val title: String, val feedUrl: String)

/** OPML 1.0/2.0 subscription lists (Pocket Casts, AntennaPod, Podcast Addict, ...). */
object Opml {
    /** Every `<outline xmlUrl=...>` at any depth, de-duplicated by normalised URL, in file order. */
    fun parse(source: String): List<OpmlEntry> {
        val root = try {
            Xml.parse(source)
        } catch (e: XmlParseException) {
            throw FeedParseException("Not a readable OPML file", e)
        }
        if (root.name != "opml") throw FeedParseException("Not an OPML file")
        return root.descendants()
            .filter { it.name == "outline" }
            .mapNotNull { outline ->
                val url = outline.attr("xmlUrl") ?: return@mapNotNull null
                // Never fall back to the URL for display: private feed URLs are secrets.
                OpmlEntry(title = outline.attr("text") ?: outline.attr("title").orEmpty(), feedUrl = url)
            }
            .distinctBy { FeedUrl.normalise(it.feedUrl) }
            .toList()
    }
}
