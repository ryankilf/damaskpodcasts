package uk.co.kilfeder.damask.podcasts.presentation.format

import kotlinx.datetime.TimeZone
import uk.co.kilfeder.damask.podcasts.presentation.genres.CategoryOptionUi
import uk.co.kilfeder.damask.podcasts.presentation.genres.matching
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Destination
import uk.co.kilfeder.damask.podcasts.presentation.navigation.NavState
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Tab
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FormatsTest {
    private val utc = TimeZone.UTC
    private val now = 1_795_780_800_000L // Fri 27 Nov 2026, 12:00 UTC
    private val day = 86_400_000L

    @Test
    fun relativeDatesFollowTheCalendar() {
        assertEquals("Today", Formats.relativeDate(now - 3_600_000, now, utc))
        assertEquals("Yesterday", Formats.relativeDate(now - day, now, utc))
        assertEquals("3 days ago", Formats.relativeDate(now - 3 * day, now, utc))
        assertEquals("1 week ago", Formats.relativeDate(now - 8 * day, now, utc))
        assertEquals("2 months ago", Formats.relativeDate(now - 65 * day, now, utc))
        assertEquals("2 years ago", Formats.relativeDate(now - 800 * day, now, utc))
    }

    @Test
    fun exactValuesMatchTheDesignSystem() {
        assertEquals("Fri 27 Nov 2026, 12:00", Formats.exactDateTime(now, utc))
        // The design hand-off writes this as "Thu 6 Nov"; 6 Nov 2026 is a Friday.
        assertEquals("Fri 6 Nov 2026, 21:14", Formats.exactDateTime(1_793_999_640_000L, utc))
    }

    @Test
    fun lengthsAndTimeLeft() {
        assertEquals(Summarised("54 min", "54:10"), Formats.length(3_250_000, null))
        assertEquals(Summarised("1 h 12 min", "1:12:00"), Formats.length(4_320_000, 0))
        assertEquals(
            Summarised("32 min left", "32:04 left of 54:10 (40%)"),
            Formats.length(3_250_000, 3_250_000 - 1_924_000),
        )
        assertNull(Formats.length(null, 1_000))
    }

    @Test
    fun categorySearchIgnoresCaseAndAmpersands() {
        val options = listOf(
            CategoryOptionUi("a", "Health & Fitness"),
            CategoryOptionUi("b", "History"),
            CategoryOptionUi("c", "TV & Film"),
        )
        assertEquals(listOf("Health & Fitness"), options.matching("health and").map { it.name })
        assertEquals(listOf("Health & Fitness", "History"), options.matching(" H ").map { it.name })
        assertEquals(listOf("TV & Film"), options.matching("tv & f").map { it.name })
        assertEquals(options, options.matching(""))
        assertEquals(emptyList(), options.matching("zzz"))
    }

    @Test
    fun navigationPushesPopsAndClearsOnTabChange() {
        val queue = NavState().push(Destination.GenreQueue("news"))
        assertEquals(Destination.GenreQueue("news"), queue.top)
        assertEquals(NavState(), queue.pop())
        assertNull(NavState().pop())
        assertEquals(NavState(tab = Tab.PLAYED), queue.select(Tab.PLAYED))
    }

    @Test
    fun lastPlayedReadsMidSentence() {
        assertEquals("yesterday", Formats.lastPlayed(now - day, now, utc).summary)
        assertEquals("3 days ago", Formats.lastPlayed(now - 3 * day, now, utc).summary)
    }
}
