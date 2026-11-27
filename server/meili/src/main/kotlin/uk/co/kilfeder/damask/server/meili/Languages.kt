package uk.co.kilfeder.damask.server.meili

/**
 * Normalises language tags, both the catalogue's (messy: `en-GB`, `en_gb`, `EN`, `eng`,
 * `english`, `en_US`) and the app's requested `lang`, to lowercase `language[-region]`:
 * `en_GB` -> `en-gb`, `EN` -> `en`, `eng` -> `en`, `es-419` -> `es-419`.
 *
 * Shared by the importer (document fields) and the gateway (the `lang` parameter) so both sides
 * compare the same strings.
 */
object Languages {
    /** Three-letter codes and English names seen in the dump, mapped to their two-letter code. */
    private val ALIASES = mapOf(
        "eng" to "en", "english" to "en",
        "fre" to "fr", "fra" to "fr", "french" to "fr",
        "ger" to "de", "deu" to "de", "german" to "de",
        "spa" to "es", "spanish" to "es",
        "ita" to "it", "italian" to "it",
        "por" to "pt", "portuguese" to "pt",
        "dut" to "nl", "nld" to "nl", "dutch" to "nl",
    )
    private val REGION = Regex("^([a-z]{2}|[0-9]{3})$")
    private const val MAX_CODE_LENGTH = 3

    /** The normalised `language[-region]` tag, or "" when no language is given. */
    fun tag(raw: String): String {
        val parts = raw.trim().lowercase().replace('_', '-').split('-').filter { it.isNotEmpty() }
        val primary = parts.firstOrNull()?.filter { it in 'a'..'z' }.orEmpty()
        if (primary.isEmpty()) return ""
        // Unknown long words ("deutsch") stay as they are: they then match no `lang` filter,
        // rather than counting as "no language listed".
        val language = ALIASES[primary] ?: primary
        val region = parts.getOrNull(1)?.takeIf { language.length <= MAX_CODE_LENGTH && REGION.matches(it) }
        return if (region != null) "$language-$region" else language
    }

    /** The language part of a normalised tag: `en-gb` -> `en`. */
    fun prefix(tag: String): String = tag.substringBefore('-')
}
