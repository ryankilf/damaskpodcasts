package uk.co.kilfeder.damask.podcasts.core.model

import kotlin.jvm.JvmInline

/** UUIDv5 of the feed URL at first subscription; fixed for life even if the URL changes. */
@JvmInline
value class ShowId(val value: String)

@JvmInline
value class CategoryId(val value: String)

/** Episodes are keyed by `(show_id, guid)`; there is no derived episode ID. */
data class EpisodeKey(val showId: ShowId, val guid: String)
