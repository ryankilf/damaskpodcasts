package uk.co.kilfeder.damask.server.search

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import uk.co.kilfeder.damask.podcasts.api.SearchResponse
import uk.co.kilfeder.damask.podcasts.api.ShowHit
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.time.Instant
import java.util.Base64

const val SUPABASE_URL = "https://project.supabase.co"
const val ISSUER = "$SUPABASE_URL/auth/v1"

/** An ES256 (P-256) signing key with a key ID, like Supabase's default asymmetric keys. */
class TestSigningKey(val kid: String) {
    private val pair: KeyPair = KeyPairGenerator.getInstance("EC")
        .apply { initialize(ECGenParameterSpec("secp256r1")) }
        .generateKeyPair()
    private val public = pair.public as ECPublicKey
    private val private = pair.private as ECPrivateKey

    /** This key as a JWKS entry: kty EC, crv P-256, x/y base64url, as Supabase publishes it. */
    fun jwk(): String {
        val x = coordinate(public.w.affineX)
        val y = coordinate(public.w.affineY)
        return """{"kty":"EC","crv":"P-256","alg":"ES256","use":"sig","kid":"$kid","x":"$x","y":"$y"}"""
    }

    /** A P-256 coordinate as exactly 32 unsigned big-endian bytes, base64url without padding. */
    private fun coordinate(n: java.math.BigInteger): String {
        val raw = n.toByteArray().takeLast(COORD_BYTES).toByteArray()
        val padded = ByteArray(COORD_BYTES - raw.size) + raw
        return Base64.getUrlEncoder().withoutPadding().encodeToString(padded)
    }

    fun token(
        sub: String? = "user-123",
        issuer: String = ISSUER,
        audience: String = "authenticated",
        expiresIn: Long = 3_600,
        anonymous: Boolean = false,
        kid: String = this.kid,
    ): String = JWT.create()
        .withKeyId(kid)
        .withIssuer(issuer)
        .withAudience(audience)
        .apply { sub?.let { withSubject(it) } }
        .withClaim("is_anonymous", anonymous)
        .withClaim("role", "authenticated")
        .withIssuedAt(Instant.now().minusSeconds(10))
        .withExpiresAt(Instant.now().plusSeconds(expiresIn))
        .sign(Algorithm.ECDSA256(public, private))

    private companion object {
        const val COORD_BYTES = 32
    }
}

fun jwks(vararg keys: TestSigningKey) = """{"keys":[${keys.joinToString(",") { it.jwk() }}]}"""

/** A fake source that records requests and can be told to fail. */
class FakeSource(var healthy: Boolean = true) : SearchSource {
    val requests = mutableListOf<SearchRequest>()
    var failure: Throwable? = null

    override suspend fun search(request: SearchRequest): SearchResponse {
        failure?.let { throw it }
        requests += request
        return SearchResponse(listOf(ShowHit(podcastIndexId = 1, title = "Hit", feedUrl = "https://e.com/f")), 1)
    }

    override suspend fun isHealthy(): Boolean = healthy
}

const val PRIVATE_TOKEN = "private-build-token-for-tests"

/** Runs [block] against a gateway with Supabase auth (from [jwksProvider]) and the private token. */
fun gatewayTest(
    deps: GatewayDeps,
    block: suspend ApplicationTestBuilder.(HttpClient) -> Unit,
) = testApplication {
    application { searchGateway(deps) }
    block(client)
}

fun deps(
    source: SearchSource = FakeSource(),
    jwksProvider: JwksKeyProvider? = null,
    privateToken: String? = PRIVATE_TOKEN,
    log: ((String) -> Unit)? = null,
    certChecker: CertChecker? = null,
    burst: Int = GatewayDeps.SEARCH_BURST,
) = GatewayDeps(
    source = source,
    supabase = jwksProvider?.let { SupabaseAuth(SUPABASE_URL, it) },
    privateToken = privateToken,
    certChecker = certChecker,
    searchBurst = burst,
    accessLogSink = log,
)
