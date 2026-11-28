package uk.co.kilfeder.damask.podcasts.core.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** Sample feeds shared by the parser and import tests. */
object SampleFeeds {
    /** Deliberately messy: CDATA, HTML entities, nested categories, a duplicate and a missing guid. */
    val MESSY = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0" xmlns:itunes="http://www.itunes.com/DTDs/Podcast-1.0.dtd">
          <channel>
            <title><![CDATA[History & Friends]]></title>
            <language>en-gb</language>
            <itunes:author>Friends &amp; Co</itunes:author>
            <description>Old things, told well.</description>
            <itunes:type>serial</itunes:type>
            <itunes:new-feed-url>https://new.example.com/feed</itunes:new-feed-url>
            <image><url>https://example.com/rss-art.jpg</url></image>
            <itunes:image href="https://example.com/art.jpg"/>
            <itunes:category text="Society &amp; Culture">
              <itunes:category text="History"/>
            </itunes:category>
            <itunes:category text="Education"/>
            <item>
              <title>Part 1&nbsp;— The Start</title>
              <guid isPermaLink="false"> ep-1 </guid>
              <pubDate>Wed, 02 Oct 2002 13:00:00 GMT</pubDate>
              <enclosure url="https://cdn.example.com/1.mp3" length="1" type="audio/mpeg"/>
              <itunes:duration>1:00:00</itunes:duration>
              <itunes:image href="https://example.com/ep1.jpg"/>
            </item>
            <item>
              <title>Duplicate of part 1</title>
              <guid>ep-1</guid>
              <enclosure url="https://cdn.example.com/1-dupe.mp3"/>
            </item>
            <item>
              <title>Don&#8217;t &#x2014; no guid</title>
              <enclosure url="https://cdn.example.com/2.mp3"/>
            </item>
            <item>
              <title>Trailer page with no audio</title>
              <guid>no-audio</guid>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    /** An episodic feed with [count] episodes, newest first, one a day. */
    fun episodic(title: String, category: String, count: Int): String = buildString {
        append("""<rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel>""")
        append("<title>$title</title><itunes:category text=\"$category\"/>")
        for (n in 1..count) {
            val day = (count - n + 1).toString().padStart(2, '0')
            append("<item><title>$title $n</title><guid>$title-$n</guid>")
            append("<pubDate>2026-11-${day}T08:00:00Z</pubDate>")
            append("<enclosure url=\"https://cdn.example.com/$title/$n.mp3\"/></item>")
        }
        append("</channel></rss>")
    }
}

class FeedParserTest {
    private val feed = FeedParser.parse(SampleFeeds.MESSY)

    @Test
    fun readsChannelData() {
        assertEquals("History & Friends", feed.title)
        assertEquals("https://example.com/art.jpg", feed.imageUrl, "itunes:image beats <image><url>")
        assertEquals("serial", feed.itunesType)
        assertEquals("en-gb", feed.language)
        assertEquals("Friends & Co", feed.author)
        assertEquals("Old things, told well.", feed.description, "the channel's own, not an item's")
        assertEquals("https://new.example.com/feed", feed.newFeedUrl)
        assertEquals(listOf(FeedCategory("Society & Culture", "History"), FeedCategory("Education")), feed.categories)
    }

    @Test
    fun readsEpisodesSkippingDuplicatesAndItemsWithoutAudio() {
        assertEquals(listOf("ep-1", "https://cdn.example.com/2.mp3"), feed.episodes.map { it.guid })
        val first = feed.episodes.first()
        assertEquals("Part 1 — The Start", first.title)
        assertEquals(1_033_563_600_000, first.publishedAt)
        assertEquals(3_600_000, first.durationMs)
        assertEquals("https://example.com/ep1.jpg", first.imageUrl)
        assertNull(feed.episodes[1].publishedAt)
        assertEquals("Don\u2019t \u2014 no guid", feed.episodes[1].title, "numeric character references")
    }

    @Test
    fun readsShowNotesPreferringContentEncoded() {
        val item = { inner: String ->
            """<rss xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"
                xmlns:content="http://purl.org/rss/1.0/modules/content/"><channel><title>T</title>
                <item><guid>g</guid><enclosure url="https://e.com/a.mp3"/>$inner</item>
                </channel></rss>"""
        }
        val notes = { inner: String -> FeedParser.parse(item(inner)).episodes.single().notesHtml }
        assertEquals(
            "<p>Rich</p>",
            notes("<description>Plain</description><content:encoded><![CDATA[<p>Rich</p>]]></content:encoded>"),
        )
        assertEquals("<b>Escaped</b> & more", notes("<description>&lt;b&gt;Escaped&lt;/b&gt; &amp; more</description>"))
        assertEquals("Summary", notes("<itunes:summary>Summary</itunes:summary>"))
        assertNull(notes(""))
        val huge = "x".repeat(FeedParser.MAX_NOTES_CHARS + 10)
        assertEquals(FeedParser.MAX_NOTES_CHARS, notes("<description>$huge</description>")?.length)
    }

    @Test
    fun fallsBackToRssImage() {
        val noItunesImage = SampleFeeds.MESSY.replace("""<itunes:image href="https://example.com/art.jpg"/>""", "")
        assertEquals("https://example.com/rss-art.jpg", FeedParser.parse(noItunesImage).imageUrl)
    }

    @Test
    fun rejectsThingsThatArentFeeds() {
        assertFailsWith<FeedParseException> { FeedParser.parse("<html><body>Not found</body></html>") }
        assertFailsWith<FeedParseException> { FeedParser.parse("not xml at all") }
    }
}
