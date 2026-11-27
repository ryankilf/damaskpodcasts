package uk.co.kilfeder.damask.server.search

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.runBlocking
import uk.co.kilfeder.damask.podcasts.api.ApiError
import uk.co.kilfeder.damask.podcasts.api.ApiJson
import uk.co.kilfeder.damask.podcasts.api.CertStatus
import uk.co.kilfeder.damask.podcasts.api.HealthResponse
import uk.co.kilfeder.damask.podcasts.api.HealthStatus
import io.ktor.server.plugins.ratelimit.RateLimiter
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class ValidationTest {
    private val source = FakeSource()

    @Test
    // More than 20 cases, so a bigger bucket than production's: this tests validation, not limits.
    fun everyBadParameterIsA400() = gatewayTest(deps(source, burst = 100)) { client ->
        val bad = listOf(
            "", "q=", "q=%20%20", "q=" + "a".repeat(101),
            "q=x&lang=e", "q=x&lang=english", "q=x&lang=e1", "q=x&lang=en_GB", "q=x&lang=en-GBR",
            "q=x&lang=en-1", "q=x&category=podcasts", "q=x&category=News", "q=x&category=news&category=bogus",
            "q=x&hideExplicit=yes", "q=x&hideExplicit=1",
            "q=x&limit=0", "q=x&limit=21", "q=x&limit=ten",
            "q=x&offset=-1", "q=x&offset=41", "q=x&offset=x",
        )
        bad.forEach { query ->
            val response = client.get("/v1/search?$query") { bearerAuth(PRIVATE_TOKEN) }
            assertEquals(HttpStatusCode.BadRequest, response.status, query)
            assertEquals("bad_request", ApiJson.decodeFromString(ApiError.serializer(), response.bodyAsText()).error)
        }
        assertTrue(source.requests.isEmpty(), "nothing invalid reaches the source")
    }

    @Test
    fun validRequestsAreTrimmedAndDefaulted() = gatewayTest(deps(source)) { client ->
        client.get("/v1/search?q=%20hardcore%20history%20") { bearerAuth(PRIVATE_TOKEN) }
        val everything = "q=x&lang=FR&hideExplicit=true&limit=20&offset=40&unknown=1"
        client.get("/v1/search?$everything") { bearerAuth(PRIVATE_TOKEN) }
        assertEquals(SearchRequest("hardcore history", null, false, 20, 0), source.requests[0])
        assertEquals(SearchRequest("x", "fr", true, 20, 40), source.requests[1])
    }

    @Test
    fun languageRegionsAndCategoriesAreNormalised() = gatewayTest(deps(source)) { client ->
        client.get("/v1/search?q=x&lang=en-GB&category=news&category=history&category=news") {
            bearerAuth(PRIVATE_TOKEN)
        }
        client.get("/v1/search?q=x&lang=ES-419") { bearerAuth(PRIVATE_TOKEN) }
        client.get("/v1/search?q=x&lang=eng") { bearerAuth(PRIVATE_TOKEN) }
        val (gb, latam, eng) = source.requests
        assertEquals("en", gb.langPrefix)
        assertEquals("en-gb", gb.langTag)
        assertEquals(listOf("news", "history"), gb.categories, "de-duplicated, in order")
        assertEquals("es", latam.langPrefix)
        assertEquals("es-419", latam.langTag)
        assertEquals("en", eng.langPrefix, "three-letter code normalised")
        assertEquals(null, eng.langTag, "no region: a single query")
    }
}

class RateLimitTest {
    @Test
    fun tokenBucketBurstsThenRefillsOnePerSecond() = runBlocking {
        var now = 0L
        val bucket = TokenBucket(capacity = 20, refillEveryMs = 1_000, nowMs = { now })
        repeat(20) { assertIs<RateLimiter.State.Available>(bucket.tryConsume(1)) }
        val exhausted = assertIs<RateLimiter.State.Exhausted>(bucket.tryConsume(1))
        assertEquals(1.seconds, exhausted.toWait)
        now += 1_000
        assertIs<RateLimiter.State.Available>(bucket.tryConsume(1))
        assertIs<RateLimiter.State.Exhausted>(bucket.tryConsume(1))
        now += 60_000
        val refilled = assertIs<RateLimiter.State.Available>(bucket.tryConsume(1))
        assertEquals(19, refilled.remainingTokens, "never more than the burst")
    }

    @Test
    fun overTheLimitIsA429WithRetryAfter() {
        val key = TestSigningKey("k")
        val jwksFor = JwksKeyProvider(fetchJwks = { jwks(key) })
        gatewayTest(deps(jwksProvider = jwksFor, burst = 3)) { client ->
            val alice = key.token(sub = "alice")
            repeat(3) { assertEquals(HttpStatusCode.OK, client.get("/v1/search?q=x") { bearerAuth(alice) }.status) }
            val limited = client.get("/v1/search?q=x") { bearerAuth(alice) }
            assertEquals(HttpStatusCode.TooManyRequests, limited.status)
            assertEquals("1", limited.headers[HttpHeaders.RetryAfter])
            assertEquals("rate_limited", ApiJson.decodeFromString(ApiError.serializer(), limited.bodyAsText()).error)

            // Limits are per user: someone else is unaffected.
            assertEquals(HttpStatusCode.OK, client.get("/v1/search?q=x") { bearerAuth(key.token(sub = "bob")) }.status)
        }
    }
}

class ErrorTest {
    private val source = FakeSource()

    @Test
    fun sourceOutageIsA503WithoutDetail() = gatewayTest(deps(source)) { client ->
        source.failure = SourceUnavailableException("Meilisearch search failed (HttpRequestTimeoutException)")
        val response = client.get("/v1/search?q=x") { bearerAuth(PRIVATE_TOKEN) }
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertNoInternals(response.bodyAsText())
    }

    @Test
    fun unexpectedErrorsNeverLeakAStackTrace() = gatewayTest(deps(source)) { client ->
        source.failure = IllegalStateException("boom with internal detail")
        val response = client.get("/v1/search?q=x") { bearerAuth(PRIVATE_TOKEN) }
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val body = response.bodyAsText()
        assertNoInternals(body)
        assertFalse("boom" in body)
        assertEquals("internal", ApiJson.decodeFromString(ApiError.serializer(), body).error)
    }

    @Test
    fun unknownPathsAreJson404s() = gatewayTest(deps(source)) { client ->
        val response = client.get("/v2/search?q=x")
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertEquals("not_found", ApiJson.decodeFromString(ApiError.serializer(), response.bodyAsText()).error)
    }

    private fun assertNoInternals(body: String) {
        listOf("\tat ", "Exception", "uk.co.kilfeder", "kotlin.", "java.").forEach {
            assertFalse(it in body, "response leaks '$it': $body")
        }
    }
}

class AccessLogTest {
    @Test
    fun logsNeitherQueryTextNorRawSub() {
        val lines = mutableListOf<String>()
        val key = TestSigningKey("k")
        val provider = JwksKeyProvider(fetchJwks = { jwks(key) })
        val stderr = ByteArrayOutputStream()
        val original = System.err
        System.setErr(PrintStream(stderr, true))
        try {
            gatewayTest(deps(jwksProvider = provider, log = { lines += it })) { client ->
                client.get("/v1/search?q=supersecretquery&lang=en") { bearerAuth(key.token(sub = "user-0042")) }
            }
        } finally {
            System.setErr(original)
        }
        val line = lines.single()
        // Exactly: method, path (no query string), status, latency, caller hash. Nothing else.
        assertTrue(Regex("""^GET /v1/search 200 \d+ms caller=[0-9a-f]{12}$""").matches(line), line)
        assertTrue("caller=${shortHash("user-0042")}" in line)
        val everything = lines.joinToString("\n") + "\n" + stderr.toString()
        assertFalse("supersecretquery" in everything, "query text must never be logged")
        assertFalse("user-0042" in everything, "the raw sub must never be logged")
        assertFalse("lang=en" in everything, "no query string at all")
    }
}

class AccessLogOnceTest {
    @Test
    fun eachRequestIsLoggedExactlyOnce() {
        val lines = mutableListOf<String>()
        gatewayTest(deps(log = { lines += it })) { client ->
            client.get("/v1/search?q=x") // no token: challenge + JSON body
            client.get("/v1/search?q=x") { bearerAuth("wrong") }
            client.get("/v1/search?q=x") { bearerAuth(PRIVATE_TOKEN) }
        }
        assertEquals(listOf("401", "401", "200"), lines.map { it.split(' ')[2] })
    }
}

class HealthTest {
    @Test
    fun okWhenTheIndexIsUpAndNoCertificateToCheck() = gatewayTest(deps(FakeSource(healthy = true))) { client ->
        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(
            HealthResponse(HealthStatus.OK, db = true, cert = CertStatus.UNKNOWN),
            ApiJson.decodeFromString(HealthResponse.serializer(), response.bodyAsText()),
        )
    }

    @Test
    fun downIsA503() = gatewayTest(deps(FakeSource(healthy = false))) { client ->
        val response = client.get("/health")
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        val health = ApiJson.decodeFromString(HealthResponse.serializer(), response.bodyAsText())
        assertEquals(HealthStatus.DOWN, health.status)
    }

    @Test
    fun certificateWarningsAreA200WithStatusWarning() {
        val cert = SelfSignedServer.start(validFromDaysAgo = 80, validDays = 85) // 5 days left
        try {
            gatewayTest(deps(FakeSource(), certChecker = CertChecker("localhost", cert.port))) { client ->
                val response = client.get("/health")
                assertEquals(HttpStatusCode.OK, response.status)
                assertEquals(
                    HealthResponse(HealthStatus.WARNING, db = true, cert = CertStatus.WARNING),
                    ApiJson.decodeFromString(HealthResponse.serializer(), response.bodyAsText()),
                )
            }
        } finally {
            cert.close()
        }
    }

    @Test
    fun statusRules() {
        assertEquals(HealthStatus.OK, healthOf(db = true, cert = CertStatus.OK).status)
        assertEquals(HealthStatus.OK, healthOf(db = true, cert = CertStatus.UNKNOWN).status)
        assertEquals(HealthStatus.WARNING, healthOf(db = true, cert = CertStatus.WARNING).status)
        assertEquals(HealthStatus.WARNING, healthOf(db = true, cert = CertStatus.EXPIRED).status)
        assertEquals(HealthStatus.DOWN, healthOf(db = false, cert = CertStatus.OK).status)
    }
}
