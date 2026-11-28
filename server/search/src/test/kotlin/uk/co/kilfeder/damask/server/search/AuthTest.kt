package uk.co.kilfeder.damask.server.search

import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val SEARCH = "/v1/search?q=history"

class SupabaseJwtTest {
    private val key = TestSigningKey("key-1")
    private var served = jwks(key)
    private var clock = 1_000_000L
    private val provider = JwksKeyProvider(fetchJwks = { served }, nowMs = { clock })

    @Test
    fun validEs256TokenIsAccepted() = gatewayTest(deps(jwksProvider = provider)) { client ->
        assertEquals(HttpStatusCode.OK, client.get(SEARCH) { bearerAuth(key.token()) }.status)
    }

    @Test
    fun anonymousUsersAreAccepted() = gatewayTest(deps(jwksProvider = provider)) { client ->
        val anonymous = key.token(sub = "anon-1", anonymous = true)
        assertEquals(HttpStatusCode.OK, client.get(SEARCH) { bearerAuth(anonymous) }.status)
    }

    @Test
    fun badTokensAreRejected() = gatewayTest(deps(jwksProvider = provider)) { client ->
        val bad = listOf(
            key.token(expiresIn = -3_600),
            key.token(issuer = "https://evil.supabase.co/auth/v1"),
            key.token(audience = "anon"),
            key.token(sub = null),
            "not-a-jwt",
        )
        bad.forEach { token ->
            val response = client.get(SEARCH) { bearerAuth(token) }
            assertEquals(HttpStatusCode.Unauthorized, response.status, token.take(20))
            assertTrue("\"unauthorized\"" in response.bodyAsText())
        }
        assertEquals(HttpStatusCode.Unauthorized, client.get(SEARCH).status)
    }

    @Test
    fun tokenSignedByAnotherKeyWithAKnownKidIsRejected() = gatewayTest(deps(jwksProvider = provider)) { client ->
        val impostor = TestSigningKey("key-1")
        assertEquals(HttpStatusCode.Unauthorized, client.get(SEARCH) { bearerAuth(impostor.token()) }.status)
    }

    @Test
    fun unknownKidTriggersARefresh() = gatewayTest(deps(jwksProvider = provider)) { client ->
        assertEquals(HttpStatusCode.OK, client.get(SEARCH) { bearerAuth(key.token()) }.status)
        assertEquals(1, provider.fetchCount)

        // Supabase rotates to a new key; the gateway hasn't seen it yet.
        val rotated = TestSigningKey("key-2")
        served = jwks(key, rotated)
        clock += JwksKeyProvider.DEFAULT_MIN_REFRESH_MS
        assertEquals(HttpStatusCode.OK, client.get(SEARCH) { bearerAuth(rotated.token()) }.status)
        assertEquals(2, provider.fetchCount)
    }

    @Test
    fun refreshesAreRateLimited() = gatewayTest(deps(jwksProvider = provider)) { client ->
        client.get(SEARCH) { bearerAuth(key.token()) }
        val madeUp = TestSigningKey("made-up")
        repeat(5) { client.get(SEARCH) { bearerAuth(madeUp.token()) } }
        assertEquals(1, provider.fetchCount, "unknown kids within the interval don't refetch")
        clock += JwksKeyProvider.DEFAULT_MIN_REFRESH_MS
        repeat(5) { client.get(SEARCH) { bearerAuth(madeUp.token()) } }
        assertEquals(2, provider.fetchCount, "at most one refresh per interval")
    }

    @Test
    fun keysAreRefreshedHourly() {
        provider.get("key-1")
        clock += JwksKeyProvider.DEFAULT_MAX_AGE_MS + 1
        provider.get("key-1")
        assertEquals(2, provider.fetchCount)
    }
}

class PrivateTokenTest {
    @Test
    fun acceptedWhenSet() = gatewayTest(deps(privateToken = PRIVATE_TOKEN)) { client ->
        assertEquals(HttpStatusCode.OK, client.get(SEARCH) { bearerAuth(PRIVATE_TOKEN) }.status)
        assertEquals(HttpStatusCode.Unauthorized, client.get(SEARCH) { bearerAuth("$PRIVATE_TOKEN-wrong") }.status)
    }

    @Test
    fun rejectedWhenUnset() {
        val provider = JwksKeyProvider(fetchJwks = { jwks(TestSigningKey("k")) })
        gatewayTest(deps(jwksProvider = provider, privateToken = null)) { client ->
            assertEquals(HttpStatusCode.Unauthorized, client.get(SEARCH) { bearerAuth(PRIVATE_TOKEN) }.status)
        }
    }

    @Test
    fun comparisonIsConstantTime() {
        assertTrue(constantTimeEquals("secret", "secret"))
        assertFalse(constantTimeEquals("secret", "secreT"))
        assertFalse(constantTimeEquals("s", "a-much-longer-secret"))
        // Both sides are hashed to 32 bytes before MessageDigest.isEqual (itself constant-time),
        // so neither the content nor the length of the expected token affects timing.
    }
}
