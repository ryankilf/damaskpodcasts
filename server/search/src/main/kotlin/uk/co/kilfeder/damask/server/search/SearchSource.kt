package uk.co.kilfeder.damask.server.search

import uk.co.kilfeder.damask.podcasts.api.SearchResponse

/** A validated `GET /v1/search` request. */
data class SearchRequest(
    val query: String,
    /** Lowercase primary language subtag, e.g. `en`; null = any language. */
    val langPrefix: String?,
    val hideExplicit: Boolean,
    val limit: Int,
    val offset: Int,
)

/** The backend failed or timed out. Maps to 503; the message never contains query text. */
class SourceUnavailableException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Where search results come from. Routes only ever talk to this, never to Meilisearch or
 * Podcast Index directly (enforced by Konsist).
 */
interface SearchSource {
    suspend fun search(request: SearchRequest): SearchResponse

    /** The backing store is reachable and has the live index (health's `db`). */
    suspend fun isHealthy(): Boolean
}
