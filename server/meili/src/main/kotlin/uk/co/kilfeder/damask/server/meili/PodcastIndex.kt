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
    /** Normalised language (`en_GB` and `EN` -> `en`, `eng` -> `en`), for the `lang` filter. */
    val languagePrefix: String = "",
    /** Normalised `language[-region]` (`en_GB` -> `en-gb`), for the region boost. "" if none. */
    val languageTag: String = "",
    val explicit: Boolean = false,
    val itunesType: String = "",
    val episodeCount: Int = 0,
    /** Epoch seconds. */
    val newestItemPubdate: Long? = null,
    /** Epoch seconds of the oldest episode still in the feed: how long the feed has run. */
    val oldestItemPubdate: Long? = null,
    /** 0-9 in the real dump (the brief said 0-5). */
    val popularityScore: Int = 0,
    /** The dump has an Apple Podcasts ID (`itunesId`) for this feed. Never looked up with Apple. */
    val appleListed: Boolean = false,
    /** category1..10, non-empty, de-duplicated, in order. */
    val categories: List<String> = emptyList(),
    /** [categories] mapped to Apple's top-level keys (`news`, `tvandfilm`, ...), for the `category` filter. */
    val topCategories: List<String> = emptyList(),
    /** Popularity weighted by recency, Apple listing and feed age (importer RankScore). Ranked on. */
    val rankScore: Double = 0.0,
    /** When [rankScore] was computed (epoch seconds), so it can be recomputed without a re-import. */
    val scoredAt: Long = 0,
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
    val FILTERABLE = listOf("languagePrefix", "languageTag", "explicit", "categories", "topCategories")

    /**
     * `rankScore` (popularity weighted by recency) comes right after `words`, `typo` and
     * `proximity`, ahead of `attribute` and `exactness`, so a popular, active show beats a tiny
     * feed with the exact same title. It stays after `proximity`: ahead of it, loosely related
     * popular shows ("The History of Punk Rock" for "hardcore history") pushed into the top
     * results. Measured on the 21 Nov 2026 dump; see docs/decisions.md › Server.
     */
    val RANKING_RULES = listOf(
        "words", "typo", "proximity", "rankScore:desc", "attribute", "sort", "exactness",
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

    /** `en-GB` -> `en`, `eng` -> `en`; "" when no language is given. See [Languages]. */
    fun languagePrefix(language: String): String = Languages.prefix(Languages.tag(language))
}
