package uk.co.kilfeder.damask.podcasts.core.retention

import uk.co.kilfeder.damask.podcasts.core.NOW_MS
import uk.co.kilfeder.damask.podcasts.core.at
import uk.co.kilfeder.damask.podcasts.core.daysAgo
import uk.co.kilfeder.damask.podcasts.core.episode
import uk.co.kilfeder.damask.podcasts.core.model.KeepMode
import uk.co.kilfeder.damask.podcasts.core.model.ShowType
import uk.co.kilfeder.damask.podcasts.core.rules.ListeningRules
import uk.co.kilfeder.damask.podcasts.core.model.MINUTE_MS
import uk.co.kilfeder.damask.podcasts.core.show
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RetentionTest {
    private val retention = Retention(ListeningRules())

    /** Episodes e1 (newest) .. e[count] (oldest). */
    private fun feed(count: Int) = (1..count).map { episode("s", "e$it", publishedDaysAgo = it.toLong()) }

    @Test
    fun episodicKeepsTheLatestTen() {
        val pruned = retention.episodesToPrune(show("s", "c"), feed(12), NOW_MS)
        assertEquals(listOf("e11", "e12"), pruned.map { it.guid })
    }

    @Test
    fun serialKeepsEverything() {
        assertTrue(retention.episodesToPrune(show("s", "c", ShowType.SERIAL), feed(50), NOW_MS).isEmpty())
    }

    @Test
    fun overridesWin() {
        val keepAll = show("s", "c").copy(keepModeOverride = KeepMode.ALL)
        assertTrue(retention.episodesToPrune(keepAll, feed(12), NOW_MS).isEmpty())
        val keepThree = show("s", "c").copy(keepCount = 3)
        assertEquals(9, retention.episodesToPrune(keepThree, feed(12), NOW_MS).size)
    }

    @Test
    fun pinnedEpisodesAreNeverPruned() {
        val episodes = feed(10) + listOf(
            episode("s", "downloaded", publishedDaysAgo = 50).copy(isDownloaded = true),
            episode("s", "in-progress", publishedDaysAgo = 50).at(MINUTE_MS, daysAgo(1)),
            episode("s", "expired", publishedDaysAgo = 300).at(MINUTE_MS, daysAgo(200)),
        )
        val pruned = retention.episodesToPrune(show("s", "c"), episodes, NOW_MS)
        assertEquals(listOf("expired"), pruned.map { it.guid }, "expired in-progress loses protection")
    }
}
