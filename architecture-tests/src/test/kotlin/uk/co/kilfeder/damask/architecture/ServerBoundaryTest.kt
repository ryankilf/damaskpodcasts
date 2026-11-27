package uk.co.kilfeder.damask.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue as kAssertTrue

/**
 * Rules for the search backend (spec › Architecture): the server may only share `shared/api` with
 * the app, the API module stays dependency-free, and the gateway's routes only reach search
 * backends through `SearchSource`.
 */
class ServerBoundaryTest {

    @Test
    fun `server code imports nothing from shared except shared api`() =
        serverScope().files.assertFalse(testName = "server imports app code") { file ->
            file.hasImport { it.name.startsWith(APP_PKG) && !it.name.startsWith(API_PKG) }
        }

    @Test
    fun `server build files depend on no shared module but api`() {
        SERVER_MODULES.forEach { module ->
            val build = File(rootDir(), "$module/build.gradle.kts").readText()
            val shared = Regex("""projects\.shared\.(\w+)""").findAll(build).map { it.groupValues[1] }.toSet()
            kAssertTrue(shared.all { it == "api" }, "$module depends on shared modules $shared")
        }
    }

    @Test
    fun `shared api imports only kotlin and kotlinx serialization`() =
        scope(API).files.assertFalse(testName = "api imports") { file ->
            file.hasImport { import -> ALLOWED_API_IMPORTS.none { import.name.startsWith(it) } }
        }

    @Test
    fun `shared api DTOs are serializable data classes with only vals`() =
        scope(API).classes()
            .filter { it.hasAnnotationWithName("Serializable") }
            .assertTrue(testName = "api DTO shape") { dto ->
                dto.hasDataModifier && dto.properties().none { it.isVar }
            }

    @Test
    fun `server main code never prints to stdout or stderr`() =
        serverScope().files
            .filterNot { it.sourceSetName.contains("test", ignoreCase = true) }
            .assertFalse(testName = "server printing") { file -> PRINTING.containsMatchIn(file.text) }

    @Test
    fun `routes reach search backends only through SearchSource`() {
        val gateway = scope(SEARCH).files.filterNot { it.sourceSetName.contains("test", ignoreCase = true) }
        // Languages is a pure normaliser shared with the importer, not a way to reach Meilisearch.
        val usesMeili = gateway
            .filter { file -> file.hasImport { it.name.startsWith(MEILI_PKG) && it.name != "$MEILI_PKG.Languages" } }
            .map { it.name }
        assertEquals(
            setOf("MeiliSource", "RegionMerge", "Main"),
            usesMeili.toSet(),
            "only MeiliSource (and its RegionMerge) and wiring use Meilisearch",
        )
        val usesPodcastIndex = gateway.filter { "api.podcastindex.org" in it.text }.map { it.name }
        assertEquals(listOf("PodcastIndexSource"), usesPodcastIndex, "only PodcastIndexSource calls Podcast Index")
        val routes = gateway.filter { "routing {" in it.text }
        routes.assertFalse(testName = "routes use a concrete source") { file ->
            "MeiliClient" in file.text || "PodcastIndexSource" in file.text || "MeiliSource" in file.text
        }
    }

    @Test
    fun `the gateway never installs Ktor CallLogging`() =
        // CallLogging logs the full URI, query string included.
        scope(SEARCH).files.assertFalse(testName = "CallLogging") { file ->
            file.hasImport { it.name.startsWith("io.ktor.server.plugins.calllogging") }
        }

    private fun serverScope(): KoScope = SERVER_MODULES.map(::scope).reduce { a, b -> a + b }

    private fun scope(module: String): KoScope = Konsist.scopeFromModule(module).also {
        kAssertTrue(it.files.isNotEmpty(), "No sources found for module '$module'")
    }

    /** The repository root: Konsist resolves modules from it; the build-file check reads it directly. */
    private fun rootDir(): File =
        generateSequence(File("").absoluteFile) { it.parentFile }.first { File(it, "settings.gradle.kts").exists() }

    private companion object {
        const val API = "shared/api"
        const val SEARCH = "server/search"
        val SERVER_MODULES = listOf("server/search", "server/importer", "server/meili")

        const val APP_PKG = "uk.co.kilfeder.damask.podcasts."
        const val API_PKG = "uk.co.kilfeder.damask.podcasts.api"
        const val MEILI_PKG = "uk.co.kilfeder.damask.server.meili"

        val ALLOWED_API_IMPORTS = listOf("kotlin.", "kotlinx.serialization.", API_PKG)
        val PRINTING = Regex("""\bprintln\(|\bprint\(|System\.out|System\.err""")
    }
}
