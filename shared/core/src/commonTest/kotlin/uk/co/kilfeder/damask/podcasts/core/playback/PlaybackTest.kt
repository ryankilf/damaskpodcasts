package uk.co.kilfeder.damask.podcasts.core.playback

import uk.co.kilfeder.damask.podcasts.core.NOW_MS
import uk.co.kilfeder.damask.podcasts.core.category
import uk.co.kilfeder.damask.podcasts.core.daysAgo
import uk.co.kilfeder.damask.podcasts.core.episode
import uk.co.kilfeder.damask.podcasts.core.finished
import uk.co.kilfeder.damask.podcasts.core.minutesAgo
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.Hlc
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext
import uk.co.kilfeder.damask.podcasts.core.model.PositionState
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.model.ShowType
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.core.rules.ListeningRules
import uk.co.kilfeder.damask.podcasts.core.show
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AutoplayTest {
    private val categories = listOf(category("History"), category("Tech"))
    private val shows = listOf(show("hist", "History"), show("tech", "Tech"), show("saga", "History", ShowType.SERIAL))

    private fun rotationAfter(vararg episodes: Episode) =
        Rotation(Library(categories, shows, episodes.toList()), ListeningRules(), NOW_MS)

    private val justFinished = episode("hist", "h1", publishedDaysAgo = 1).finished(minutesAgo(0))
    private val finishedKey = justFinished.key

    @Test
    fun upNextRecomputesSoTheCategoryJustHeardMovesBack() {
        val rotation = rotationAfter(
            justFinished,
            episode("hist", "h2", publishedDaysAgo = 2),
            episode("tech", "t1").finished(daysAgo(3)),
            episode("tech", "t2"),
        )
        assertEquals("t2", Autoplay.next(rotation, PlayContext.UpNext, finishedKey)?.key?.guid)
    }

    @Test
    fun genreContextPlaysNextInThatQueue() {
        val rotation = rotationAfter(justFinished, episode("hist", "h2", publishedDaysAgo = 2), episode("tech", "t1"))
        val next = Autoplay.next(rotation, PlayContext.Genre(CategoryId("History")), finishedKey)
        assertEquals("h2", next?.key?.guid)
    }

    @Test
    fun showContextFollowsTheShowsOwnOrder() {
        val ch1 = episode("saga", "ch1", publishedDaysAgo = 30).finished(minutesAgo(0))
        val rotation = rotationAfter(
            ch1,
            episode("saga", "ch2", publishedDaysAgo = 20),
            episode("saga", "ch3", publishedDaysAgo = 10),
        )
        val next = Autoplay.next(rotation, PlayContext.Show(ShowId("saga")), ch1.key)
        assertEquals("ch2", next?.key?.guid)
    }

    @Test
    fun noneContextAndSwitchesStopPlayback() {
        val rotation = rotationAfter(justFinished, episode("hist", "h2"))
        assertNull(Autoplay.next(rotation, PlayContext.None, finishedKey))
        assertNull(Autoplay.next(rotation, PlayContext.UpNext, finishedKey, AutoplaySwitches(enabled = false)))
        assertNull(Autoplay.next(rotation, PlayContext.UpNext, finishedKey, AutoplaySwitches(stopAfterThis = true)))
    }

    @Test
    fun playContextRoundTripsThroughItsColumnEncoding() {
        val contexts = listOf(
            PlayContext.UpNext,
            PlayContext.None,
            PlayContext.Genre(CategoryId("c-1")),
            PlayContext.Show(ShowId("s-1")),
        )
        contexts.forEach { assertEquals(it, PlayContext.decode(it.encode())) }
        assertEquals(PlayContext.None, PlayContext.decode("garbage"))
        assertEquals(PlayContext.None, PlayContext.decode(null))
    }
}

class ResumePositionTest {
    private fun saved(positionMs: Long, savedOnMs: Long?) = PositionState(positionMs, Hlc.ZERO, savedOnMs)

    @Test
    fun matchingCopiesResumeExactly() {
        assertEquals(600_000, ResumePosition.resumeAtMs(saved(600_000, 3_600_000), 3_604_000))
    }

    @Test
    fun mismatchedCopiesRewindThirtySeconds() {
        assertEquals(570_000, ResumePosition.resumeAtMs(saved(600_000, 3_600_000), 3_660_000))
        assertEquals(0, ResumePosition.resumeAtMs(saved(10_000, 3_600_000), 3_660_000), "never below zero")
    }

    @Test
    fun unknownLengthsResumeExactly() {
        assertEquals(600_000, ResumePosition.resumeAtMs(saved(600_000, null), 3_660_000))
        assertEquals(600_000, ResumePosition.resumeAtMs(saved(600_000, 3_600_000), null))
    }
}
