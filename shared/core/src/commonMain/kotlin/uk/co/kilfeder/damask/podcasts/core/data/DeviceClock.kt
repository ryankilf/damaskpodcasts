package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.model.Hlc
import uk.co.kilfeder.damask.podcasts.core.sync.HlcClock
import kotlin.uuid.Uuid

/**
 * This device's identity and HLC clock, persisted in the `device` row. The clock resumes from
 * `device.last_hlc`, so timestamps keep increasing across restarts even if the wall clock goes
 * backwards. One instance per database. Not thread-safe: use from a single database dispatcher.
 */
class DeviceClock(
    private val db: DamaskDatabase,
    private val wallClockMs: () -> Long,
) {
    val deviceId: String
    private val clock: HlcClock

    init {
        val device = db.deviceQueries.selectDevice().executeAsOneOrNull()
            ?: Uuid.random().toString().let { id ->
                db.deviceQueries.insertDevice(id = id, last_hlc = 0)
                db.deviceQueries.selectDevice().executeAsOne()
            }
        deviceId = device.id
        clock = HlcClock(wallClockMs, Hlc(device.last_hlc))
    }

    fun nowMs(): Long = wallClockMs()

    fun issue(): Hlc = clock.issue().also { persist() }

    /** Called for every HLC pulled from the server, so later local writes order after it. */
    fun receive(remote: Hlc) {
        clock.receive(remote)
        persist()
    }

    private fun persist() {
        db.deviceQueries.advanceLastHlc(last_hlc = clock.last.value, id = deviceId)
    }
}
