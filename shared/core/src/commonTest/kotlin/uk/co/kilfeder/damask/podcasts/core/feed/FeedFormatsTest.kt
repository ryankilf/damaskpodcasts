package uk.co.kilfeder.damask.podcasts.core.feed

import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.sync.ShowIds
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FeedDatesTest {
    private val expected = 1_033_563_600_000L // 2002-10-02T13:00:00Z

    @Test
    fun readsRfc822AndCommonVariants() {
        assertEquals(expected, FeedDates.parse("Wed, 02 Oct 2002 13:00:00 GMT"))
        assertEquals(expected, FeedDates.parse("Wed, 02 Oct 2002 15:00:00 +0200"))
        assertEquals(expected, FeedDates.parse("Wed, 02 Oct 2002 08:00:00 EST"))
        assertEquals(expected, FeedDates.parse("2 Oct 2002 13:00 GMT"), "no day name, single-digit day, no seconds")
        assertEquals(expected, FeedDates.parse("Wednesday, 02 October 02 13:00:00 Z"), "long names, two-digit year")
        assertEquals(expected, FeedDates.parse("Wed, 02 Oct 2002 13:00:00"), "missing zone read as UTC")
    }

    @Test
    fun readsIso8601() {
        assertEquals(expected, FeedDates.parse("2002-10-02T13:00:00Z"))
        assertEquals(expected, FeedDates.parse("2002-10-02T14:00:00.000+01:00"))
        assertEquals(1_033_516_800_000, FeedDates.parse("2002-10-02"))
    }

    @Test
    fun rejectsNonsense() {
        assertNull(FeedDates.parse("yesterday"))
        assertNull(FeedDates.parse("Wed, 32 Oct 2002 13:00:00 GMT"))
        assertNull(FeedDates.parse("Wed, 02 Foo 2002 13:00:00 GMT"))
    }
}

class ItunesDurationTest {
    @Test
    fun readsEveryCommonForm() {
        assertEquals(3_723_000, ItunesDuration.parseMs("1:02:03"))
        assertEquals(3_723_000, ItunesDuration.parseMs("62:03"))
        assertEquals(3_723_000, ItunesDuration.parseMs("3723"))
        assertEquals(3_723_500, ItunesDuration.parseMs("3723.5"))
        assertNull(ItunesDuration.parseMs(""))
        assertNull(ItunesDuration.parseMs("about an hour"))
        assertNull(ItunesDuration.parseMs("0"))
    }
}

class ShowIdsTest {
    @Test
    fun uuid5MatchesTheReferenceVector() {
        // Python: uuid.uuid5(uuid.NAMESPACE_URL, "http://python.org/")
        assertEquals("4c565f0d-3f5a-5890-b41b-20cf47701c5e", ShowIds.uuid5("http://python.org/"))
    }

    @Test
    fun equivalentSpellingsOfAFeedGetTheSameId() {
        assertEquals(ShowIds.fromFeedUrl("http://Example.com/feed/"), ShowIds.fromFeedUrl("https://example.com/feed"))
        val a = ShowIds.fromFeedUrl("https://example.com/feed?token=a")
        val b = ShowIds.fromFeedUrl("https://example.com/feed?token=b")
        kotlin.test.assertNotEquals(a, b, "private feeds with different tokens stay distinct")
    }
}

class AppleCategoriesTest {
    @Test
    fun normalisesCurrentLegacyAndMisspelledNames() {
        assertEquals("Health & Fitness", AppleCategories.normalise("health and fitness"))
        assertEquals("Leisure", AppleCategories.normalise("Games & Hobbies"))
        assertEquals("News", AppleCategories.normalise("News & Politics"))
        assertEquals("TV & Film", AppleCategories.normalise("TV &amp; Film".replace("&amp;", "&")))
        assertNull(AppleCategories.normalise("Podcasts"))
    }

    @Test
    fun seededIdsAreStable() {
        assertEquals(CategoryId("apple:healthandfitness"), AppleCategories.categoryId("Health & Fitness"))
    }
}
