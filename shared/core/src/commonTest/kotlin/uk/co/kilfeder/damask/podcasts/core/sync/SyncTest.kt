package uk.co.kilfeder.damask.podcasts.core.sync

import uk.co.kilfeder.damask.podcasts.core.model.Hlc
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HlcTest {
    @Test
    fun numericOrderMatchesWallClockThenCounter() {
        assertTrue(Hlc.of(1_000, 5) < Hlc.of(1_001, 0))
        assertTrue(Hlc.of(1_000, 5) > Hlc.of(1_000, 4))
        val hlc = Hlc.of(1_795_780_800_000, 7)
        assertEquals(1_795_780_800_000, hlc.wallMs)
        assertEquals(7, hlc.counter)
    }

    @Test
    fun issuedTimestampsStrictlyIncreaseEvenWhenTheClockStallsOrGoesBack() {
        var now = 1_000L
        val clock = HlcClock({ now })
        val first = clock.issue()
        val sameMs = clock.issue()
        now = 900 // wall clock stepped backwards
        val afterStep = clock.issue()
        now = 2_000
        val advanced = clock.issue()
        assertEquals(listOf(first, sameMs, afterStep, advanced), listOf(first, sameMs, afterStep, advanced).sorted())
        assertEquals(setOf(first, sameMs, afterStep, advanced).size, 4)
        assertEquals(Hlc.of(1_000, 2), afterStep)
        assertEquals(Hlc.of(2_000), advanced)
    }

    @Test
    fun receivingLaterTimestampsOrdersLocalWritesAfterThem() {
        val clock = HlcClock({ 1_000L })
        clock.receive(Hlc.of(5_000, 3)) // another device's clock is ahead
        assertEquals(Hlc.of(5_000, 4), clock.issue())
        clock.receive(Hlc.of(10)) // older: ignored
        assertEquals(Hlc.of(5_000, 5), clock.issue())
    }

    @Test
    fun counterOverflowBorrowsTheNextMillisecond() {
        val clock = HlcClock({ 1_000L }, initial = Hlc.of(1_000, Hlc.MAX_COUNTER))
        assertEquals(Hlc.of(1_001), clock.issue())
    }
}

class FeedUrlTest {
    @Test
    fun appliesExactlyTheSpecsSteps() {
        assertEquals("https://example.com/feed", FeedUrl.normalise("HTTP://Example.COM:80/feed/"))
        assertEquals("https://example.com/feed", FeedUrl.normalise("https://example.com:443/feed"))
        assertEquals("https://example.com:8080/feed", FeedUrl.normalise("http://example.com:8080/feed"))
        assertEquals("https://example.com", FeedUrl.normalise("https://example.com/"))
    }

    @Test
    fun neverTouchesTheQueryOrPathCase() {
        assertEquals(
            "https://feeds.example.com/Show/RSS?token=AbC123&x=Y",
            FeedUrl.normalise("http://FEEDS.example.com/Show/RSS/?token=AbC123&x=Y"),
        )
    }

    @Test
    fun keepsUserInfoAndHandlesIpv6() {
        assertEquals("https://User:Pass@host.com/f", FeedUrl.normalise("https://User:Pass@HOST.com/f"))
        assertEquals("https://[::1]:8443/f", FeedUrl.normalise("https://[::1]:8443/f"))
        assertEquals("https://[::1]/f", FeedUrl.normalise("https://[::1]/f"))
    }

    @Test
    fun returnsUnparseableInputTrimmed() {
        assertEquals("not a url", FeedUrl.normalise("  not a url "))
    }
}

class ShowAliasesTest {
    private fun identity(id: String, url: String, ms: Long, subscribed: Boolean = true) =
        ShowIdentity(ShowId(id), FeedUrl.normalise(url), Hlc.of(ms), subscribed)

    @Test
    fun duplicatesAliasToTheEarliestShow() {
        val aliases = ShowAliases.detect(
            listOf(
                identity("new-url", "https://new.example.com/feed", 2_000),
                identity("old-url", "http://NEW.example.com/feed/", 1_000),
                identity("third", "https://new.example.com:443/feed", 3_000),
                identity("other", "https://other.example.com/feed", 500),
            ),
        )
        assertEquals(mapOf(ShowId("new-url") to ShowId("old-url"), ShowId("third") to ShowId("old-url")), aliases)
    }

    @Test
    fun tiesBreakByIdAndUnsubscribedShowsAreIgnored() {
        val aliases = ShowAliases.detect(
            listOf(
                identity("b", "https://x.com/f", 1_000),
                identity("a", "https://x.com/f", 1_000),
                identity("gone", "https://x.com/f", 1, subscribed = false),
            ),
        )
        assertEquals(mapOf(ShowId("b") to ShowId("a")), aliases)
    }

    @Test
    fun flattenPointsEveryAliasStraightAtItsRoot() {
        val chain = mapOf(ShowId("a") to ShowId("b"), ShowId("b") to ShowId("c"))
        val flat = ShowAliases.flatten(chain)
        assertEquals(mapOf(ShowId("a") to ShowId("c"), ShowId("b") to ShowId("c")), flat)
        assertEquals(ShowId("c"), ShowAliases.canonical(ShowId("a"), flat))
        assertEquals(ShowId("z"), ShowAliases.canonical(ShowId("z"), flat))
    }

    @Test
    fun cyclesResolveDeterministically() {
        val cycle = mapOf(ShowId("b") to ShowId("c"), ShowId("c") to ShowId("b"))
        assertEquals(mapOf(ShowId("c") to ShowId("b")), ShowAliases.flatten(cycle))
    }
}
