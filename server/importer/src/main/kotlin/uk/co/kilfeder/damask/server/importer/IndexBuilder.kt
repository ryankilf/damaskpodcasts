package uk.co.kilfeder.damask.server.importer

import uk.co.kilfeder.damask.server.meili.MeiliAdmin
import uk.co.kilfeder.damask.server.meili.MeiliClient
import uk.co.kilfeder.damask.server.meili.MeiliTask
import uk.co.kilfeder.damask.server.meili.PodcastDocument
import uk.co.kilfeder.damask.server.meili.PodcastIndex

/** What one import did. */
data class ImportCounts(
    val totalRows: Long,
    val kept: Long,
    val indexed: Long,
    val durationMs: Long,
)

/**
 * Builds a fresh index from the dump and swaps it in (spec › Search › Weekly import):
 * new `podcasts_<date>` -> settings -> NDJSON batches -> every task waited on -> document count
 * checked -> swap with `podcasts` -> old index deleted. If anything fails before the swap, the new
 * index is deleted and `podcasts` is left exactly as it was.
 */
class IndexBuilder(
    private val meili: MeiliClient,
    private val admin: MeiliAdmin = MeiliAdmin(meili),
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
    private val clockMs: () -> Long = System::currentTimeMillis,
) {
    suspend fun build(reader: DumpReader, newIndex: String): ImportCounts {
        val started = clockMs()
        val total = reader.totalRows()
        if (meili.indexExists(newIndex)) meili.awaitTask(admin.deleteIndex(newIndex)) // a failed run earlier today
        meili.awaitTask(admin.createIndex(newIndex))
        try {
            meili.awaitTask(admin.updateSettings(newIndex, PodcastIndex.settings))
            val kept = upload(reader, newIndex)
            val indexed = meili.documentCount(newIndex)
            if (indexed != kept) throw ImportException("Indexed $indexed documents but kept $kept rows")
            swapIn(newIndex)
            return ImportCounts(total, kept, indexed, clockMs() - started)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            runCatching { meili.awaitTask(admin.deleteIndex(newIndex)) }
            throw e
        }
    }

    /** Streams kept rows in batches; each batch is uploaded as soon as it's full. */
    private suspend fun upload(reader: DumpReader, index: String): Long {
        val batch = ArrayList<PodcastDocument>(batchSize)
        val tasks = mutableListOf<MeiliTask>()
        var kept = 0L
        reader.openKept().use { rows ->
            for (doc in rows) {
                batch += doc
                kept++
                if (batch.size == batchSize) {
                    tasks += admin.addDocumentsNdjson(index, ndjson(batch))
                    batch.clear()
                }
            }
        }
        if (batch.isNotEmpty()) tasks += admin.addDocumentsNdjson(index, ndjson(batch))
        tasks.forEach { meili.awaitTask(it) }
        return kept
    }

    private suspend fun swapIn(newIndex: String) {
        if (!meili.indexExists(PodcastIndex.LIVE)) meili.awaitTask(admin.createIndex(PodcastIndex.LIVE))
        meili.awaitTask(admin.swapIndexes(PodcastIndex.LIVE, newIndex))
        // After the swap, newIndex holds the previous live data.
        meili.awaitTask(admin.deleteIndex(newIndex))
    }

    private fun ndjson(docs: List<PodcastDocument>): String =
        docs.joinToString("\n") { MeiliClient.json.encodeToString(PodcastDocument.serializer(), it) }

    companion object {
        const val DEFAULT_BATCH_SIZE = 10_000
    }
}

class ImportException(message: String) : Exception(message)
