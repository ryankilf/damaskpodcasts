package uk.co.kilfeder.damask.server.importer

import kotlinx.coroutines.runBlocking
import uk.co.kilfeder.damask.server.meili.MeiliContainer
import uk.co.kilfeder.damask.server.meili.MeiliQuery
import uk.co.kilfeder.damask.server.meili.PodcastDocument
import uk.co.kilfeder.damask.server.meili.PodcastIndex
import java.io.File
import java.nio.file.Files
import java.sql.DriverManager
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val FIXTURE = File(System.getProperty("damask.fixture.sql") ?: "../../deploy/fixtures/podcasts.sql")
private val TODAY: LocalDate = LocalDate.of(2026, 11, 27)
private val NOW_SECONDS = TODAY.atStartOfDay().toEpochSecond(ZoneOffset.UTC)

class DumpReaderTest {
    private val workDir = Files.createTempDirectory("dump-test").toFile()
    private val reader = DumpReader(DumpSource(workDir).open(FIXTURE), NOW_SECONDS)

    private fun kept(): List<PodcastDocument> = reader.openKept().use { it.asSequence().toList() }

    @Test
    fun keepsExactlyTheLiveNonJunkFeeds() {
        assertEquals(12, reader.totalRows())
        // Dropped: 3 dead, 4 duplicate, 5 no episodes, 6 HTTP 404, 7 junk (<3 eps), 8 junk (stale).
        assertEquals(listOf(1L, 2, 9, 10, 11, 12), kept().map { it.id })
    }

    @Test
    fun mapsRowsToDocuments() {
        val first = kept().first()
        assertEquals(
            PodcastDocument(
                id = 1,
                title = "Dan Carlin's Hardcore History",
                author = "Dan Carlin",
                ownerName = "Dan Carlin Owner",
                imageUrl = "https://img.example.com/hh.jpg",
                feedUrl = "https://feeds.example.com/hardcore-history",
                podcastGuid = "guid-hardcore-history",
                language = "en-US",
                languagePrefix = "en",
                languageTag = "en-us",
                explicit = false,
                itunesType = "episodic",
                episodeCount = 70,
                newestItemPubdate = 1_795_000_000,
                popularityScore = 5,
                categories = listOf("History", "Society"),
                topCategories = listOf("history", "societyandculture"),
                oldestItemPubdate = 1_100_000_000,
                appleListed = true,
                rankScore = RankScore().score(
                    5, 1_795_000_000, NOW_SECONDS, appleListed = true, oldestItemPubdate = 1_100_000_000,
                ),
                scoredAt = NOW_SECONDS,
                dedupeKey = "guid-hardcore-history",
            ),
            first,
        )
    }

    @Test
    fun dedupeKeyFallsBackFromGuidToChashToId() {
        val keys = kept().associate { it.id to it.dedupeKey }
        assertEquals("guid-hardcore-history", keys[2])
        assertEquals("chash-fr", keys[9])
        assertEquals("chash-fr", keys[10])
        assertEquals("id:11", keys[11])
    }

    @Test
    fun emptyStringsBecomeNulls() {
        val fr = kept().single { it.id == 9L }
        assertEquals(null, fr.podcastGuid)
        assertEquals(null, fr.imageUrl)
        assertTrue(fr.explicit)
        assertEquals("serial", fr.itunesType)
    }
}

class TarReaderTest {
    @Test
    fun extractsTheDatabaseFromAGzippedTar() {
        val dir = Files.createTempDirectory("tar-test").toFile()
        val content = File(dir, "podcastindex_feeds.db").apply { writeBytes(ByteArray(70_000) { (it % 251).toByte() }) }
        File(dir, "README.txt").writeText("not this one")
        val archive = File(dir, "dump.tgz")
        val tar = ProcessBuilder("tar", "czf", archive.path, "-C", dir.path, "README.txt", content.name)
            .inheritIO().start()
        assertEquals(0, tar.waitFor())

        val out = DumpSource(File(dir, "out")).extractDb(archive)
        assertEquals("podcastindex_feeds.db", out.name)
        assertTrue(content.readBytes().contentEquals(out.readBytes()))
    }
}

class IndexBuilderTest {
    private fun fixtureReader(): DumpReader {
        val db = DumpSource(Files.createTempDirectory("ib").toFile()).open(FIXTURE)
        return DumpReader(db, NOW_SECONDS)
    }

    @Test
    fun buildsAndSwapsInTheNewIndex(): Unit = runBlocking {
        val meili = MeiliContainer.masterClient()
        val counts = IndexBuilder(meili, batchSize = 4).build(fixtureReader(), "podcasts_20261127")
        assertEquals(12, counts.totalRows)
        assertEquals(6, counts.kept)
        assertEquals(6, counts.indexed)
        assertEquals(6, meili.documentCount(PodcastIndex.LIVE))
        assertFalse(meili.indexExists("podcasts_20261127"), "the old data is deleted after the swap")

        // A second run (next week) replaces it cleanly.
        IndexBuilder(meili).build(fixtureReader(), "podcasts_20261204")
        assertEquals(6, meili.documentCount(PodcastIndex.LIVE))
        assertFalse(meili.indexExists("podcasts_20261204"))
        // Feeds 1 and 2 share a podcast GUID, so search returns one of them, never both.
        val hits = meili.search(PodcastIndex.LIVE, MeiliQuery("hardcore", 10, 0)).hits
        assertEquals(listOf("guid-hardcore-history"), hits.map { it.dedupeKey })
    }

    @Test
    fun aFailedImportLeavesTheLiveIndexUntouched(): Unit = runBlocking {
        val meili = MeiliContainer.masterClient()
        IndexBuilder(meili).build(fixtureReader(), "podcasts_20261120")
        val before = meili.documentCount(PodcastIndex.LIVE)

        // A dump missing a column: the table exists (so counting works) but reading rows fails.
        val broken = File(Files.createTempDirectory("broken").toFile(), "broken.db")
        DriverManager.getConnection("jdbc:sqlite:${broken.path}").use {
            it.createStatement().executeUpdate("CREATE TABLE podcasts (id INTEGER PRIMARY KEY, title TEXT)")
        }
        val brokenReader = DumpReader(broken, NOW_SECONDS)
        assertFailsWith<Exception> { IndexBuilder(meili).build(brokenReader, "podcasts_20261127") }

        assertEquals(before, meili.documentCount(PodcastIndex.LIVE))
        assertFalse(meili.indexExists("podcasts_20261127"), "the half-built index is cleaned up")
    }
}
