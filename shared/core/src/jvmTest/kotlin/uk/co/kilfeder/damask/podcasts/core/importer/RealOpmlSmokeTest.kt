package uk.co.kilfeder.damask.podcasts.core.importer

import kotlinx.coroutines.runBlocking
import uk.co.kilfeder.damask.podcasts.core.data.CategoryPrefill
import uk.co.kilfeder.damask.podcasts.core.data.DeviceClock
import uk.co.kilfeder.damask.podcasts.core.data.FeedStore
import uk.co.kilfeder.damask.podcasts.core.data.LibraryRepository
import uk.co.kilfeder.damask.podcasts.core.data.inMemoryDriver
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.feed.FeedFetcher
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.core.rules.ListeningRules
import java.io.File
import kotlin.test.Test

/**
 * Opt-in: runs the real import against a real OPML file over the network.
 *
 *     ./gradlew :shared:core:jvmTest --tests '*RealOpmlSmokeTest*' -Pdamask.opml=llm-scrapbook/podcasts_opml.xml
 *
 * Skipped unless `damask.opml` is set, so `check` never touches the network. The report is written
 * next to the OPML file and deliberately contains no feed URLs (they can be private).
 */
class RealOpmlSmokeTest {
    @Test
    fun importRealOpml() = runBlocking {
        val path = System.getProperty("damask.opml")?.takeIf { it.isNotBlank() } ?: return@runBlocking
        val opml = File(path)
        val db = DamaskDatabase(inMemoryDriver())
        val clock = DeviceClock(db) { System.currentTimeMillis() }
        val store = FeedStore(db, clock)
        val importer = OpmlImporter(FeedFetcher(FeedFetcher.createClient()), store, CategoryPrefill(db))

        val started = System.currentTimeMillis()
        val candidates = importer.prepare(opml.readText())
        val elapsed = System.currentTimeMillis() - started
        importer.confirm(candidates.map { it to it.suggestedCategory })
        val rotation = Rotation(LibraryRepository(db).load(), ListeningRules(), System.currentTimeMillis())

        val report = buildString {
            appendLine("# Import smoke test: ${opml.name}")
            val problems = candidates.count { it.problem != null }
            appendLine("${candidates.size} feeds in ${elapsed / 1000}s; $problems problems")
            appendLine()
            appendLine("| Show | Type | Episodes | Undated | No duration | Feed category | Prefill | Problem |")
            appendLine("|---|---|---|---|---|---|---|---|")
            candidates.forEach { c ->
                val doc = c.feed?.document
                val category = doc?.categories?.firstOrNull()?.let { "${it.category} / ${it.subcategory}" }.orEmpty()
                appendLine(
                    "| ${c.title.take(40)} | ${doc?.itunesType.orEmpty()} | ${doc?.episodes?.size ?: "-"} | " +
                        "${doc?.episodes?.count { it.publishedAt == null } ?: "-"} | " +
                        "${doc?.episodes?.count { it.durationMs == null } ?: "-"} | $category | " +
                        "${c.suggestedCategory.value} | ${c.problem.orEmpty()} |",
                )
            }
            appendLine()
            appendLine("## Genres")
            rotation.genres().forEach { appendLine("- ${it.name}") }
            appendLine()
            appendLine("## First 15 of Up Next")
            rotation.upNext().take(15).forEach { appendLine("- ${it.key.guid.take(60)}") }
        }
        File(opml.parentFile, "import-report.md").writeText(report)
    }
}
