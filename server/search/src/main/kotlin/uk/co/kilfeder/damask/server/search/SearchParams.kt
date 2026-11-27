package uk.co.kilfeder.damask.server.search

import io.ktor.http.Parameters
import uk.co.kilfeder.damask.podcasts.api.SearchApi
import uk.co.kilfeder.damask.podcasts.api.SearchCategories
import uk.co.kilfeder.damask.server.meili.Languages

/** A 400. The message names the parameter and the rule, never the submitted value. */
class BadRequestException(message: String) : Exception(message)

/**
 * Validates `GET /v1/search` parameters (spec › Search › Endpoints). Anything missing, malformed
 * or out of range is a [BadRequestException]. Unknown parameters are ignored.
 */
object SearchParams {
    private val LANG = Regex(SearchApi.LANG_PATTERN)

    fun parse(params: Parameters): SearchRequest {
        val query = params[SearchApi.PARAM_QUERY]?.trim()
            ?: throw BadRequestException("q is required")
        if (query.isEmpty() || query.length > SearchApi.QUERY_MAX_LENGTH) {
            throw BadRequestException("q must be 1-${SearchApi.QUERY_MAX_LENGTH} characters")
        }
        val tag = params[SearchApi.PARAM_LANG]?.let(::lang)
        return SearchRequest(
            query = query,
            langPrefix = tag?.let(Languages::prefix),
            hideExplicit = params[SearchApi.PARAM_HIDE_EXPLICIT]?.let(::bool) ?: false,
            limit = int(params, SearchApi.PARAM_LIMIT, SearchApi.DEFAULT_LIMIT, 1..SearchApi.MAX_LIMIT),
            offset = int(params, SearchApi.PARAM_OFFSET, 0, 0..SearchApi.MAX_OFFSET),
            langTag = tag?.takeIf { '-' in it },
            categories = categories(params.getAll(SearchApi.PARAM_CATEGORY).orEmpty()),
        )
    }

    /** `en`, `en-GB`, `es-419`, any case: normalised to `en`, `en-gb`, `es-419`. */
    private fun lang(raw: String): String {
        if (!LANG.matches(raw)) {
            throw BadRequestException("lang must be a language code, optionally with a region (en, en-GB)")
        }
        return Languages.tag(raw)
    }

    private fun categories(raw: List<String>): List<String> {
        if (raw.any { it !in SearchCategories.KEYS }) {
            throw BadRequestException("category must be one of ${SearchCategories.KEYS.joinToString(",")}")
        }
        return raw.distinct()
    }

    private fun bool(raw: String): Boolean = when (raw) {
        "true" -> true
        "false" -> false
        else -> throw BadRequestException("hideExplicit must be true or false")
    }

    private fun int(params: Parameters, name: String, default: Int, range: IntRange): Int {
        val raw = params[name] ?: return default
        val value = raw.toIntOrNull() ?: throw BadRequestException("$name must be an integer")
        if (value !in range) throw BadRequestException("$name must be ${range.first}-${range.last}")
        return value
    }
}
