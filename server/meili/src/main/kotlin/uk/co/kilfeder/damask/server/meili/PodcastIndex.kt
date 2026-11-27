package uk.co.kilfeder.damask.server.meili

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * One document in the `podcasts` index: everything search needs, nothing else (spec › Search ›
 * Weekly import). Written by server/importer, read by server/search.
 */
@Serializable
data class PodcastDocument(
    val id: Long,
    val title: String,
    val author: String = "",
    val ownerName: String = "",
    val imageUrl: String? = null,
    val feedUrl: String,
    val podcastGuid: String? = null,
    /** As the feed declares it, for display. */
    val language: String = "",
    /** Lowercased primary language subtag (`en-GB` -> `en`), for the `lang` filter. */
    val languagePrefix: String = "",
    val explicit: Boolean = false,
    val itunesType: String = "",
    val episodeCount: Int = 0,
    /** Epoch seconds. */
    val newestItemPubdate: Long? = null,
    /** 0-9 in the real dump (the brief said 0-5). */
    val popularityScore: Int = 0,
    /** category1..10, non-empty, de-duplicated, in order. */
    val categories: List<String> = emptyList(),
    /** podcastGuid, else chash, else "id:<id>": collapses one show listed under several feeds. */
    val dedupeKey: String,
)

/** The live index name and its settings. */
object PodcastIndex {
    /** The alias-like live index; each import builds `podcasts_<yyyyMMdd>` and swaps it in. */
    const val LIVE = "podcasts"
    const val PRIMARY_KEY = "id"
    const val MAX_TOTAL_HITS = 100

    val SEARCHABLE = listOf("title", "author", "ownerName")
    val FILTERABLE = listOf("languagePrefix", "explicit", "categories")
    val RANKING_RULES = listOf(
        "words", "typo", "proximity", "attribute", "sort", "exactness",
        "popularityScore:desc", "newestItemPubdate:desc",
    )
    const val DISTINCT = "dedupeKey"

    /** Body for `PATCH /indexes/{uid}/settings`. */
    val settings: JsonObject = buildJsonObject {
        put("searchableAttributes", JsonArray(SEARCHABLE.map(::JsonPrimitive)))
        put("filterableAttributes", JsonArray(FILTERABLE.map(::JsonPrimitive)))
        put("rankingRules", JsonArray(RANKING_RULES.map(::JsonPrimitive)))
        put("distinctAttribute", DISTINCT)
        putJsonObject("pagination") { put("maxTotalHits", MAX_TOTAL_HITS) }
    }

    /** `en-GB` -> `en`; anything not starting with letters -> "". */
    fun languagePrefix(language: String): String =
        language.trim().lowercase().takeWhile { it in 'a'..'z' }
}
