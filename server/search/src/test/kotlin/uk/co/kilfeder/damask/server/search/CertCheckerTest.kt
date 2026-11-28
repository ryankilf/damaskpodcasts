package uk.co.kilfeder.damask.server.search

import kotlinx.coroutines.runBlocking
import uk.co.kilfeder.damask.podcasts.api.CertStatus
import java.io.File
import java.nio.file.Files
import java.security.KeyStore
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLServerSocket
import javax.net.ssl.SSLSocket
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals

/** A local TLS server presenting a self-signed certificate with a chosen validity window. */
class SelfSignedServer private constructor(private val socket: SSLServerSocket) : AutoCloseable {
    val port: Int get() = socket.localPort

    override fun close() = socket.close()

    companion object {
        private const val PASSWORD = "changeit"

        /** Valid from [validFromDaysAgo] days ago for [validDays] days. Uses the JDK's keytool. */
        fun start(validFromDaysAgo: Int, validDays: Int): SelfSignedServer {
            val store = File(Files.createTempDirectory("cert").toFile(), "test.p12")
            val keytool = File(System.getProperty("java.home"), "bin/keytool").path
            val process = ProcessBuilder(
                keytool, "-genkeypair", "-alias", "test", "-keyalg", "EC", "-groupname", "secp256r1",
                "-dname", "CN=localhost", "-startdate", "-${validFromDaysAgo}d", "-validity", "$validDays",
                "-keystore", store.path, "-storetype", "PKCS12", "-storepass", PASSWORD, "-keypass", PASSWORD,
            ).redirectErrorStream(true).start()
            check(process.waitFor() == 0) { process.inputStream.bufferedReader().readText() }

            val keyStore = KeyStore.getInstance("PKCS12")
            store.inputStream().use { keyStore.load(it, PASSWORD.toCharArray()) }
            val keys = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
            keys.init(keyStore, PASSWORD.toCharArray())
            val context = SSLContext.getInstance("TLS").apply { init(keys.keyManagers, null, null) }
            val server = context.serverSocketFactory.createServerSocket(0) as SSLServerSocket
            thread(isDaemon = true) {
                while (!server.isClosed) {
                    runCatching { (server.accept() as SSLSocket).use { it.startHandshake() } }
                }
            }
            return SelfSignedServer(server)
        }
    }
}

class CertCheckerTest {
    private fun statusFor(validFromDaysAgo: Int, validDays: Int): String =
        SelfSignedServer.start(validFromDaysAgo, validDays).use { server ->
            runBlocking { CertChecker("localhost", server.port).status() }
        }

    @Test
    fun moreThanFourteenDaysLeftIsOk() = assertEquals(CertStatus.OK, statusFor(validFromDaysAgo = 1, validDays = 60))

    @Test
    fun fourteenDaysOrFewerIsAWarning() =
        assertEquals(CertStatus.WARNING, statusFor(validFromDaysAgo = 80, validDays = 85))

    @Test
    fun expiredIsExpired() = assertEquals(CertStatus.EXPIRED, statusFor(validFromDaysAgo = 30, validDays = 10))

    @Test
    fun unreachableCountsAsExpired() {
        val closed = SelfSignedServer.start(1, 60).also { it.close() }
        assertEquals(CertStatus.EXPIRED, runBlocking { CertChecker("localhost", closed.port).status() })
    }

    @Test
    fun resultIsCachedForAnHour() = runBlocking {
        var now = 0L
        var handshakes = 0
        val server = SelfSignedServer.start(1, 60)
        server.use {
            val checker = CertChecker("localhost", it.port, nowMs = { now }, fetchLeaf = { host, port ->
                handshakes++
                CertChecker.handshakeLeaf(host, port)
            })
            checker.status()
            now += CertChecker.DEFAULT_CACHE_MS - 1
            checker.status()
            assertEquals(1, handshakes)
            now += 2
            checker.status()
            assertEquals(2, handshakes)
        }
    }

    @Test
    fun classifyBoundaries() {
        val day = 24 * 60 * 60 * 1000L
        assertEquals(CertStatus.OK, CertChecker.classify(notAfterMs = 15 * day, nowMs = 0))
        assertEquals(CertStatus.WARNING, CertChecker.classify(notAfterMs = 14 * day, nowMs = 0))
        assertEquals(CertStatus.WARNING, CertChecker.classify(notAfterMs = 1, nowMs = 0))
        assertEquals(CertStatus.EXPIRED, CertChecker.classify(notAfterMs = 0, nowMs = 0))
    }

    @Suppress("unused")
    private fun X509Certificate.daysLeft(now: Long) = (notAfter.time - now) / (24 * 60 * 60 * 1000)
}
