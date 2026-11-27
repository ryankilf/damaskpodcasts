package uk.co.kilfeder.damask.server.meili

import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class MeiliKey(val uid: String, val key: String, val actions: List<String> = emptyList())

/** The write side of Meilisearch, used only by the importer (with the master key). */
class MeiliAdmin(private val client: MeiliClient) {
    private val http get() = client.http

    suspend fun createIndex(index: String): MeiliTask = http.post("/indexes") {
        contentType(ContentType.Application.Json)
        setBody(buildJsonObject { put("uid", index); put("primaryKey", PodcastIndex.PRIMARY_KEY) })
    }.expectSuccess("create index").body()

    suspend fun deleteIndex(index: String): MeiliTask =
        http.delete("/indexes/$index").expectSuccess("delete index").body()

    suspend fun updateSettings(index: String, settings: JsonObject): MeiliTask =
        http.patch("/indexes/$index/settings") {
            contentType(ContentType.Application.Json)
            setBody(settings)
        }.expectSuccess("settings").body()

    /** Uploads newline-delimited JSON documents. */
    suspend fun addDocumentsNdjson(index: String, ndjson: String): MeiliTask =
        http.post("/indexes/$index/documents?primaryKey=${PodcastIndex.PRIMARY_KEY}") {
            contentType(NDJSON)
            setBody(ndjson)
        }.expectSuccess("add documents").body()

    suspend fun swapIndexes(a: String, b: String): MeiliTask = http.post("/swap-indexes") {
        contentType(ContentType.Application.Json)
        val pair = buildJsonArray { add(JsonPrimitive(a)); add(JsonPrimitive(b)) }
        setBody(buildJsonArray { add(buildJsonObject { put("indexes", pair) }) })
    }.expectSuccess("swap indexes").body()

    /**
     * Creates (or finds) a non-expiring API key with a fixed [uid], restricted to [actions] on
     * [indexes]. Meilisearch derives the key's value as HMAC-SHA256(master key, uid).
     */
    suspend fun ensureKey(uid: String, description: String, actions: List<String>, indexes: List<String>): MeiliKey {
        val existing = http.get("/keys/$uid")
        if (existing.status.isSuccess()) return existing.body()
        return http.post("/keys") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("uid", uid)
                    put("description", description)
                    put("actions", JsonArray(actions.map(::JsonPrimitive)))
                    put("indexes", JsonArray(indexes.map(::JsonPrimitive)))
                    put("expiresAt", JsonNull)
                },
            )
        }.expectSuccess("create key").body()
    }

    private companion object {
        val NDJSON = ContentType("application", "x-ndjson")
    }
}
