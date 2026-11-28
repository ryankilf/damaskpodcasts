package uk.co.kilfeder.damask.podcasts.core.directory

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import uk.co.kilfeder.damask.podcasts.core.feed.HttpClients
import uk.co.kilfeder.damask.podcasts.core.feed.describeNetworkFailure
import uk.co.kilfeder.damask.podcasts.core.sync.FeedUrl

/** A show in Apple's directory: only what Discover shows and what subscribing needs. */
data class DirectoryShow(
    val appleId: Long,
    val title: String,
    val author: String,
    val artworkUrl: String?,
    val feedUrl: String,
    val explicit: Boolean,
)

sealed interface DirectoryResult {
    /** In Apple's order; can be empty. */
    data class Found(val shows: List<DirectoryShow>) : DirectoryResult

    /** Apple refused (403 or 429): its rate limit, about 20 searches a minute. */
    data object Busy : DirectoryResult

    data class Failed(val reason: String) : DirectoryResult
}

/**
 * Searches Apple's podcast directory from the device (spec › Search): no key, no account, nothing
 * of ours in between. Results without a feed URL are dropped (they can't be subscribed to), and
 * one feed listed twice appears once. Nothing from here is stored.
 */
class AppleDirectory(private val client: HttpClient) {
    suspend fun search(term: String, country: String?, hideExplicit: Boolean): DirectoryResult = try {
        val response = client.get(SEARCH_URL) {
            parameter("media", "podcast")
            parameter("entity", "podcast")
            parameter("term", term.trim())
            parameter("limit", LIMIT)
            country?.takeIf { it.length == COUNTRY_CODE_LENGTH }?.let { parameter("country", it.lowercase()) }
            if (hideExplicit) parameter("explicit", "No")
        }
        when {
            response.status.value in REFUSED -> DirectoryResult.Busy
            !response.status.isSuccess() -> DirectoryResult.Failed("HTTP ${response.status.value}")
            else -> DirectoryResult.Found(parse(response.bodyAsText()).filter { !hideExplicit || !it.explicit })
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: SerializationException) {
        DirectoryResult.Failed(e::class.simpleName ?: "Unreadable reply")
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        DirectoryResult.Failed(describeNetworkFailure(e))
    }

    companion object {
        private const val SEARCH_URL = "https://itunes.apple.com/search"
        private const val LIMIT = 50
        private const val COUNTRY_CODE_LENGTH = 2
        private const val FORBIDDEN = 403
        private const val TOO_MANY_REQUESTS = 429
        private val REFUSED = setOf(FORBIDDEN, TOO_MANY_REQUESTS)

        fun create(): AppleDirectory = AppleDirectory(HttpClients.directory)

        /** The `results` of a Search API reply, keeping only podcasts with a feed URL, each feed once. */
        internal fun parse(json: String): List<DirectoryShow> =
            Json.parseToJsonElement(json).jsonObject["results"]?.jsonArray.orEmpty()
                .mapNotNull { (it as? JsonObject)?.toShow() }
                .distinctBy { FeedUrl.normalise(it.feedUrl) }

        private fun JsonObject.toShow(): DirectoryShow? {
            val feedUrl = text("feedUrl") ?: return null
            return DirectoryShow(
                appleId = this["collectionId"]?.jsonPrimitive?.longOrNull ?: 0,
                title = text("collectionName") ?: text("trackName").orEmpty(),
                author = text("artistName").orEmpty(),
                artworkUrl = text("artworkUrl600") ?: text("artworkUrl100"),
                feedUrl = feedUrl,
                explicit = text("collectionExplicitness") == "explicit",
            )
        }

        private fun JsonObject.text(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
    }
}
