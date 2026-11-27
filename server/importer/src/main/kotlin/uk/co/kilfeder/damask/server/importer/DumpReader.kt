package uk.co.kilfeder.damask.server.importer

import uk.co.kilfeder.damask.server.meili.PodcastDocument
import uk.co.kilfeder.damask.server.meili.PodcastIndex
import java.io.File
import org.sqlite.SQLiteConfig
import java.sql.Connection
import java.sql.ResultSet

/**
 * Reads the Podcast Index SQLite dump and keeps only feeds worth searching (spec › Search ›
 * Weekly import):
 *
 * - `dead = 0`;
 * - not a duplicate (`duplicateOf` NULL, or 0, which the dump also uses for "none");
 * - at least one episode;
 * - last fetch answered 2xx;
 * - not junk: popularity 0 together with fewer than 3 episodes or nothing newer than 3 years.
 */
class DumpReader(private val db: File, private val nowEpochSeconds: Long) {

    fun totalRows(): Long = connect().use { c ->
        c.createStatement().use { it.executeQuery("SELECT COUNT(*) FROM podcasts").also { r -> r.next() }.getLong(1) }
    }

    /** Kept rows as documents, in ID order, streamed from the database. Close it when done. */
    fun openKept(): KeptRows {
        val connection = connect()
        val statement = connection.prepareStatement(KEPT_SQL)
        statement.setLong(1, nowEpochSeconds - THREE_YEARS_SECONDS)
        statement.fetchSize = FETCH_SIZE
        return KeptRows(connection, statement.executeQuery())
    }

    /** Looks one row ahead, so [hasNext] has no side effects. */
    inner class KeptRows(private val connection: Connection, private val rows: ResultSet) :
        Iterator<PodcastDocument>, AutoCloseable {
        private var upcoming: PodcastDocument? = fetch()

        private fun fetch(): PodcastDocument? = if (rows.next()) rows.toDocument() else null

        override fun hasNext(): Boolean = upcoming != null

        override fun next(): PodcastDocument {
            val current = upcoming ?: throw NoSuchElementException()
            upcoming = fetch()
            return current
        }

        override fun close() {
            rows.close()
            connection.close()
        }
    }

    private fun connect(): Connection =
        SQLiteConfig().apply { setReadOnly(true) }.createConnection("jdbc:sqlite:${db.absolutePath}")

    private fun ResultSet.toDocument(): PodcastDocument {
        val id = getLong("id")
        val guid = getString("podcastGuid").orEmpty().trim()
        val chash = getString("chash").orEmpty().trim()
        val language = getString("language").orEmpty().trim()
        return PodcastDocument(
            id = id,
            title = getString("title").orEmpty().trim(),
            author = getString("itunesAuthor").orEmpty().trim(),
            ownerName = getString("itunesOwnerName").orEmpty().trim(),
            imageUrl = getString("imageUrl").orEmpty().trim().ifEmpty { null },
            feedUrl = getString("url").orEmpty().trim(),
            podcastGuid = guid.ifEmpty { null },
            language = language,
            languagePrefix = PodcastIndex.languagePrefix(language),
            explicit = getInt("explicit") != 0,
            itunesType = getString("itunesType").orEmpty().trim(),
            episodeCount = getInt("episodeCount"),
            newestItemPubdate = getLong("newestItemPubdate").takeUnless { wasNull() },
            popularityScore = getInt("popularityScore"),
            categories = (1..CATEGORY_COLUMNS)
                .mapNotNull { getString("category$it")?.trim()?.ifEmpty { null } }
                .distinct(),
            dedupeKey = when {
                guid.isNotEmpty() -> guid
                chash.isNotEmpty() -> chash
                else -> "id:$id"
            },
        )
    }

    private companion object {
        const val CATEGORY_COLUMNS = 10
        const val FETCH_SIZE = 5_000
        const val THREE_YEARS_SECONDS = 3L * 365 * 24 * 60 * 60
        val KEPT_SQL = """
            SELECT id, url, title, itunesAuthor, itunesOwnerName, imageUrl, podcastGuid, chash, language,
                   explicit, itunesType, episodeCount, newestItemPubdate, popularityScore,
                   category1, category2, category3, category4, category5,
                   category6, category7, category8, category9, category10
            FROM podcasts
            WHERE dead = 0
              AND (duplicateOf IS NULL OR duplicateOf = 0)
              AND episodeCount > 0
              AND lastHttpStatus BETWEEN 200 AND 299
              AND NOT (COALESCE(popularityScore, 0) = 0
                       AND (episodeCount < 3 OR COALESCE(newestItemPubdate, 0) < ?))
            ORDER BY id
        """.trimIndent()
    }
}
