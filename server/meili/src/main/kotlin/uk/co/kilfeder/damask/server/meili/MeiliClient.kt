package uk.co.kilfeder.damask.server.meili

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** A Meilisearch call failed. The message is safe to log: it never contains query text. */
class MeiliException(message: String, cause: Throwable? = null) : Exception(message, cause)

@Serializable
data class MeiliTask(val taskUid: Long)

@Serializable
data class MeiliTaskStatus(val uid: Long, val status: String, val type: String = "", val error: MeiliTaskError? = null)

@Serializable
data class MeiliTaskError(val message: String = "", val code: String = "")

@Serializable
data class MeiliSearchResult(
    val hits: List<PodcastDocument> = emptyList(),
    val estimatedTotalHits: Int = 0,
)

/** One ranking rule's contribution to a hit: relevance rules have a [score], custom rules a [value]. */
data class RuleScore(val score: Double?, val value: Double?)

data class ScoredHit(val document: PodcastDocument, val details: Map<String, RuleScore>)

data class ScoredSearchResult(val hits: List<ScoredHit>, val estimatedTotalHits: Int)

/** The search request the gateway sends. */
data class MeiliQuery(
    val text: String,
    val limit: Int,
    val offset: Int,
    val filters: List<String> = emptyList(),
)

/**
 * The read side of Meilisearch: health, search, index inspection and task polling. The gateway
 * only ever holds this (with the search-only key); writes live in [MeiliAdmin].
 */
class MeiliClient(internal val http: HttpClient) {

    suspend fun isHealthy(): Boolean = runCatching { http.get("/health").status.isSuccess() }.getOrDefault(false)

    suspend fun search(index: String, query: MeiliQuery): MeiliSearchResult {
        val response = http.post("/indexes/$index/search") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("q", query.text)
                    put("limit", query.limit)
                    put("offset", query.offset)
                    if (query.filters.isNotEmpty()) {
                        put("filter", JsonArray(query.filters.map(::JsonPrimitive)))
                    }
                },
            )
        }
        return response.expectSuccess("search").body()
    }

    /**
     * A search that also returns each hit's per-rule ranking details, so the gateway can merge two
     * result lists in Meilisearch's own rule order (federated search can't: its merge uses
     * `_rankingScore`, which ignores custom rules like `rankScore:desc`).
     */
    suspend fun searchScored(index: String, query: MeiliQuery): ScoredSearchResult {
        val response = http.post("/indexes/$index/search") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("q", query.text)
                    put("limit", query.limit)
                    put("offset", query.offset)
                    put("showRankingScoreDetails", true)
                    if (query.filters.isNotEmpty()) {
                        put("filter", JsonArray(query.filters.map(::JsonPrimitive)))
                    }
                },
            )
        }
        val raw: RawScoredResult = response.expectSuccess("search").body()
        return ScoredSearchResult(
            hits = raw.hits.map { hit ->
                val details = (hit["_rankingScoreDetails"] as? JsonObject).orEmpty().mapValues { (_, v) ->
                    val rule = v as? JsonObject
                    RuleScore(
                        score = (rule?.get("score") as? JsonPrimitive)?.content?.toDoubleOrNull(),
                        value = (rule?.get("value") as? JsonPrimitive)?.content?.toDoubleOrNull(),
                    )
                }
                ScoredHit(json.decodeFromJsonElement(PodcastDocument.serializer(), hit), details)
            },
            estimatedTotalHits = raw.estimatedTotalHits,
        )
    }

    @Serializable
    private data class RawScoredResult(val hits: List<JsonObject> = emptyList(), val estimatedTotalHits: Int = 0)

    suspend fun indexExists(index: String): Boolean = http.get("/indexes/$index").status.isSuccess()

    /** One stored document, as Meilisearch holds it (needs a key with documents.get). */
    suspend fun document(index: String, id: Long): JsonObject =
        http.get("/indexes/$index/documents/$id").expectSuccess("get document").body()

    suspend fun documentCount(index: String): Long {
        val stats: JsonObject = http.get("/indexes/$index/stats").expectSuccess("stats").body()
        return (stats["numberOfDocuments"] as? JsonPrimitive)?.content?.toLongOrNull()
            ?: throw MeiliException("stats without numberOfDocuments")
    }

    /** Polls a task until it finishes; throws unless it succeeded. */
    suspend fun awaitTask(task: MeiliTask, timeoutMs: Long = DEFAULT_TASK_TIMEOUT_MS, pollMs: Long = POLL_MS) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val status: MeiliTaskStatus = http.get("/tasks/${task.taskUid}").expectSuccess("task").body()
            when (status.status) {
                "succeeded" -> return
                "failed", "canceled" -> throw MeiliException(
                    "Task ${status.uid} (${status.type}) ${status.status}: " +
                        "${status.error?.code} ${status.error?.message}",
                )
            }
            delay(pollMs)
        }
        throw MeiliException("Task ${task.taskUid} timed out")
    }

    companion object {
        private const val DEFAULT_TASK_TIMEOUT_MS = 60 * 60 * 1000L
        private const val POLL_MS = 250L

        /**
         * `encodeDefaults` must be on: a field left at its default (`explicit = false`,
         * `popularityScore = 0`) would otherwise be missing from the uploaded document, and a
         * filter like `explicit = false` never matches a missing field. Found by the first real
         * import, where `hideExplicit=true` returned nothing.
         */
        val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = true }

        /** A CIO client for [baseUrl] authenticated with [apiKey]; [timeoutMs] bounds every request. */
        fun create(baseUrl: String, apiKey: String?, timeoutMs: Long): MeiliClient = MeiliClient(
            HttpClient(CIO) {
                expectSuccess = false
                install(HttpTimeout) {
                    requestTimeoutMillis = timeoutMs
                    connectTimeoutMillis = timeoutMs
                }
                install(ContentNegotiation) { json(json) }
                defaultRequest {
                    url(baseUrl.trimEnd('/') + "/")
                    apiKey?.takeIf { it.isNotBlank() }?.let { bearerAuth(it) }
                }
            },
        )
    }
}

private const val ERROR_BODY_CHARS = 300

internal suspend fun HttpResponse.expectSuccess(what: String): HttpResponse {
    if (!status.isSuccess()) {
        throw MeiliException("Meilisearch $what failed: HTTP ${status.value} ${bodyAsText().take(ERROR_BODY_CHARS)}")
    }
    return this
}
