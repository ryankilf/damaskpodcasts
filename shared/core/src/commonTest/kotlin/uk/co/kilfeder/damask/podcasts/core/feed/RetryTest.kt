package uk.co.kilfeder.damask.podcasts.core.feed

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Stand-ins for the platform exceptions, matched by name like the real ones. */
private class SocketTimeoutException : Exception("timed out")

private class SSLHandshakeException : Exception("Chain validation failed")

class RetryTest {
    private var calls = 0

    /** A client whose server answers with [replies] in turn: a status, or an exception to throw. */
    private fun client(vararg replies: Any): HttpClient {
        val engine = MockEngine {
            val reply = replies[minOf(calls, replies.lastIndex)]
            calls++
            if (reply is Exception) throw reply
            respond("ok", reply as HttpStatusCode)
        }
        return HttpClient(engine) {
            expectSuccess = false
            retryTransientFailures(baseDelayMs = 1)
        }
    }

    @Test
    fun serverErrorsAndTooManyRequestsAreRetried() = runTest {
        val client = client(HttpStatusCode.ServiceUnavailable, HttpStatusCode.TooManyRequests, HttpStatusCode.OK)
        assertEquals(HttpStatusCode.OK, client.get("https://example.com/feed").status)
        assertEquals(3, calls)
    }

    @Test
    fun theServerSayingNoIsNotRetried() = runTest {
        assertEquals(HttpStatusCode.NotFound, client(HttpStatusCode.NotFound).get("https://example.com/feed").status)
        assertEquals(1, calls)
    }

    @Test
    fun aDroppedConnectionIsRetriedThenGivenUpOn() = runTest {
        assertFailsWith<SocketTimeoutException> { client(SocketTimeoutException()).get("https://example.com/feed") }
        assertEquals(3, calls)
    }

    @Test
    fun certificateFailuresAreNotRetried() = runTest {
        assertFailsWith<SSLHandshakeException> { client(SSLHandshakeException()).get("https://example.com/art.jpg") }
        assertEquals(1, calls)
        assertFalse(isTransientNetworkFailure(Exception("wrapped", SSLHandshakeException())))
        assertTrue(isTransientNetworkFailure(SocketTimeoutException()))
    }
}
