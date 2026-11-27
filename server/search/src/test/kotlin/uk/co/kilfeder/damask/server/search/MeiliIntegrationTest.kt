package uk.co.kilfeder.damask.server.search

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import uk.co.kilfeder.damask.podcasts.api.ApiJson
import uk.co.kilfeder.damask.podcasts.api.HealthResponse
import uk.co.kilfeder.damask.podcasts.api.SearchResponse
import uk.co.kilfeder.damask.server.meili.Languages
import uk.co.kilfeder.damask.server.meili.MeiliAdmin
import uk.co.kilfeder.damask.server.meili.MeiliClient
import uk.co.kilfeder.damask.server.meili.MeiliContainer
import uk.co.kilfeder.damask.server.meili.PodcastDocument
import uk.co.kilfeder.damask.server.meili.PodcastIndex
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The gateway against a real Meilisearch (Testcontainers), through the search-only key, with the
 * production index settings. Skipped when Docker isn't available.
 */
class MeiliIntegrationTest {
    private lateinit var source: MeiliSource

    @Suppress("LongParameterList")
    private fun doc(
        id: Long,
        title: String,
        dedupeKey: String,
        language: String = "en",
        explicit: Boolean = false,
        top: List<String> = listOf("history"),
        rankScore: Double = 4.0,
    ) = PodcastDocument(
        id = id,
        title = title,
        author = "Author $id",
        feedUrl = "https://feeds.example.com/$id",
        language = language,
        languagePrefix = PodcastIndex.languagePrefix(language),
        languageTag = Languages.tag(language),
        explicit = explicit,
        itunesType = "episodic",
        episodeCount = 10,
        newestItemPubdate = 1_795_000_000,
        popularityScore = 3,
        categories = listOf("History"),
        topCategories = top,
        rankScore = rankScore,
        dedupeKey = dedupeKey,
    )

    private val docs = listOf(
        doc(1, "Dan Carlin's Hardcore History", "guid-hh", "en-US"),
        doc(2, "Hardcore History (mirror feed)", "guid-hh", "en"), // same show, another feed
        doc(3, "Histoire Explicite", "chash-fr", "fr", explicit = true),
        doc(4, "Histoire de France", "chash-fr2", "fr-FR"),
        doc(5, "Explicit History Hour", "id:5", "en-GB", explicit = true),
        doc(6, "Hardcore Unlisted", "id:6", language = "", top = emptyList()), // no language, no category
        // Region boost: the same show quality in two regions.
        doc(7, "Morning Briefing Show", "id:7", "en-GB", top = listOf("news")),
        doc(8, "Morning Briefing Show", "id:8", "en-US", top = listOf("news")),
        // ...and a much stronger en-US match that a weak en-GB one must not beat.
        doc(9, "Evening Briefing", "id:9", "en-US", top = listOf("news"), rankScore = 10.0),
        doc(10, "Evening Briefing Extra Words Here", "id:10", "en_GB", top = listOf("news"), rankScore = 1.0),
        // Same name, different categories.
        doc(11, "Unfiltered", "id:11", top = listOf("news")),
        doc(12, "Unfiltered", "id:12", top = listOf("comedy")),
    ) + (1L..6L).flatMap { n ->
        // Six shows each listed twice, en-GB and en-US, under one dedupeKey: cross-list duplicates.
        listOf(
            doc(100 + n, "Twin Feed Show $n", "twin-$n", "en-GB", rankScore = 2.0 + n),
            doc(200 + n, "Twin Feed Show $n", "twin-$n", "en-US", rankScore = 2.0 + n),
        )
    }

    @BeforeTest
    fun index() = runBlocking {
        val master = MeiliContainer.masterClient()
        val admin = MeiliAdmin(master)
        if (master.indexExists(PodcastIndex.LIVE)) master.awaitTask(admin.deleteIndex(PodcastIndex.LIVE))
        master.awaitTask(admin.createIndex(PodcastIndex.LIVE))
        master.awaitTask(admin.updateSettings(PodcastIndex.LIVE, PodcastIndex.settings))
        val ndjson = docs.joinToString("\n") { MeiliClient.json.encodeToString(PodcastDocument.serializer(), it) }
        master.awaitTask(admin.addDocumentsNdjson(PodcastIndex.LIVE, ndjson))
        val key = admin.ensureKey(
            uid = "0c9e3f52-7f61-4f3a-9a0e-0d4f6b6a2c11",
            description = "integration test",
            actions = listOf("search"),
            indexes = listOf(PodcastIndex.LIVE),
        )
        source = MeiliSource(MeiliClient.create(MeiliContainer.require(), key.key, timeoutMs = 2_000))
    }

    private fun search(query: String): SearchResponse {
        var result: SearchResponse? = null
        gatewayTest(deps(source)) { client ->
            val response = client.get("/v1/search?$query") { bearerAuth(PRIVATE_TOKEN) }
            assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
            result = ApiJson.decodeFromString(SearchResponse.serializer(), response.bodyAsText())
        }
        return result!!
    }

    @Test
    fun typoTolerance() {
        val titles = search("q=hardcore%20histry").hits.map { it.title }
        assertTrue(titles.any { "Hardcore History" in it }, titles.toString())
    }

    @Test
    fun duplicatesCollapseByDedupeKey() {
        val ids = search("q=hardcore").hits.map { it.podcastIndexId }
        assertEquals(1, ids.count { it == 1L || it == 2L }, "one of feeds 1 and 2, never both: $ids")
    }

    @Test
    fun languageFilterMatchesThePrefix() {
        val hits = search("q=histoire&lang=fr").hits
        assertEquals(setOf(3L, 4L), hits.map { it.podcastIndexId }.toSet())
        assertTrue(search("q=histoire&lang=en").hits.isEmpty())
    }

    @Test
    fun hideExplicitFilter() {
        val all = search("q=history").hits.map { it.podcastIndexId }.toSet()
        val clean = search("q=history&hideExplicit=true").hits.map { it.podcastIndexId }.toSet()
        assertTrue(5L in all)
        assertTrue(5L !in clean)
        // Not just "no explicit hits": the clean shows must still be there (an empty result once
        // passed this test while the filter matched nothing at all).
        assertTrue(1L in clean || 2L in clean, "clean history shows survive the filter: $clean")
        assertEquals(setOf(4L), search("q=histoire&hideExplicit=true").hits.map { it.podcastIndexId }.toSet())
    }

    @Test
    fun languageAndExplicitFiltersCombine() {
        val ids = search("q=history&lang=en&hideExplicit=true").hits.map { it.podcastIndexId }.toSet()
        assertTrue(ids.isNotEmpty())
        assertTrue(5L !in ids && 3L !in ids && 4L !in ids, ids.toString())
    }

    @Test
    fun showsWithNoLanguagePassTheLanguageFilter() {
        assertTrue(6L in search("q=hardcore&lang=en").hits.map { it.podcastIndexId })
        assertTrue(6L in search("q=hardcore&lang=en-GB").hits.map { it.podcastIndexId })
        assertTrue(search("q=histoire&lang=en").hits.none { it.podcastIndexId == 3L || it.podcastIndexId == 4L })
    }

    @Test
    fun aRegionNudgesItsShowsUp() {
        val gb = search("q=morning%20briefing&lang=en-GB").hits.map { it.podcastIndexId }
        val us = search("q=morning%20briefing&lang=en-US").hits.map { it.podcastIndexId }
        assertEquals(listOf(7L, 8L), gb.filter { it == 7L || it == 8L }, "en-GB first for an en-GB device")
        assertEquals(listOf(8L, 7L), us.filter { it == 7L || it == 8L }, "en-US first for an en-US device")
        val plain = search("q=morning%20briefing&lang=en").hits.map { it.podcastIndexId }
        assertEquals(setOf(7L, 8L), plain.filter { it == 7L || it == 8L }.toSet(), "both still appear")
    }

    @Test
    fun aRegionDoesNotBeatAMuchBetterMatch() {
        val gb = search("q=evening%20briefing&lang=en-GB").hits.map { it.podcastIndexId }
        assertEquals(9L, gb.first(), "the strong en-US show still wins: $gb")
        assertTrue(10L in gb, "the en_GB one (normalised) is still there")
    }

    @Test
    fun pagesWithCrossRegionDuplicatesStillFill() {
        val hits = search("q=twin%20feed%20show&lang=en-GB&limit=5").hits
        assertEquals(5, hits.size, "the page fills despite six duplicate pairs")
        assertEquals(5, hits.map { it.podcastIndexId % 100 }.toSet().size, "one copy per show")
        assertTrue(hits.all { it.podcastIndexId in 101L..106L }, "the boosted en-GB copy wins each pair: $hits")
        val next = search("q=twin%20feed%20show&lang=en-GB&limit=5&offset=5").hits.map { it.podcastIndexId % 100 }
        assertEquals(1, next.size, "the sixth show is on page two")
        assertTrue(next.single() !in hits.map { it.podcastIndexId % 100 }, "no show on both pages")
    }

    @Test
    fun categoriesFilterAndOr() {
        assertEquals(setOf(11L, 12L), search("q=unfiltered").hits.map { it.podcastIndexId }.toSet())
        assertEquals(listOf(11L), search("q=unfiltered&category=news").hits.map { it.podcastIndexId })
        assertEquals(
            setOf(11L, 12L),
            search("q=unfiltered&category=news&category=comedy").hits.map { it.podcastIndexId }.toSet(),
        )
        // A show with no category is excluded only when a category is asked for.
        assertTrue(6L in search("q=hardcore").hits.map { it.podcastIndexId })
        assertTrue(6L !in search("q=hardcore&category=history").hits.map { it.podcastIndexId })
        assertEquals(listOf("news"), search("q=unfiltered&category=news").hits.single().topCategories)
    }

    @Test
    fun healthReportsTheIndex() {
        gatewayTest(deps(source)) { client ->
            val health = ApiJson.decodeFromString(HealthResponse.serializer(), client.get("/health").bodyAsText())
            assertTrue(health.db)
        }
    }

    @Test
    fun meilisearchDownIsA503AndDbFalse() {
        val dead = MeiliSource(MeiliClient.create("http://127.0.0.1:1", "x", timeoutMs = 500))
        gatewayTest(deps(dead)) { client ->
            val search = client.get("/v1/search?q=x") { bearerAuth(PRIVATE_TOKEN) }
            assertEquals(HttpStatusCode.ServiceUnavailable, search.status)
            assertEquals(HttpStatusCode.ServiceUnavailable, client.get("/health").status)
        }
    }
}
