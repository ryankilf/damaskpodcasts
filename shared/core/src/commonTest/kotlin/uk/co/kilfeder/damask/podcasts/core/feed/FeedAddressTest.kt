package uk.co.kilfeder.damask.podcasts.core.feed

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeedAddressTest {
    @Test
    fun webAddressesPassThrough() {
        assertEquals("https://example.com/feed.xml", FeedAddress.parse("  https://example.com/feed.xml \n"))
        assertEquals("http://example.com/rss", FeedAddress.parse("http://example.com/rss"))
        assertEquals("https://example.com/feed", FeedAddress.parse("HTTPS://example.com/feed"))
    }

    @Test
    fun aMissingSchemeMeansHttps() {
        assertEquals("https://example.com/feed", FeedAddress.parse("example.com/feed"))
        assertEquals("https://example.com:8080/feed", FeedAddress.parse("example.com:8080/feed"))
    }

    @Test
    fun podcastAppSchemesMeanTheWeb() {
        assertEquals("https://example.com/rss", FeedAddress.parse("feed://example.com/rss"))
        assertEquals("https://example.com/rss", FeedAddress.parse("podcast://example.com/rss"))
        assertEquals("https://example.com/rss", FeedAddress.parse("itpc://example.com/rss"))
        assertEquals("https://example.com/rss", FeedAddress.parse("pcast://example.com/rss"))
        assertEquals("http://example.com/rss", FeedAddress.parse("feed:http://example.com/rss"))
    }

    @Test
    fun privateTokensAreKept() {
        val url = "https://www.patreon.com/rss/show?auth=AbC123&show=9"
        assertEquals(url, FeedAddress.parse(url))
    }

    @Test
    fun thingsThatArentLinks() {
        assertNull(FeedAddress.parse(""))
        assertNull(FeedAddress.parse("the rest is history"))
        assertNull(FeedAddress.parse("history"))
        assertNull(FeedAddress.parse("mailto:someone@example.com"))
        assertNull(FeedAddress.parse("https://"))
    }
}
