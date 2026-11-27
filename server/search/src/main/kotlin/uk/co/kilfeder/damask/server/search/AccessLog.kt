// The file is named after the AccessLog plugin value, the thing callers install.
@file:Suppress("MatchingDeclarationName")

package uk.co.kilfeder.damask.server.search

import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.ResponseSent
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.principal
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.util.AttributeKey
import org.slf4j.LoggerFactory
import java.security.MessageDigest

/**
 * The gateway's only request log (spec › Search › Limits): method, path, status, latency and a
 * short hash of the caller's `sub`. Never the query string, query text, headers or the raw `sub`.
 * Ktor's CallLogging is deliberately not installed: it logs the full URI, query included.
 */
class AccessLogConfig {
    /** Where lines go; tests replace it. */
    var sink: (String) -> Unit = { LoggerFactory.getLogger("access").info(it) }
}

private val StartedAt = AttributeKey<Long>("damask.startedAt")

val AccessLog = createApplicationPlugin("AccessLog", ::AccessLogConfig) {
    val sink = pluginConfig.sink
    onCall { call -> call.attributes.put(StartedAt, System.nanoTime()) }
    on(ResponseSent) { call ->
        // A call can send twice (an auth challenge, then StatusPages' JSON body): log it once.
        val started = call.attributes.getOrNull(StartedAt) ?: return@on
        call.attributes.remove(StartedAt)
        val latencyMs = (System.nanoTime() - started) / NANOS_PER_MS
        val status = call.response.status()?.value ?: 0
        // path() is the path only: no query string.
        val method = call.request.httpMethod.value
        sink("$method ${call.request.path()} $status ${latencyMs}ms caller=${callerHash(call)}")
    }
}

private fun callerHash(call: io.ktor.server.application.ApplicationCall): String {
    val jwtSub = call.principal<JWTPrincipal>()?.subject
    val subject = jwtSub ?: call.principal<PrivateCaller>()?.let { PrivateCaller.KEY } ?: return "-"
    return shortHash(subject)
}

/** First 12 hex chars of SHA-256: enough to follow one caller through the logs, not to recover it. */
fun shortHash(value: String): String =
    MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        .take(HASH_BYTES)
        .joinToString("") { "%02x".format(it) }

private const val NANOS_PER_MS = 1_000_000L
private const val HASH_BYTES = 6
