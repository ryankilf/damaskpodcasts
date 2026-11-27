package uk.co.kilfeder.damask.podcasts.playback

import uk.co.kilfeder.damask.podcasts.presentation.playback.EngineEvent
import uk.co.kilfeder.damask.podcasts.presentation.playback.MediaRequest
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Counts PCM instead of playing it, as fast as the decoder goes. */
private class CountingSink : PcmSink {
    @Volatile
    var bytes = 0L
    override val bufferedBytes = 0

    override fun write(bytes: ByteArray, length: Int) {
        this.bytes += length
    }

    override fun start() = Unit

    override fun stop() = Unit

    override fun flush() = Unit

    override fun drain() = Unit

    override fun close() = Unit
}

/**
 * The desktop engine against two 30-second test tones (MP3 and Opus, made with the ffmpeg CLI):
 * formats open, `atempo` really changes the amount of audio, seeking lands where asked, and the
 * engine reports the events the shared controller relies on.
 */
class FfmpegEngineTest {
    private fun fixture(name: String) = File(javaClass.getResource("/audio/$name")!!.toURI()).absolutePath

    private fun decodedSeconds(name: String, speed: Float): Double {
        val sink = CountingSink()
        FfmpegDecoder(fixture(name)).use { decoder ->
            decoder.speed = speed
            while (decoder.next { _, length -> sink.write(ByteArray(0), length) }) Unit
        }
        return sink.bytes / BYTES_PER_MS / 1000
    }

    @Test
    fun opensMp3AndOpusWithTheirLength() {
        listOf("tone-30s.mp3", "tone-30s.opus").forEach { name ->
            FfmpegDecoder(fixture(name)).use { decoder ->
                val duration = decoder.durationMs ?: error("$name: no duration")
                assertTrue(duration in 29_500L..30_500L, "$name: $duration ms")
            }
        }
    }

    @Test
    fun speedChangesHowMuchAudioThereIs() {
        listOf("tone-30s.mp3", "tone-30s.opus").forEach { name ->
            val normal = decodedSeconds(name, 1f)
            val double = decodedSeconds(name, 2f)
            assertTrue(normal in 29.0..31.0, "$name at 1×: $normal s")
            assertTrue(double in 14.0..16.0, "$name at 2×: $double s")
        }
    }

    @Test
    fun seekingLandsWhereAsked() {
        FfmpegDecoder(fixture("tone-30s.mp3")).use { decoder ->
            decoder.seekTo(20_000)
            decoder.next { _, _ -> }
            assertTrue(decoder.contentMs in 19_500L..20_600L, "landed at ${decoder.contentMs}")
        }
    }

    @Test
    fun engineReportsPreparedPlayingAndEnded() {
        val events = CopyOnWriteArrayList<EngineEvent>()
        val engine = FfmpegEngine { CountingSink() }
        engine.setListener { events += it }
        engine.load(
            MediaRequest("s/e", fixture("tone-30s.opus"), startMs = 0, "Tone", "Tests", null, speed = 1f),
            playWhenReady = true,
        )
        val deadline = System.currentTimeMillis() + 10_000
        while (EngineEvent.Ended !in events && System.currentTimeMillis() < deadline) Thread.sleep(20)
        val prepared = events.filterIsInstance<EngineEvent.Prepared>().single()
        assertTrue((prepared.durationMs ?: 0) in 29_500L..30_500L)
        assertEquals(EngineEvent.PlayingChanged(true), events[1])
        assertEquals(EngineEvent.Ended, events.last())
    }
}
