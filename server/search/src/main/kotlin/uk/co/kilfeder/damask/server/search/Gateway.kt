package uk.co.kilfeder.damask.server.search

import com.auth0.jwk.JwkProvider
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.auth.AuthenticationStrategy
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.bearer
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.auth.principal
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.slf4j.LoggerFactory
import uk.co.kilfeder.damask.podcasts.api.ApiError
import uk.co.kilfeder.damask.podcasts.api.ApiErrorCode
import uk.co.kilfeder.damask.podcasts.api.ApiJson
import uk.co.kilfeder.damask.podcasts.api.CertStatus
import uk.co.kilfeder.damask.podcasts.api.HealthStatus
import uk.co.kilfeder.damask.podcasts.api.SearchApi

/** Everything the gateway needs, so tests can swap in fakes. */
class GatewayDeps(
    val source: SearchSource,
    /** null disables Supabase auth (local dev with only the private token). */
    val supabase: SupabaseAuth?,
    /** null disables the private-build token. */
    val privateToken: String?,
    /** null when there's no public hostname (local compose): cert is reported `unknown`. */
    val certChecker: CertChecker? = null,
    val searchBurst: Int = SEARCH_BURST,
    val searchRefillMs: Long = SEARCH_REFILL_MS,
    val nowMs: () -> Long = System::currentTimeMillis,
    val accessLogSink: ((String) -> Unit)? = null,
) {
    companion object {
        const val SEARCH_BURST = 20
        const val SEARCH_REFILL_MS = 1_000L
    }
}

/** Supabase JWT settings: keys from [jwks]; `iss` must be `$url/auth/v1`, `aud` `authenticated`. */
class SupabaseAuth(val url: String, val jwks: JwkProvider) {
    val issuer: String get() = url.trimEnd('/') + "/auth/v1"

    companion object {
        const val AUDIENCE = "authenticated"
    }
}

private val log = LoggerFactory.getLogger("gateway")
private val SEARCH_LIMIT = RateLimitName("search")
private const val AUTH_SUPABASE = "supabase"
private const val AUTH_PRIVATE = "private"
private const val LEEWAY_SECONDS = 30L
private val UNAUTHORIZED_BODY = ApiError(ApiErrorCode.UNAUTHORIZED, "Missing or invalid token")
private val UNAVAILABLE_BODY = ApiError(ApiErrorCode.UNAVAILABLE, "Search is temporarily unavailable")

fun Application.searchGateway(deps: GatewayDeps) {
    install(AccessLog) { deps.accessLogSink?.let { sink = it } }
    install(ContentNegotiation) { json(ApiJson) }
    installErrors()
    installAuth(deps)
    install(RateLimit) {
        register(SEARCH_LIMIT) {
            rateLimiter { _, _ -> TokenBucket(deps.searchBurst, deps.searchRefillMs, deps.nowMs) }
            requestKey { call -> callerKey(call) }
        }
    }

    routing {
        get(SearchApi.HEALTH_PATH) {
            val health = healthOf(
                db = deps.source.isHealthy(),
                cert = deps.certChecker?.status() ?: CertStatus.UNKNOWN,
            )
            val code = if (health.status == HealthStatus.DOWN) HttpStatusCode.ServiceUnavailable else HttpStatusCode.OK
            call.respond(code, health)
        }
        val providers = listOfNotNull(
            AUTH_SUPABASE.takeIf { deps.supabase != null },
            AUTH_PRIVATE.takeIf { deps.privateToken != null },
        )
        authenticate(*providers.toTypedArray(), strategy = AuthenticationStrategy.FirstSuccessful) {
            rateLimit(SEARCH_LIMIT) {
                get(SearchApi.SEARCH_PATH) {
                    val request = SearchParams.parse(call.request.queryParameters)
                    call.respond(deps.source.search(request))
                }
            }
        }
    }
}

/** The rate-limit key: the token's `sub`, or "private" for the private build's token. */
private fun callerKey(call: ApplicationCall): Any =
    call.principal<JWTPrincipal>()?.subject ?: call.principal<PrivateCaller>()?.name ?: "unknown"

private fun Application.installAuth(deps: GatewayDeps) {
    require(deps.supabase != null || deps.privateToken != null) {
        "No auth configured: set SUPABASE_URL or PRIVATE_TOKEN"
    }
    authentication {
        deps.supabase?.let { supabase ->
            jwt(AUTH_SUPABASE) {
                verifier(supabase.jwks, supabase.issuer) {
                    withAudience(SupabaseAuth.AUDIENCE)
                    acceptLeeway(LEEWAY_SECONDS)
                }
                // Any valid Supabase user, anonymous ones included (is_anonymous isn't checked).
                validate { credential ->
                    credential.payload.subject?.takeIf { it.isNotBlank() }?.let { JWTPrincipal(credential.payload) }
                }
                challenge { _, _ -> call.respond(HttpStatusCode.Unauthorized, UNAUTHORIZED_BODY) }
            }
        }
        deps.privateToken?.let { expected ->
            bearer(AUTH_PRIVATE) {
                authenticate { credential ->
                    if (constantTimeEquals(credential.token, expected)) PrivateCaller() else null
                }
            }
        }
    }
}

private fun Application.installErrors() {
    install(StatusPages) {
        exception<BadRequestException> { call, e ->
            call.respond(HttpStatusCode.BadRequest, ApiError(ApiErrorCode.BAD_REQUEST, e.message.orEmpty()))
        }
        exception<SourceUnavailableException> { call, e ->
            log.warn("Search source unavailable: {}", e.message)
            call.respond(HttpStatusCode.ServiceUnavailable, UNAVAILABLE_BODY)
        }
        exception<Throwable> { call, e ->
            // Never a stack trace in the response; logged by class only, as messages can carry input.
            log.error("Unhandled {}", e::class.qualifiedName)
            call.respond(HttpStatusCode.InternalServerError, ApiError(ApiErrorCode.INTERNAL, "Internal error"))
        }
        status(HttpStatusCode.TooManyRequests) { call, status ->
            call.respond(status, ApiError(ApiErrorCode.RATE_LIMITED, "Too many requests"))
        }
        status(HttpStatusCode.Unauthorized) { call, status -> call.respond(status, UNAUTHORIZED_BODY) }
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(status, ApiError(ApiErrorCode.NOT_FOUND, "Not found"))
        }
    }
}
