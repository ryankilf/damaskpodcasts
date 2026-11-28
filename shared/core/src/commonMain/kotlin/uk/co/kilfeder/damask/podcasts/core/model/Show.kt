package uk.co.kilfeder.damask.podcasts.core.model

/** From the feed's `itunes:type`. Drives the show's default order and retention. */
enum class ShowType {
    EPISODIC,
    SERIAL,
    ;

    companion object {
        /** Anything other than `serial` (including missing) is episodic, the feed default. */
        fun fromItunesType(raw: String?): ShowType =
            if (raw?.trim().equals("serial", ignoreCase = true)) SERIAL else EPISODIC
    }
}

enum class EpisodeOrder { NEWEST_FIRST, OLDEST_FIRST }

enum class KeepMode { LATEST, ALL }

data class Show(
    val id: ShowId,
    val categoryId: CategoryId,
    val type: ShowType = ShowType.EPISODIC,
    /** `episode_order`; null = automatic from [type]. */
    val episodeOrderOverride: EpisodeOrder? = null,
    /** `keep_mode`; null = automatic from [type]. */
    val keepModeOverride: KeepMode? = null,
    /** Used when [keepMode] is [KeepMode.LATEST]. */
    val keepCount: Int = DEFAULT_KEEP_COUNT,
    /** false = unsubscribed (tombstone). */
    val subscribed: Boolean = true,
    /** Display only, from the feed cache; empty until the feed has been fetched. */
    val title: String = "",
    val imageUrl: String? = null,
    /** Display only, from the feed cache: `itunes:author`. */
    val author: String = "",
    /** Display only, from the feed cache: the channel description, as the feed wrote it. */
    val description: String = "",
    /** The current feed URL (a secret for private feeds: shown only when asked for). */
    val feedUrl: String = "",
) {
    /** Serial shows play oldest-first; everything else newest-first. */
    val episodeOrder: EpisodeOrder
        get() = episodeOrderOverride
            ?: if (type == ShowType.SERIAL) EpisodeOrder.OLDEST_FIRST else EpisodeOrder.NEWEST_FIRST

    /** Serial shows keep every episode; episodic shows keep the latest [keepCount]. */
    val keepMode: KeepMode
        get() = keepModeOverride ?: if (type == ShowType.SERIAL) KeepMode.ALL else KeepMode.LATEST

    companion object {
        const val DEFAULT_KEEP_COUNT = 10
    }
}

data class Category(
    val id: CategoryId,
    val name: String,
    /** The built-in Uncategorised category: can't be renamed or deleted. */
    val isSystem: Boolean = false,
) {
    companion object {
        /** Fixed so every device seeds the same row (see Category.sq). */
        val UNCATEGORISED_ID = CategoryId("uncategorised")
    }
}

/** Orders a show's episodes in its own order (newest- or oldest-first); ties broken by GUID. */
fun Show.episodeComparator(): Comparator<Episode> = when (episodeOrder) {
    EpisodeOrder.NEWEST_FIRST -> compareByDescending<Episode> { it.publishedAt }.thenBy { it.key.guid }
    EpisodeOrder.OLDEST_FIRST -> compareBy<Episode> { it.publishedAt }.thenBy { it.key.guid }
}
