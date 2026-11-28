package uk.co.kilfeder.damask.podcasts.presentation.episodes

/** A run of show-notes text with one style. [link] is set only for http, https and mailto links. */
data class NotesSpan(
    val text: String,
    val link: String? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
)

/** A paragraph, heading or list item of show notes. */
data class NotesBlock(val spans: List<NotesSpan>, val bullet: Boolean = false)

/**
 * Show notes as feeds write them (HTML, or plain text) turned into a few styled blocks: text,
 * bold, italic, links, paragraphs and bulleted items. Everything else is dropped: images, embeds,
 * scripts, styles and unknown tags (their text stays). Bare URLs in text become links.
 *
 * Built here rather than in the UI, so every platform shows the same notes. It's a lenient
 * tokenizer, not an HTML parser: feeds' markup is often broken, and nothing here is executed.
 */
object ShowNotes {
    /** Every distinct link in the notes, in order, labelled with its text and host. */
    fun links(notes: List<NotesBlock>): List<NotesLink> = notes
        .flatMap { it.spans }
        .mapNotNull { span -> span.link?.let { NotesLink(span.text.trim().ifEmpty { it }, it, host(it)) } }
        .distinctBy { it.url }

    private fun host(url: String): String = url
        .substringAfter("://", missingDelimiterValue = url.substringAfter(':'))
        .substringBefore('/')
        .substringBefore('?')
        .removePrefix("www.")

    private val TAG_OR_TEXT = Regex("<[^>]*>|[^<]+|<")
    private val TAG_NAME = Regex("^</?\\s*([a-zA-Z0-9]+)")
    private val HREF = Regex("""href\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""", RegexOption.IGNORE_CASE)
    private val DROPPED_WITH_CONTENT = Regex(
        "<(script|style|iframe|noscript)\\b[^>]*>.*?</\\1\\s*>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    private val LOOKS_LIKE_HTML = Regex(
        "<\\s*/?\\s*(p|br|a|div|ul|ol|li|b|strong|i|em|span|h[1-6])\\b",
        RegexOption.IGNORE_CASE,
    )
    private val WHITESPACE = Regex("[ \\t\\r\\n\\u00A0]+")
    private val BARE_URL = Regex("""https?://[^\s<>"]+[^\s<>".,;:!?)\]'’”]""")
    private val ENTITY = Regex("&(#[0-9]+|#[xX][0-9a-fA-F]+|[a-zA-Z]+);")
    private val NAMED_ENTITIES = mapOf(
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "nbsp" to " ",
        "ndash" to "–", "mdash" to "—", "hellip" to "…", "lsquo" to "‘",
        "rsquo" to "’", "ldquo" to "“", "rdquo" to "”", "copy" to "©",
        "pound" to "£", "euro" to "€", "bull" to "•", "middot" to "·",
    )
    private val OPENABLE = listOf("http://", "https://", "mailto:")
    private val BLOCK_TAGS = setOf("p", "div", "ul", "ol", "blockquote", "h1", "h2", "h3", "h4", "h5", "h6", "section")
    private val HEADINGS = setOf("h1", "h2", "h3", "h4", "h5", "h6")

    fun parse(notes: String?): List<NotesBlock> {
        val source = notes?.trim().orEmpty()
        if (source.isEmpty()) return emptyList()
        return if (LOOKS_LIKE_HTML.containsMatchIn(source)) fromHtml(source) else fromPlainText(source)
    }

    private fun fromPlainText(text: String): List<NotesBlock> = text
        .replace("\r\n", "\n")
        .split(Regex("\n\\s*\n"))
        .map { paragraph -> paragraph.lines().joinToString("\n") { it.trim() } }
        .filter { it.isNotBlank() }
        .map { NotesBlock(linkify(NotesSpan(decode(it)))) }

    private fun fromHtml(html: String): List<NotesBlock> {
        val builder = BlockBuilder()
        val cleaned = html.replace(COMMENT, "").replace(DROPPED_WITH_CONTENT, "")
        TAG_OR_TEXT.findAll(cleaned).forEach { match ->
            val token = match.value
            if (token.startsWith("<") && token.length > 1) builder.tag(token) else builder.text(decode(token))
        }
        return builder.finish()
    }

    /** Accumulates spans and splits them into blocks as tags open and close. */
    private class BlockBuilder {
        private val blocks = mutableListOf<NotesBlock>()
        private val spans = mutableListOf<NotesSpan>()
        private var bullet = false
        private var bold = 0
        private var italic = 0
        private var link: String? = null

        fun tag(token: String) {
            val name = TAG_NAME.find(token)?.groupValues?.get(1)?.lowercase() ?: return
            val closing = token.startsWith("</")
            when {
                name == "br" -> spans += NotesSpan("\n")
                name == "li" -> { endBlock(); bullet = !closing }
                name in HEADINGS -> { endBlock(); bold += if (closing) -1 else 1 }
                name in BLOCK_TAGS -> endBlock()
                name == "b" || name == "strong" -> bold += if (closing) -1 else 1
                name == "i" || name == "em" -> italic += if (closing) -1 else 1
                name == "a" -> link = if (closing) null else openable(href(token))
            }
            bold = bold.coerceAtLeast(0)
            italic = italic.coerceAtLeast(0)
        }

        fun text(raw: String) {
            val text = raw.replace(WHITESPACE, " ")
            if (text.isEmpty()) return
            val span = NotesSpan(text, link, bold > 0, italic > 0)
            spans += if (link == null) linkify(span) else trimLink(span)
        }

        fun finish(): List<NotesBlock> {
            endBlock()
            return blocks
        }

        private fun endBlock() {
            val trimmed = trim(spans)
            if (trimmed.isNotEmpty()) blocks += NotesBlock(trimmed, bullet)
            spans.clear()
        }

        private fun href(token: String): String? =
            HREF.find(token)?.groupValues?.drop(1)?.firstOrNull { it.isNotEmpty() }?.let(::decode)?.trim()
    }

    /** Drops leading and trailing whitespace and line breaks from a block's spans. */
    private fun trim(spans: List<NotesSpan>): List<NotesSpan> {
        val joined = spans.filter { it.text.isNotEmpty() }.toMutableList()
        while (joined.isNotEmpty() && joined.first().text.isBlank()) joined.removeAt(0)
        while (joined.isNotEmpty() && joined.last().text.isBlank()) joined.removeAt(joined.lastIndex)
        if (joined.isEmpty()) return joined
        joined[0] = joined[0].copy(text = joined[0].text.trimStart())
        joined[joined.lastIndex] = joined.last().copy(text = joined.last().text.trimEnd())
        return joined
    }

    /** Spaces at the edges of a link's text stay outside the link, so they aren't underlined. */
    private fun trimLink(span: NotesSpan): List<NotesSpan> {
        val core = span.text.trim()
        if (core.isEmpty()) return listOf(span.copy(link = null))
        val lead = span.text.substringBefore(core)
        val tail = span.text.substring(lead.length + core.length)
        return listOfNotNull(
            lead.takeIf { it.isNotEmpty() }?.let { span.copy(text = it, link = null) },
            span.copy(text = core),
            tail.takeIf { it.isNotEmpty() }?.let { span.copy(text = it, link = null) },
        )
    }

    private fun openable(url: String?): String? =
        url?.takeIf { candidate -> OPENABLE.any { candidate.startsWith(it, ignoreCase = true) } }

    /** Splits bare http(s) URLs out of [span] as links. */
    private fun linkify(span: NotesSpan): List<NotesSpan> {
        val matches = BARE_URL.findAll(span.text).toList()
        if (matches.isEmpty()) return listOf(span)
        val parts = mutableListOf<NotesSpan>()
        var at = 0
        matches.forEach { match ->
            if (match.range.first > at) parts += span.copy(text = span.text.substring(at, match.range.first))
            parts += span.copy(text = match.value, link = match.value)
            at = match.range.last + 1
        }
        if (at < span.text.length) parts += span.copy(text = span.text.substring(at))
        return parts
    }

    private fun decode(text: String): String = ENTITY.replace(text) { match ->
        val body = match.groupValues[1]
        when {
            body.startsWith("#x", ignoreCase = true) -> codePoint(body.drop(2).toIntOrNull(radix = 16))
            body.startsWith("#") -> codePoint(body.drop(1).toIntOrNull())
            else -> NAMED_ENTITIES[body]
        } ?: match.value
    }

    /** A Unicode code point as a string, as a UTF-16 surrogate pair above the BMP. */
    private fun codePoint(value: Int?): String? = when {
        value == null || value !in 1..MAX_CODE_POINT -> null
        value < SUPPLEMENTARY_START -> value.toChar().toString()
        else -> {
            val offset = value - SUPPLEMENTARY_START
            val high = HIGH_SURROGATE + (offset shr SURROGATE_BITS)
            val low = LOW_SURROGATE + (offset and LOW_BITS_MASK)
            charArrayOf(high.toChar(), low.toChar()).concatToString()
        }
    }

    private const val MAX_CODE_POINT = 0x10FFFF
    private const val SUPPLEMENTARY_START = 0x10000
    private const val HIGH_SURROGATE = 0xD800
    private const val LOW_SURROGATE = 0xDC00
    private const val SURROGATE_BITS = 10
    private const val LOW_BITS_MASK = 0x3FF
}
