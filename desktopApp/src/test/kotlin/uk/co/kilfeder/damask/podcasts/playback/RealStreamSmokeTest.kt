package uk.co.kilfeder.damask.podcasts.playback

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Opt-in: streams a real episode over the network, at 1.5× from 10 minutes in, for a few seconds
 * of decoding. Proves TLS, redirects and seeking on a live host.
 *
 *     ./gradlew :desktopApp:test --tests '*RealStreamSmokeTest*' -Pdamask.audio=<enclosure URL>
 */
class RealStreamSmokeTest {
    @Test
    fun streamsARealEpisode() {
        val url = System.getProperty("damask.audio") ?: return
        FfmpegDecoder(url).use { decoder ->
            println("duration: ${decoder.durationMs} ms")
            decoder.speed = 1.5f
            decoder.seekTo(10 * 60_000L)
            var bytes = 0L
            while (bytes < 5 * 1000 * BYTES_PER_MS && decoder.next { _, length -> bytes += length }) Unit
            println("after seek: ${decoder.contentMs} ms, decoded ${bytes / BYTES_PER_MS} ms of output")
            assertTrue(decoder.contentMs in 600_000L..620_000L, "landed at ${decoder.contentMs}")
        }
    }
}
