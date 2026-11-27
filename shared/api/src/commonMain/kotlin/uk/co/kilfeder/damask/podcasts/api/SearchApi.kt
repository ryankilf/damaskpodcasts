package uk.co.kilfeder.damask.podcasts.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * The search gateway's `/v1` contract (spec › Search).
 *
 * Rules: within `/v1`, fields are only ever added, each with a default; nothing is removed or
 * renamed. Clients decode with [ApiJson] (`ignoreUnknownKeys = true`). A breaking change is a new
 * `/v2` alongside `/v1`. Golden-JSON tests in commonTest fail the build on an accidental break.
 */
object SearchApi {
    const val SEARCH_PATH = "/v1/search"
    const val HEALTH_PATH = "/health"

    // Query parameters of GET /v1/search.
    const val PARAM_QUERY = "q"
    const val PARAM_LANG = "lang"
    const val PARAM_HIDE_EXPLICIT = "hideExplicit"
    const val PARAM_LIMIT = "limit"
    const val PARAM_OFFSET = "offset"

    const val QUERY_MAX_LENGTH = 100
    const val DEFAULT_LIMIT = 20
    const val MAX_LIMIT = 20
    const val MAX_OFFSET = 40
}

/** The JSON settings both sides use. */
val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = true
}

/** `GET /v1/search` response. */
@Serializable
data class SearchResponse(
    val hits: List<ShowHit> = emptyList(),
    /** Meilisearch's estimate, capped by the index's `maxTotalHits` (100). */
    val estimatedTotal: Int = 0,
)

/**
 * One show from our index of the Podcast Index catalogue. Raw facts only: the client decides
 * labels such as "Ended" from [newestItemPubdate].
 */
@Serializable
data class ShowHit(
    val podcastIndexId: Long,
    /** `podcast:guid`; null when the catalogue has none. */
    val podcastGuid: String? = null,
    val title: String,
    val author: String = "",
    /** null when the catalogue has no artwork. */
    val imageUrl: String? = null,
    val feedUrl: String,
    /** As the feed declares it, e.g. `en`, `en-gb`; "" when unknown. */
    val language: String = "",
    /** The catalogue's `category1..10`, de-duplicated, in order. The first is the main one. */
    val categories: List<String> = emptyList(),
    /** `episodic`, `serial`, or whatever else the feed wrote; "" when absent. */
    val itunesType: String = "",
    val explicit: Boolean = false,
    val episodeCount: Int = 0,
    /** Epoch seconds; null when unknown. */
    val newestItemPubdate: Long? = null,
)

/** Body of every non-2xx response. Never contains a stack trace. */
@Serializable
data class ApiError(
    /** Stable machine-readable code: one of [ApiErrorCode]. */
    val error: String,
    /** Human-readable detail, safe to show in logs; never echoes the query. */
    val message: String = "",
)

object ApiErrorCode {
    const val BAD_REQUEST = "bad_request"
    const val UNAUTHORIZED = "unauthorized"
    const val RATE_LIMITED = "rate_limited"
    const val UNAVAILABLE = "unavailable"
    const val NOT_FOUND = "not_found"
    const val INTERNAL = "internal"
}

/**
 * `GET /health` response (no auth), for an external uptime monitor. HTTP 503 when [status] is
 * `down`, 200 otherwise.
 */
@Serializable
data class HealthResponse(
    /** [HealthStatus]: `down` if ![db], else `warning` if the certificate needs attention, else `ok`. */
    val status: String,
    /** Meilisearch is reachable and the `podcasts` index exists. */
    val db: Boolean,
    /** [CertStatus] of the gateway's own public TLS certificate. */
    val cert: String,
)

object HealthStatus {
    const val OK = "ok"
    const val WARNING = "warning"
    const val DOWN = "down"
}

object CertStatus {
    /** More than 14 days left. */
    const val OK = "ok"

    /** 14 days or fewer left. */
    const val WARNING = "warning"

    /** Expired, or the certificate couldn't be read. */
    const val EXPIRED = "expired"

    /** No public hostname configured (local compose): not checked, doesn't affect status. */
    const val UNKNOWN = "unknown"
}
