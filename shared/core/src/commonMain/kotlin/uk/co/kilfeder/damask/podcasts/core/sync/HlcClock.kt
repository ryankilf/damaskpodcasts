package uk.co.kilfeder.damask.podcasts.core.sync

import uk.co.kilfeder.damask.podcasts.core.model.Hlc

/**
 * Issues hybrid logical clock timestamps for one device (spec › Sync › Hybrid logical clock).
 *
 * - Issue: `max(now, last.ms)`; the counter increments when the millisecond part doesn't advance.
 * - Receive: `last = max(last, incoming)`, so later local writes order after anything seen.
 *
 * Not thread-safe: callers serialise access (the DB layer owns one instance and persists [last] as
 * `device.last_hlc`).
 */
class HlcClock(
    private val wallClockMs: () -> Long,
    initial: Hlc = Hlc.ZERO,
) {
    var last: Hlc = initial
        private set

    fun issue(): Hlc {
        val now = wallClockMs()
        last = when {
            now > last.wallMs -> Hlc.of(now)
            last.counter < Hlc.MAX_COUNTER -> Hlc.of(last.wallMs, last.counter + 1)
            // Counter exhausted within one millisecond: borrow the next millisecond.
            else -> Hlc.of(last.wallMs + 1)
        }
        return last
    }

    fun receive(incoming: Hlc) {
        if (incoming > last) last = incoming
    }
}
