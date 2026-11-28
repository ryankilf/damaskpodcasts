package uk.co.kilfeder.damask.podcasts.core.feed

import nl.adaptivity.xmlutil.EventType
import nl.adaptivity.xmlutil.core.KtXmlReader

/** A minimal immutable XML tree: enough for RSS and OPML, built with a lenient parser. */
internal class XmlNode(
    val name: String,
    val namespace: String,
    val prefix: String,
    val attributes: Map<String, String>,
    val children: List<XmlNode>,
    val text: String,
) {
    fun child(name: String, ns: Namespace = Namespace.NONE): XmlNode? = children.firstOrNull { it.matches(name, ns) }

    fun children(name: String, ns: Namespace = Namespace.NONE): List<XmlNode> = children.filter { it.matches(name, ns) }

    fun childText(name: String, ns: Namespace = Namespace.NONE): String? =
        child(name, ns)?.text?.trim()?.takeIf { it.isNotEmpty() }

    fun attr(name: String): String? = attributes[name]?.trim()?.takeIf { it.isNotEmpty() }

    fun descendants(): Sequence<XmlNode> = sequence {
        for (child in children) {
            yield(child)
            yieldAll(child.descendants())
        }
    }

    private fun matches(name: String, ns: Namespace): Boolean = this.name == name && ns.matches(this)
}

/**
 * Namespaces matched leniently: feeds in the wild use odd capitalisation of the iTunes URI, or
 * the `itunes:` prefix without declaring it.
 */
internal enum class Namespace(private val uri: String, private val prefix: String) {
    NONE("", ""),
    ITUNES("http://www.itunes.com/dtds/podcast-1.0.dtd", "itunes"),
    CONTENT("http://purl.org/rss/1.0/modules/content/", "content"),
    ;

    fun matches(node: XmlNode): Boolean =
        if (this == NONE) node.namespace.isEmpty() && node.prefix.isEmpty()
        else node.namespace.equals(uri, ignoreCase = true) || node.prefix.equals(prefix, ignoreCase = true)
}

internal class XmlParseException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal object Xml {
    private const val BYTE_ORDER_MARK = "\uFEFF"

    /** The HTML entities that turn up in podcast feeds. Unknown ones are dropped. */
    private val HTML_ENTITIES = mapOf(
        "nbsp" to "\u00A0", "ndash" to "\u2013", "mdash" to "\u2014", "hellip" to "\u2026",
        "lsquo" to "\u2018", "rsquo" to "\u2019", "ldquo" to "\u201C", "rdquo" to "\u201D",
        "laquo" to "\u00AB", "raquo" to "\u00BB", "copy" to "\u00A9", "reg" to "\u00AE",
        "trade" to "\u2122", "eacute" to "\u00E9", "egrave" to "\u00E8", "agrave" to "\u00E0",
        "uuml" to "\u00FC", "ouml" to "\u00F6", "auml" to "\u00E4", "ccedil" to "\u00E7",
        "pound" to "\u00A3", "euro" to "\u20AC", "bull" to "\u2022", "middot" to "\u00B7",
        "amp" to "&", "lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'",
    )

    /** Parses [source] in relaxed mode (unknown entities like `&nbsp;` and sloppy markup tolerated). */
    fun parse(source: String): XmlNode {
        val reader = try {
            KtXmlReader(source.removePrefix(BYTE_ORDER_MARK), expandEntities = false, relaxed = true)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw XmlParseException("Unreadable XML", e)
        }
        return try {
            TreeBuilder().build(reader)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            throw XmlParseException("Malformed XML", e)
        }
    }

    private class Builder(
        val name: String,
        val namespace: String,
        val prefix: String,
        val attributes: Map<String, String>,
    ) {
        val children = mutableListOf<XmlNode>()
        val text = StringBuilder()

        fun build() = XmlNode(name, namespace, prefix, attributes, children, text.toString())
    }

    private class TreeBuilder {
        private val stack = ArrayDeque<Builder>()
        private var root: XmlNode? = null

        fun build(reader: KtXmlReader): XmlNode {
            while (reader.hasNext()) {
                when (reader.next()) {
                    EventType.START_ELEMENT -> stack.addLast(reader.toBuilder())
                    EventType.END_ELEMENT -> close()
                    EventType.TEXT, EventType.CDSECT, EventType.IGNORABLE_WHITESPACE ->
                        stack.lastOrNull()?.text?.append(reader.text)
                    EventType.ENTITY_REF -> stack.lastOrNull()?.text?.append(entityText(reader))
                    else -> Unit
                }
            }
            return root ?: throw XmlParseException("No root element")
        }

        private fun close() {
            val node = stack.removeLast().build()
            val parent = stack.lastOrNull()
            if (parent == null) root = node else parent.children += node
        }

        /**
         * Character references (`&#8217;`) and XML's own entities come resolved. Feeds also use HTML
         * entities, which XML doesn't define. Character references have no entity name, and the
         * reader throws if asked for one, hence the `runCatching`.
         */
        private fun entityText(reader: KtXmlReader): String {
            val text = reader.text
            val name = runCatching { reader.localName }.getOrNull()
            return when {
                name == null -> text
                text.isNotEmpty() && text != "&$name;" -> text
                else -> HTML_ENTITIES[name].orEmpty()
            }
        }

        private fun KtXmlReader.toBuilder(): Builder {
            val attributes = (0 until attributeCount).associate { getAttributeLocalName(it) to getAttributeValue(it) }
            return Builder(localName, namespaceURI, prefix, attributes)
        }
    }
}
