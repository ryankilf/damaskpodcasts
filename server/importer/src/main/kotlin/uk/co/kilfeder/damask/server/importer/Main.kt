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
    /** Skip the import when `podcasts` already has documents (the local stack's fixture seed). */
    val onlyIfEmpty: Boolean = false,
    /** The rankScore constants (RANK_HALF_LIFE_DAYS, RANK_FRESHNESS_FLOOR, RANK_POPULARITY_PRIOR). */
    val rank: RankScore = RankScore(),
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
            onlyIfEmpty = env["ONLY_IF_EMPTY"] == "true",
            rank = RankScore.fromEnv(env),
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
                "rescore" -> rescore(config)
                else -> throw IllegalArgumentException("Unknown command: $command")
            }
        }
    }.onFailure { log.error("{} failed: {}", command, it.message ?: it::class.simpleName) }.isSuccess
    exitProcess(if (ok) 0 else 1)
}

/** Returns null when skipped because of [ImporterConfig.onlyIfEmpty]. */
suspend fun runImport(config: ImporterConfig, today: LocalDate = LocalDate.now(ZoneOffset.UTC)): ImportCounts? {
    val meili = MeiliClient.create(config.meiliUrl, config.masterKey, timeoutMs = MEILI_TIMEOUT_MS)
    ensureSearchKey(config, meili)
    if (config.onlyIfEmpty && meili.indexExists(PodcastIndex.LIVE) && meili.documentCount(PodcastIndex.LIVE) > 0) {
        log.info("podcasts already has documents; skipping (ONLY_IF_EMPTY)")
        return null
    }
    val source = DumpSource(config.workDir)
    val given = config.dumpFile?.let(::File)
    val dump = given ?: downloadDump(source, config)
    // Everything the import itself wrote (the download, the unpacked database) is deleted
    // afterwards, success or not: about 6.7 GB a week otherwise. A DUMP_FILE you gave is kept.
    val written = mutableListOf<File>().apply { if (given == null) add(dump) }
    try {
        val unpackStarted = System.currentTimeMillis()
        val db = source.open(dump).also { if (it != dump) written += it }
        log.info("Opened {} ({} MB) in {} s", db.name, db.length() / BYTES_PER_MB, elapsedSeconds(unpackStarted))
        val now = today.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        val reader = DumpReader(db, nowEpochSeconds = now, rank = config.rank)
        val newIndex = "${PodcastIndex.LIVE}_${today.format(DateTimeFormatter.BASIC_ISO_DATE)}"
        val counts = IndexBuilder(meili, batchSize = config.batchSize).build(reader, newIndex)
        log.info(
            "Import done: {} rows, {} kept, {} indexed, {} s",
            counts.totalRows, counts.kept, counts.indexed, counts.durationMs / MS_PER_SECOND,
        )
        return counts
    } finally {
        written.forEach { it.delete() }
    }
}

/**
 * `importer rescore`: recomputes `rankScore` on the live index with the current RANK_* constants,
 * in place, without a re-import (each document keeps the inputs: popularity, newest date and the
 * import time). Uses Meilisearch's edit-documents-by-function; see [Rescorer].
 */
suspend fun rescore(config: ImporterConfig) {
    val meili = MeiliClient.create(config.meiliUrl, config.masterKey, timeoutMs = MEILI_TIMEOUT_MS)
    val started = System.currentTimeMillis()
    Rescorer(meili).rescore(PodcastIndex.LIVE, config.rank)
    log.info("Rescored {} with {} in {} s", PodcastIndex.LIVE, config.rank, elapsedSeconds(started))
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
    val started = System.currentTimeMillis()
    val file = client.use { source.download(it, config.dumpUrl, config.userAgent) }
    log.info("Downloaded {} MB in {} s", file.length() / BYTES_PER_MB, elapsedSeconds(started))
    return file
}

private fun elapsedSeconds(startedMs: Long): Long = (System.currentTimeMillis() - startedMs) / MS_PER_SECOND

private const val BYTES_PER_MB = 1024 * 1024

private const val MEILI_TIMEOUT_MS = 120_000L
private const val DOWNLOAD_TIMEOUT_MS = 2 * 60 * 60 * 1000L
private const val MS_PER_SECOND = 1_000
