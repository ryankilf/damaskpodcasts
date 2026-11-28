package uk.co.kilfeder.damask.server.search

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import uk.co.kilfeder.damask.podcasts.api.CertStatus
import uk.co.kilfeder.damask.podcasts.api.HealthResponse
import uk.co.kilfeder.damask.podcasts.api.HealthStatus
import java.net.InetSocketAddress
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocket
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/** `status` is `down` without the index, else `warning` if the certificate needs attention, else `ok`. */
fun healthOf(db: Boolean, cert: String): HealthResponse {
    val status = when {
        !db -> HealthStatus.DOWN
        cert == CertStatus.WARNING || cert == CertStatus.EXPIRED -> HealthStatus.WARNING
        else -> HealthStatus.OK
    }
    return HealthResponse(status = status, db = db, cert = cert)
}

/**
 * Checks the gateway's own public certificate (the one Caddy serves) by doing a TLS handshake to
 * [host] and reading the leaf certificate's expiry. Cached for [cacheMs] (an hour), so `/health`
 * stays cheap however often a monitor polls it.
 */
class CertChecker(
    private val host: String,
    private val port: Int = HTTPS_PORT,
    private val cacheMs: Long = DEFAULT_CACHE_MS,
    private val nowMs: () -> Long = System::currentTimeMillis,
    private val fetchLeaf: (String, Int) -> X509Certificate = ::handshakeLeaf,
) {
    private val log = LoggerFactory.getLogger(CertChecker::class.java)

    @Volatile private var cached: Pair<Long, String>? = null

    suspend fun status(): String {
        val now = nowMs()
        cached?.let { (at, status) -> if (now - at < cacheMs) return status }
        val status = withContext(Dispatchers.IO) {
            runCatching { classify(fetchLeaf(host, port).notAfter.time, now) }
                .onFailure { log.warn("Certificate check failed: {}", it::class.simpleName) }
                .getOrDefault(CertStatus.EXPIRED)
        }
        cached = now to status
        return status
    }

    companion object {
        const val HTTPS_PORT = 443
        const val DEFAULT_CACHE_MS = 60 * 60 * 1000L
        const val WARNING_DAYS = 14L
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        private const val TIMEOUT_MS = 5_000

        fun classify(notAfterMs: Long, nowMs: Long): String = when {
            notAfterMs <= nowMs -> CertStatus.EXPIRED
            notAfterMs - nowMs <= WARNING_DAYS * DAY_MS -> CertStatus.WARNING
            else -> CertStatus.OK
        }

        /**
         * Reads the server's leaf certificate. Trust isn't checked: this only inspects the expiry of
         * our own certificate, and must still report an expired or broken one rather than fail.
         */
        fun handshakeLeaf(host: String, port: Int): X509Certificate {
            val context = SSLContext.getInstance("TLS")
            context.init(null, arrayOf<TrustManager>(InspectOnlyTrustManager), SecureRandom())
            (context.socketFactory.createSocket() as SSLSocket).use { socket ->
                socket.connect(InetSocketAddress(host, port), TIMEOUT_MS)
                socket.soTimeout = TIMEOUT_MS
                if (host.any { it.isLetter() }) {
                    val params = socket.sslParameters
                    params.serverNames = listOf(javax.net.ssl.SNIHostName(host))
                    socket.sslParameters = params
                }
                socket.startHandshake()
                return socket.session.peerCertificates.first() as X509Certificate
            }
        }
    }

    @Suppress("EmptyFunctionBlock")
    private object InspectOnlyTrustManager : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
