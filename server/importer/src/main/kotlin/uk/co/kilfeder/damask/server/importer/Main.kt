package uk.co.kilfeder.damask.server.importer

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import uk.co.kilfeder.damask.server.meili.MeiliAdmin
import uk.co.kilfeder.damask.server.meili.MeiliClient
import uk.co.kilfeder.damask.server.meili.PodcastIndex
import java.io.File
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

private val log = LoggerFactory.getLogger("importer")

/** Everything comes from the environment (spec › Deploy); see deploy/.env.example. */
data class ImporterConfig(
    val meiliUrl: String,
    val masterKey: String,
    val searchKeyUid: String?,
    val dumpUrl: String,
    val dumpFile: String?,
    val workDir: File,
    val userAgent: String,
    val batchSize: Int,
) {
    companion object {
        const val DEFAULT_DUMP_URL = "https://public.podcastindex.org/podcastindex_feeds.db.tgz"

        fun fromEnv(env: Map<String, String> = System.getenv()): ImporterConfig = ImporterConfig(
            meiliUrl = env["MEILI_URL"] ?: "http://127.0.0.1:7700",
            masterKey = env["MEILI_MASTER_KEY"]?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("MEILI_MASTER_KEY is required"),
            searchKeyUid = env["MEILI_SEARCH_KEY_UID"]?.takeIf { it.isNotBlank() },
            dumpUrl = env["DUMP_URL"] ?: DEFAULT_DUMP_URL,
            dumpFile = env["DUMP_FILE"]?.takeIf { it.isNotBlank() },
            workDir = File(env["WORK_DIR"] ?: System.getProperty("java.io.tmpdir")).resolve("damask-import"),
            userAgent = "Damask/${env["DAMASK_VERSION"] ?: "dev"} (search index importer)",
            batchSize = env["BATCH_SIZE"]?.toIntOrNull() ?: IndexBuilder.DEFAULT_BATCH_SIZE,
        )
    }
}

/**
 * `importer [import|ensure-search-key]`. Exits 0 on success, 1 on any failure (leaving the live
 * `podcasts` index untouched).
 */
fun main(args: Array<String>) {
    val command = args.firstOrNull() ?: "import"
    val ok = runCatching {
        val config = ImporterConfig.fromEnv()
        runBlocking {
            when (command) {
                "import" -> runImport(config)
                "ensure-search-key" -> ensureSearchKey(config)
                else -> throw IllegalArgumentException("Unknown command: $command")
            }
        }
    }.onFailure { log.error("{} failed: {}", command, it.message ?: it::class.simpleName) }.isSuccess
    exitProcess(if (ok) 0 else 1)
}

suspend fun runImport(config: ImporterConfig, today: LocalDate = LocalDate.now(ZoneOffset.UTC)): ImportCounts {
    val meili = MeiliClient.create(config.meiliUrl, config.masterKey, timeoutMs = MEILI_TIMEOUT_MS)
    ensureSearchKey(config, meili)
    val source = DumpSource(config.workDir)
    val dump = config.dumpFile?.let(::File) ?: downloadDump(source, config)
    log.info("Reading dump {}", dump.name)
    val db = source.open(dump)
    val reader = DumpReader(db, nowEpochSeconds = today.atStartOfDay().toEpochSecond(ZoneOffset.UTC))
    val newIndex = "${PodcastIndex.LIVE}_${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"
    val counts = IndexBuilder(meili, batchSize = config.batchSize).build(reader, newIndex)
    log.info(
        "Import done: {} rows, {} kept, {} indexed, {} s",
        counts.totalRows, counts.kept, counts.indexed, counts.durationMs / MS_PER_SECOND,
    )
    return counts
}

/**
 * Makes sure the gateway's key exists: search-only, on `podcasts` only. Its value is derived by
 * Meilisearch from the UID and the master key (see deploy/README.md), so it's never printed.
 */
suspend fun ensureSearchKey(config: ImporterConfig, meili: MeiliClient? = null) {
    val uid = config.searchKeyUid ?: return log.info("MEILI_SEARCH_KEY_UID not set; skipping search key")
    val client = meili ?: MeiliClient.create(config.meiliUrl, config.masterKey, timeoutMs = MEILI_TIMEOUT_MS)
    MeiliAdmin(client).ensureKey(
        uid = uid,
        description = "Damask search gateway",
        actions = listOf("search"),
        indexes = listOf(PodcastIndex.LIVE),
    )
    log.info("Search key {} present", uid)
}

private suspend fun downloadDump(source: DumpSource, config: ImporterConfig): File {
    log.info("Downloading dump")
    val client = HttpClient(CIO) {
        install(HttpTimeout) { requestTimeoutMillis = DOWNLOAD_TIMEOUT_MS }
    }
    return client.use { source.download(it, config.dumpUrl, config.userAgent) }
}

private const val MEILI_TIMEOUT_MS = 120_000L
private const val DOWNLOAD_TIMEOUT_MS = 2 * 60 * 60 * 1000L
private const val MS_PER_SECOND = 1_000
