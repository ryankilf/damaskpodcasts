package uk.co.kilfeder.damask.podcasts.core.importer

import uk.co.kilfeder.damask.podcasts.core.feed.FeedParseException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OpmlTest {
    @Test
    fun readsPocketCastsStyleExportsAtAnyDepth() {
        val opml = """
            <?xml version="1.0" encoding="utf-8"?>
            <opml version="1.0">
              <head><title>Pocket Casts Feeds</title></head>
              <body>
                <outline text="feeds">
                  <outline type="rss" text="History &amp; Friends" xmlUrl="https://example.com/history" />
                  <outline type="rss" text="Duplicate" xmlUrl="http://EXAMPLE.com/history/" />
                  <outline type="rss" title="Titled" xmlUrl="https://example.com/titled" />
                  <outline type="rss" xmlUrl="https://private.example.com/feed?token=secret" />
                  <outline text="A folder, not a feed" />
                </outline>
              </body>
            </opml>
        """.trimIndent()
        assertEquals(
            listOf(
                OpmlEntry("History & Friends", "https://example.com/history"),
                OpmlEntry("Titled", "https://example.com/titled"),
                OpmlEntry("", "https://private.example.com/feed?token=secret"),
            ),
            Opml.parse(opml),
        )
    }

    @Test
    fun rejectsNonOpml() {
        assertFailsWith<FeedParseException> { Opml.parse("<rss><channel/></rss>") }
    }
}
