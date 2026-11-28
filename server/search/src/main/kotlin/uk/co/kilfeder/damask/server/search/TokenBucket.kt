package uk.co.kilfeder.damask.server.search

import io.ktor.server.plugins.ratelimit.RateLimiter
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.ceil
import kotlin.math.min
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A continuously refilling token bucket for Ktor's RateLimit plugin: [capacity] requests in a
 * burst, then one more every [refillEveryMs] (spec › Search › Limits: search bursts of 20,
 * refilling at 1 a second). Ktor's built-in limiter refills the whole bucket at once per period,
 * which isn't the same thing.
 *
 * `refillAtTimeMillis` is when the bucket will be full again; Ktor drops the limiter for a key at
 * that point, so memory only grows with active users, and a dropped limiter is equivalent to a
 * fresh one.
 */
class TokenBucket(
    private val capacity: Int,
    private val refillEveryMs: Long,
    private val nowMs: () -> Long = System::currentTimeMillis,
) : RateLimiter {
    private val lock = Mutex()
    private var tokens = capacity.toDouble()
    private var updatedAt = nowMs()

    override suspend fun tryConsume(tokens: Int): RateLimiter.State = lock.withLock {
        refill()
        if (this.tokens >= tokens) {
            this.tokens -= tokens
            RateLimiter.State.Available(
                remainingTokens = this.tokens.toInt(),
                limit = capacity,
                refillAtTimeMillis = updatedAt + ((capacity - this.tokens) * refillEveryMs).toLong(),
            )
        } else {
            val missing = tokens - this.tokens
            RateLimiter.State.Exhausted(toWait = waitFor(missing))
        }
    }

    private fun refill() {
        val now = nowMs()
        val elapsed = (now - updatedAt).coerceAtLeast(0)
        tokens = min(capacity.toDouble(), tokens + elapsed.toDouble() / refillEveryMs)
        updatedAt = now
    }

    private fun waitFor(missingTokens: Double): Duration = ceil(missingTokens * refillEveryMs).toLong().milliseconds
}
