package uk.co.kilfeder.damask.server.importer

import kotlinx.coroutines.runBlocking
import uk.co.kilfeder.damask.server.meili.MeiliAdmin
import uk.co.kilfeder.damask.server.meili.MeiliClient
import uk.co.kilfeder.damask.server.meili.MeiliContainer
import uk.co.kilfeder.damask.server.meili.PodcastDocument
import uk.co.kilfeder.damask.server.meili.PodcastIndex
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.double
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val DAY = 86_400L
private const val NOW = 1_795_737_600L // 2026-11-27T00:00Z

class RankScoreTest {
    private val rank = RankScore()

    @Test
    fun freshPopularShowsScoreHighestAndDecaySmoothly() {
        val fresh = rank.score(popularity = 9, newestItemPubdate = NOW, scoredAt = NOW)
        assertEquals(10.0, fresh, 1e-9, "(9 + prior 1) at full freshness")
        val halfLife = rank.score(9, NOW - 365 * DAY, NOW)
        assertEquals(10.0 * (0.4 + 0.6 * 0.5), halfLife, 1e-9)
        // No cliffs: a day older is only slightly lower.
        val a = rank.score(9, NOW - 90 * DAY, NOW)
        val b = rank.score(9, NOW - 91 * DAY, NOW)
        assertTrue(a > b && a - b < 0.02, "$a vs $b")
    }

    @Test
    fun oldPopularShowsKeepTheFloorAndMissingDatesCountAsVeryOld() {
        assertEquals(4.0, rank.score(9, newestItemPubdate = null, scoredAt = NOW), 1e-9)
        assertEquals(4.0, rank.score(9, NOW - 50 * 365 * DAY, NOW), 1e-6)
    }

    @Test
    fun popularityZeroIsNotFlattenedAndOutliersAreClamped() {
        assertEquals(1.0, rank.score(0, NOW, NOW), 1e-9)
        assertEquals(rank.score(9, NOW, NOW), rank.score(29, NOW, NOW), 1e-9)
        assertEquals(rank.score(0, NOW, NOW), rank.score(-3, NOW, NOW), 1e-9)
    }

    @Test
    fun recencyCanOutrankPopularity() {
        // A popularity-5 show that posted this week beats a popularity-9 show silent for three years.
        assertTrue(rank.score(5, NOW - 7 * DAY, NOW) > rank.score(9, NOW - 3 * 365 * DAY, NOW))
    }

    @Test
    fun anAppleListedShowBeatsAnUnlistedReUploadOfIt() {
        // The real case (dump of 21 Nov 2026), scored at 27 Nov: MAC's unlisted re-upload of Dan
        // Carlin's catalogue (oldest episode March 2026, newest 26 Aug) against the real feed
        // (Apple-listed, oldest episode July 2018, newest 31 Jul). Both have popularity 9.
        val copy = rank.score(9, NOW - 93 * DAY, NOW, appleListed = false, oldestItemPubdate = NOW - 252 * DAY)
        val real = rank.score(9, NOW - 119 * DAY, NOW, appleListed = true, oldestItemPubdate = NOW - 3058 * DAY)
        assertTrue(real > copy * 1.4, "real $real, copy $copy")
    }

    @Test
    fun theAppleBoostOutweighsAFewPopularityPointsButNotAWeakShow() {
        val listed = { pop: Int -> rank.score(pop, NOW, NOW, appleListed = true) }
        val unlisted = { pop: Int -> rank.score(pop, NOW, NOW, appleListed = false) }
        assertTrue(listed(9) > unlisted(9), "same popularity")
        assertTrue(listed(7) > unlisted(9), "two points lower still wins: 8 x 1.5 = 12 > 10")
        assertTrue(listed(5) < unlisted(9), "four points lower loses: 6 x 1.5 = 9 < 10")
        assertTrue(listed(2) < unlisted(9), "a weak listed show loses")
        // A boost, not a filter: an active unlisted show still beats a dormant listed one.
        assertTrue(rank.score(9, NOW - 5 * 365 * DAY, NOW, appleListed = true) < unlisted(9))
    }

    @Test
    fun feedAgeLiftsGentlyAndIsCapped() {
        val at = { years: Double -> rank.score(9, NOW, NOW, oldestItemPubdate = NOW - (years * 365 * DAY).toLong()) }
        assertEquals(10.0, at(0.0), 1e-9)
        assertEquals(10.5, at(2.5), 1e-9)
        assertEquals(11.0, at(5.0), 1e-9)
        assertEquals(11.0, at(20.0), 1e-9, "capped at 5 years")
        assertEquals(10.0, rank.score(9, NOW, NOW, oldestItemPubdate = null), 1e-9, "no date: no lift")
        assertEquals(10.0, rank.score(9, NOW, NOW, oldestItemPubdate = NOW + DAY), 1e-9, "future date: no lift")
    }

    @Test
    fun aStepMakesNearlyEqualScoresTie() {
        val stepped = RankScore(step = 1.0)
        assertEquals(9.0, stepped.score(9, NOW - 1 * DAY, NOW), 1e-9)
        assertEquals(9.0, stepped.score(9, NOW - 2 * DAY, NOW), 1e-9)
        assertTrue(RankScore().score(9, NOW - 1 * DAY, NOW) > RankScore().score(9, NOW - 2 * DAY, NOW))
    }

    @Test
    fun constantsComeFromTheEnvironment() {
        val tuned = RankScore.fromEnv(mapOf("RANK_HALF_LIFE_DAYS" to "180", "RANK_FRESHNESS_FLOOR" to "0.2"))
        assertEquals(RankScore(halfLifeDays = 180.0, freshnessFloor = 0.2), tuned)
        assertEquals(RankScore(), RankScore.fromEnv(emptyMap()))
    }
}

/** The in-place rescore (Rhai, run by Meilisearch) must agree with the Kotlin RankScore. */
class RescorerTest {
    @Test
    fun rescoreMatchesTheKotlinScore(): Unit = runBlocking {
        val meili = MeiliContainer.masterClient()
        val admin = MeiliAdmin(meili)
        val index = "rescore_test"
        if (meili.indexExists(index)) meili.awaitTask(admin.deleteIndex(index))
        meili.awaitTask(admin.createIndex(index))
        val cases = listOf(
            Case(1L, 9, NOW, listed = true, oldest = NOW - 9 * 365 * DAY),
            Case(2L, 0, NOW - 30 * DAY, listed = false, oldest = NOW - 400 * DAY),
            Case(3L, 5, NOW - 400 * DAY, listed = true, oldest = null),
            Case(4L, 29, NOW - 10 * DAY, listed = false, oldest = NOW + DAY),
            Case(5L, 3, null, listed = true, oldest = NOW - 2 * 365 * DAY),
        )
        val docs = cases.map { c ->
            PodcastDocument(
                id = c.id, title = "t${c.id}", feedUrl = "https://e.com/${c.id}", dedupeKey = "id:${c.id}",
                popularityScore = c.pop, newestItemPubdate = c.newest, scoredAt = NOW, rankScore = -1.0,
                appleListed = c.listed, oldestItemPubdate = c.oldest,
            )
        }
        val ndjson = docs.joinToString("\n") { MeiliClient.json.encodeToString(PodcastDocument.serializer(), it) }
        meili.awaitTask(admin.addDocumentsNdjson(index, ndjson))

        listOf(
            RankScore(),
            RankScore(
                halfLifeDays = 180.0, freshnessFloor = 0.25, popularityPrior = 2.0,
                appleListedBoost = 2.0, feedAgeBoost = 0.3, feedAgeFullYears = 3.0,
            ),
            RankScore(step = 0.5),
        ).forEach { tuned ->
            Rescorer(meili, admin).rescore(index, tuned)
            cases.forEach { c ->
                val doc: JsonObject = meili.document(index, c.id)
                val stored = (doc["rankScore"] as JsonPrimitive).double
                val expected = tuned.score(c.pop, c.newest, NOW, c.listed, c.oldest)
                assertTrue(abs(stored - expected) < 1e-9, "doc ${c.id} ($tuned): Meilisearch $stored, Kotlin $expected")
            }
        }
        meili.awaitTask(admin.deleteIndex(index))
        assertTrue(PodcastIndex.RANKING_RULES.contains("rankScore:desc"))
    }

    private data class Case(val id: Long, val pop: Int, val newest: Long?, val listed: Boolean, val oldest: Long?)
}
