package uk.co.kilfeder.damask.podcasts.core.rules

import uk.co.kilfeder.damask.podcasts.core.HOUR_MS
import uk.co.kilfeder.damask.podcasts.core.NOW_MS
import uk.co.kilfeder.damask.podcasts.core.at
import uk.co.kilfeder.damask.podcasts.core.daysAgo
import uk.co.kilfeder.damask.podcasts.core.episode
import uk.co.kilfeder.damask.podcasts.core.finished
import uk.co.kilfeder.damask.podcasts.core.hoursAgo
import uk.co.kilfeder.damask.podcasts.core.markedPlayed
import uk.co.kilfeder.damask.podcasts.core.minutesAgo
import uk.co.kilfeder.damask.podcasts.core.model.MINUTE_MS
import uk.co.kilfeder.damask.podcasts.core.model.PlayedState
import uk.co.kilfeder.damask.podcasts.core.model.ShowType
import uk.co.kilfeder.damask.podcasts.core.show
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ListeningRulesTest {
    private val rules = ListeningRules()
    private val episodic = show("s", "c")
    private val serial = show("s", "c", ShowType.SERIAL)

    // --- Threshold -------------------------------------------------------------------------

    @Test
    fun thresholdIsThirtyPercentOfKnownDuration() {
        assertEquals(18 * MINUTE_MS, rules.thresholdMs(episode("s", "e", durationMs = HOUR_MS)))
    }

    @Test
    fun playerDurationBeatsFeedDuration() {
        val ep = episode("s", "e", durationMs = HOUR_MS).copy(playerDurationMs = 2 * HOUR_MS)
        assertEquals(36 * MINUTE_MS, rules.thresholdMs(ep))
    }

    @Test
    fun unknownDurationUsesTenMinutes() {
        assertEquals(10 * MINUTE_MS, rules.thresholdMs(episode("s", "e", durationMs = null)))
    }

    // --- counts_as_listened / listened_at ----------------------------------------------------

    @Test
    fun countsOnceCurrentPositionReachesThreshold() {
        val ep = episode("s", "e")
        assertFalse(rules.countsAsListened(ep.at(17 * MINUTE_MS, minutesAgo(1))))
        assertTrue(rules.countsAsListened(ep.at(18 * MINUTE_MS, minutesAgo(1))))
    }

    @Test
    fun scrubbingBackBelowThresholdUndoesIt() {
        val scrubbedBack = episode("s", "e").at(40 * MINUTE_MS, minutesAgo(5)).at(5 * MINUTE_MS, minutesAgo(1))
        assertFalse(rules.countsAsListened(scrubbedBack))
        assertNull(rules.listenedAt(scrubbedBack))
    }

    @Test
    fun finishedCountsButMarkedPlayedDoesNot() {
        assertTrue(rules.countsAsListened(episode("s", "e").finished(minutesAgo(1))))
        assertFalse(rules.countsAsListened(episode("s", "e").markedPlayed(minutesAgo(1))))
    }

    @Test
    fun markedUnplayedClearsIt() {
        val unplayed = episode("s", "e").finished(hoursAgo(2))
            .copy(played = PlayedState(played = false, playedBy = null, hlc = hoursAgo(1)))
            .at(0, hoursAgo(1))
        assertFalse(rules.countsAsListened(unplayed))
    }

    @Test
    fun listenedAtIsTheLaterOfFinishAndPosition() {
        val ep = episode("s", "e").finished(hoursAgo(3)).at(50 * MINUTE_MS, hoursAgo(1))
        assertEquals(hoursAgo(1), rules.listenedAt(ep))
        val finishedLater = episode("s", "e").at(50 * MINUTE_MS, hoursAgo(3)).finished(hoursAgo(1))
        assertEquals(hoursAgo(1), rules.listenedAt(finishedLater))
    }

    // --- In progress ------------------------------------------------------------------------

    @Test
    fun startedAndUnplayedIsInProgress() {
        assertTrue(rules.isInProgress(episode("s", "e").at(1_000, minutesAgo(1)), episodic, NOW_MS))
        assertFalse(rules.isInProgress(episode("s", "e").at(0, minutesAgo(1)), episodic, NOW_MS))
        assertFalse(rules.isInProgress(episode("s", "e"), episodic, NOW_MS))
    }

    @Test
    fun relistenOfPlayedEpisodeIsInProgress() {
        val relisten = episode("s", "e").finished(daysAgo(2)).at(5 * MINUTE_MS, minutesAgo(1))
        assertTrue(rules.isInProgress(relisten, episodic, NOW_MS))
        val playedAfter = episode("s", "e").at(5 * MINUTE_MS, daysAgo(2)).markedPlayed(minutesAgo(1))
        assertFalse(rules.isInProgress(playedAfter, episodic, NOW_MS))
    }

    @Test
    fun episodicExpiresAfterSixMonthsButSerialNever() {
        val old = episode("s", "e").at(5 * MINUTE_MS, daysAgo(200))
        assertFalse(rules.isInProgress(old, episodic, NOW_MS))
        assertTrue(rules.isInProgress(old, serial, NOW_MS))
        assertTrue(rules.isInProgress(episode("s", "e").at(5 * MINUTE_MS, daysAgo(150)), episodic, NOW_MS))
    }

    // --- Finish rule ------------------------------------------------------------------------

    @Test
    fun reachingTheEndAlwaysFinishes() {
        assertTrue(rules.isFinishedOnStop(positionMs = 0, durationMs = null, reachedEnd = true))
    }

    @Test
    fun stoppingWithinTwoMinutesFinishes() {
        assertTrue(rules.isFinishedOnStop(HOUR_MS - 2 * MINUTE_MS, HOUR_MS, reachedEnd = false))
        assertFalse(rules.isFinishedOnStop(HOUR_MS - 2 * MINUTE_MS - 1_000, HOUR_MS, reachedEnd = false))
    }

    @Test
    fun zeroTailMeansOnlyTheTrueEnd() {
        val strict = ListeningRules(ListeningSettings(finishTailMs = 0))
        assertFalse(strict.isFinishedOnStop(HOUR_MS - 1_000, HOUR_MS, reachedEnd = false))
    }

    @Test
    fun unknownDurationOnlyFinishesAtTheEnd() {
        assertFalse(rules.isFinishedOnStop(HOUR_MS, durationMs = null, reachedEnd = false))
    }
}
