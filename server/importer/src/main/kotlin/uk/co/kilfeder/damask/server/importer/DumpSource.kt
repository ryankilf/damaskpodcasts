package uk.co.kilfeder.damask.server.importer

import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File
import java.io.InputStream
import java.sql.DriverManager
import java.util.zip.GZIPInputStream

class DumpException(message: String) : Exception(message)

/**
 * Turns whatever the importer was given into a SQLite file it can read:
 *
 * - a URL (the weekly `podcastindex_feeds.db.tgz`): downloaded with an honest User-Agent (Podcast
 *   Index refuses generic ones with a 403), then unpacked;
 * - a local `.tgz` / `.tar.gz`: unpacked;
 * - a local `.db`: used as is;
 * - a local `.sql`: executed into a fresh database (the fixture used by tests and local compose).
 */
class DumpSource(private val workDir: File) {

    suspend fun download(client: HttpClient, url: String, userAgent: String): File {
        workDir.mkdirs()
        val target = File(workDir, url.substringAfterLast('/').ifEmpty { "dump.tgz" })
        client.prepareGet(url) { header(HttpHeaders.UserAgent, userAgent) }.execute { response ->
            if (!response.status.isSuccess()) throw DumpException("Dump download failed: HTTP ${response.status.value}")
            target.outputStream().use { out -> response.bodyAsChannel().copyTo(out) }
        }
        return target
    }

    fun open(file: File): File = when {
        file.name.endsWith(".tgz") || file.name.endsWith(".tar.gz") -> extractDb(file)
        file.name.endsWith(".sql") -> loadSql(file)
        file.name.endsWith(".db") -> file
        else -> throw DumpException("Unsupported dump file: ${file.name}")
    }

    /** Extracts the first `*.db` entry of a gzipped tar into [workDir]. */
    internal fun extractDb(archive: File): File {
        workDir.mkdirs()
        GZIPInputStream(archive.inputStream().buffered()).use { tar ->
            TarReader(tar).entries().forEach { entry ->
                // Skip macOS AppleDouble metadata ("._name.db") that macOS tar adds beside each file.
                val baseName = entry.name.substringAfterLast('/')
                if (entry.isFile && baseName.endsWith(".db") && !baseName.startsWith("._")) {
                    val out = File(workDir, entry.name.substringAfterLast('/'))
                    out.outputStream().use { entry.copyTo(it) }
                    return out
                }
            }
        }
        throw DumpException("No .db file inside ${archive.name}")
    }

    private fun loadSql(sql: File): File {
        workDir.mkdirs()
        val db = File(workDir, sql.nameWithoutExtension + ".db").also { it.delete() }
        DriverManager.getConnection("jdbc:sqlite:${db.absolutePath}").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("PRAGMA foreign_keys = OFF")
                sqlStatements(sql.readText()).forEach { statement.executeUpdate(it) }
            }
        }
        return db
    }

    /** Splits a script on `;` at line ends, dropping `--` comment lines. */
    private fun sqlStatements(script: String): List<String> = script.lines()
        .filterNot { it.trimStart().startsWith("--") }
        .joinToString("\n")
        .split(Regex(""";\s*\n|;\s*$"""))
        .map { it.trim() }
        .filter { it.isNotEmpty() }
}

/** A minimal ustar/GNU tar reader: enough to pull one file out of the dump archive. */
internal class TarReader(private val input: InputStream) {
    class Entry(val name: String, val size: Long, val isFile: Boolean, private val input: InputStream) {
        /** Bytes of this entry not yet read; the reader skips them before the next header. */
        internal var remaining: Long = size
            private set

        fun copyTo(out: java.io.OutputStream) {
            val buffer = ByteArray(BUFFER)
            while (remaining > 0) {
                val read = input.read(buffer, 0, minOf(buffer.size.toLong(), remaining).toInt())
                if (read < 0) throw DumpException("Truncated tar entry $name")
                out.write(buffer, 0, read)
                remaining -= read
            }
        }
    }

    fun entries(): Sequence<Entry> = sequence {
        var longName: String? = null
        while (true) {
            val header = input.readNBytes(BLOCK)
            if (header.size < BLOCK || header.all { it == 0.toByte() }) return@sequence
            val size = octal(header, SIZE_OFFSET, SIZE_LENGTH)
            val type = header[TYPE_OFFSET].toInt().toChar()
            val name = longName ?: (prefix(header) + text(header, 0, NAME_LENGTH))
            longName = null
            if (type == GNU_LONG_NAME) {
                longName = String(input.readNBytes(size.toInt())).trimEnd('\u0000')
                skipPadding(size)
                continue
            }
            val entry = Entry(name, size, type == '0' || type == '\u0000', input)
            yield(entry)
            // Skip whatever the caller didn't read, plus padding to the next block.
            skipFully(entry.remaining)
            skipPadding(size)
        }
    }

    private fun prefix(header: ByteArray): String {
        val magic = text(header, MAGIC_OFFSET, MAGIC_LENGTH)
        val prefix = if (magic.startsWith("ustar")) text(header, PREFIX_OFFSET, PREFIX_LENGTH) else ""
        return if (prefix.isEmpty()) "" else "$prefix/"
    }

    private fun skipPadding(size: Long) = skipFully((BLOCK - size % BLOCK) % BLOCK)

    private fun skipFully(count: Long) {
        var left = count
        while (left > 0) {
            val skipped = input.skip(left)
            if (skipped <= 0) {
                if (input.read() < 0) return
                left--
            } else {
                left -= skipped
            }
        }
    }

    private fun text(header: ByteArray, offset: Int, length: Int): String =
        String(header, offset, length, Charsets.UTF_8).substringBefore('\u0000').trim()

    private fun octal(header: ByteArray, offset: Int, length: Int): Long =
        text(header, offset, length).trim().ifEmpty { "0" }.toLong(OCTAL)

    private companion object {
        const val BLOCK = 512
        const val BUFFER = 64 * 1024
        const val NAME_LENGTH = 100
        const val SIZE_OFFSET = 124
        const val SIZE_LENGTH = 12
        const val TYPE_OFFSET = 156
        const val MAGIC_OFFSET = 257
        const val MAGIC_LENGTH = 6
        const val PREFIX_OFFSET = 345
        const val PREFIX_LENGTH = 155
        const val OCTAL = 8
        const val GNU_LONG_NAME = 'L'
    }
}
