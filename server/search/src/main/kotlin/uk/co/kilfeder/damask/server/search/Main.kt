package uk.co.kilfeder.damask.server.search

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.engine.embeddedServer
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import uk.co.kilfeder.damask.server.meili.MeiliClient

private const val MEILI_TIMEOUT_MS = 2_000L
private const val PI_TIMEOUT_MS = 5_000L

fun buildDeps(config: GatewayConfig): GatewayDeps {
    val log = LoggerFactory.getLogger("gateway")
    val source: SearchSource = if (config.piPassthrough) {
        log.warn("PI_PASSTHROUGH is on: searches go to the Podcast Index API. Private build only.")
        PodcastIndexSource(
            http = HttpClient(CIO) {
                install(HttpTimeout) { requestTimeoutMillis = PI_TIMEOUT_MS }
                install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            },
            apiKey = requireNotNull(config.piKey) { "PI_KEY is required with PI_PASSTHROUGH" },
            apiSecret = requireNotNull(config.piSecret) { "PI_SECRET is required with PI_PASSTHROUGH" },
            userAgent = "Damask/${config.version}",
        )
    } else {
        MeiliSource(MeiliClient.create(config.meiliUrl, config.meiliSearchKey, MEILI_TIMEOUT_MS))
    }
    if (config.supabaseUrl == null) log.warn("SUPABASE_URL not set: only the private token is accepted")
    return GatewayDeps(
        source = source,
        supabase = config.supabaseUrl?.let { url ->
            SupabaseAuth(url, JwksKeyProvider(JwksKeyProvider.httpFetcher(JwksKeyProvider.jwksUrl(url))))
        },
        privateToken = config.privateToken,
        certChecker = config.publicHostname?.let { CertChecker(it) },
    )
}

fun main() {
    val config = GatewayConfig.fromEnv()
    val deps = buildDeps(config)
    embeddedServer(io.ktor.server.cio.CIO, port = config.port, host = "0.0.0.0") { searchGateway(deps) }
        .start(wait = true)
}
