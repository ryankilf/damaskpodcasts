package uk.co.kilfeder.damask.podcasts.core.feed

/** One `itunes:category`, as written in the feed. [subcategory] is "" when there isn't one. */
data class FeedCategory(val category: String, val subcategory: String = "")

data class FeedEpisode(
    val guid: String,
    val title: String,
    /** null when the item has no parseable date. */
    val publishedAt: Long?,
    val enclosureUrl: String,
    val imageUrl: String?,
    val durationMs: Long?,
    /**
     * Show notes as the feed wrote them (HTML or plain text): `content:encoded`, else
     * `description`, else `itunes:summary`. Capped at [FeedParser.MAX_NOTES_CHARS].
     */
    val notesHtml: String? = null,
)

/** A parsed RSS podcast feed: just what the app stores (spec › Data model, local-only tables). */
data class FeedDocument(
    val title: String,
    /** Channel `itunes:image href`, falling back to RSS `<image><url>`. */
    val imageUrl: String?,
    /** Channel `itunes:author`: who makes it. */
    val author: String? = null,
    /** Channel `description`, else `itunes:summary` (HTML or plain text). Capped like notes. */
    val description: String? = null,
    /** `itunes:type`, as written (`episodic` / `serial`). */
    val itunesType: String?,
    /** Every `itunes:category` in feed order; only the first drives the import prefill. */
    val categories: List<FeedCategory>,
    /** `<language>`: the show's spoken language. Plays no part in rotation. */
    val language: String?,
    /** `itunes:new-feed-url`: the host has moved the feed. */
    val newFeedUrl: String?,
    val episodes: List<FeedEpisode>,
)
