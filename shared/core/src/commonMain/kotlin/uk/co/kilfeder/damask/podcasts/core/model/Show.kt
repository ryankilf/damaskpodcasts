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

/**
 * A genre's order. [VARIETY] is the rotation: in-progress first, then one episode per show, shows
 * least recently played first. The others list every unplayed episode in the genre by date.
 */
enum class GenreOrder { VARIETY, NEWEST_FIRST, OLDEST_FIRST }

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

    /**
     * Serial shows, and shows listed oldest first, keep every episode the feed has (oldest first
     * means nothing without the old ones); others keep the latest [keepCount].
     */
    val keepMode: KeepMode
        get() {
            val keepAll = type == ShowType.SERIAL || episodeOrder == EpisodeOrder.OLDEST_FIRST
            return keepModeOverride ?: if (keepAll) KeepMode.ALL else KeepMode.LATEST
        }

    companion object {
        const val DEFAULT_KEEP_COUNT = 10
    }
}

data class Category(
    val id: CategoryId,
    val name: String,
    /** The built-in Uncategorised category: can't be renamed or deleted. */
    val isSystem: Boolean = false,
    val order: GenreOrder = GenreOrder.VARIETY,
) {
    companion object {
        /** Fixed so every device seeds the same row (see Category.sq). */
        val UNCATEGORISED_ID = CategoryId("uncategorised")
    }
}

/** Newest first; ties broken by GUID so the order is stable. */
val newestFirst: Comparator<Episode> = compareByDescending<Episode> { it.publishedAt }.thenBy { it.key.guid }

/** Oldest first; ties broken by GUID. */
val oldestFirst: Comparator<Episode> = compareBy<Episode> { it.publishedAt }.thenBy { it.key.guid }

/** Orders a show's episodes in its own order (newest- or oldest-first). */
fun Show.episodeComparator(): Comparator<Episode> = when (episodeOrder) {
    EpisodeOrder.NEWEST_FIRST -> newestFirst
    EpisodeOrder.OLDEST_FIRST -> oldestFirst
}
