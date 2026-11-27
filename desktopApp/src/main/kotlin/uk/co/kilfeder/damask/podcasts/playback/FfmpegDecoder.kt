package uk.co.kilfeder.damask.podcasts.playback

import org.bytedeco.ffmpeg.global.avutil
import org.bytedeco.javacv.FFmpegFrameFilter
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.FrameGrabber
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

private const val MICROS_PER_MS = 1_000L

/** Sent to audio hosts, which count listeners by app. */
private const val USER_AGENT = "Damask/1.0 (desktop)"

/**
 * One episode's audio through FFmpeg: opens [uri] (a URL or a file), decodes to 44.1 kHz stereo
 * 16-bit PCM and, when the speed isn't 1×, runs it through `atempo` so speech keeps its pitch.
 * Not thread-safe: the engine's playback thread owns it.
 */
internal class FfmpegDecoder(uri: String) : AutoCloseable {
    private val grabber = FFmpegFrameGrabber(uri).apply {
        sampleMode = FrameGrabber.SampleMode.SHORT
        sampleFormat = avutil.AV_SAMPLE_FMT_S16
        sampleRate = SAMPLE_RATE
        audioChannels = CHANNELS
        setOption("user_agent", USER_AGENT)
        // Follow redirects and keep reconnecting through dropped connections while streaming.
        setOption("reconnect", "1")
        setOption("reconnect_streamed", "1")
        start()
    }
    private var filter: FFmpegFrameFilter? = null

    private companion object {
        init {
            // FFmpeg's own logging goes to stderr; keep it to errors.
            avutil.av_log_set_level(avutil.AV_LOG_ERROR)
        }
    }
    private var scratch = ByteArray(0)

    /** null when the stream doesn't say. */
    val durationMs: Long? = grabber.lengthInTime.takeIf { it > 0 }?.div(MICROS_PER_MS)

    /** The audio time reached by the last decoded frame. */
    var contentMs: Long = 0
        private set

    var speed: Float = 1f
        set(value) {
            field = value
            rebuildFilter()
        }

    fun seekTo(positionMs: Long) {
        grabber.setAudioTimestamp(positionMs * MICROS_PER_MS)
        contentMs = positionMs
        rebuildFilter()
    }

    /**
     * Decodes the next chunk and passes its PCM bytes to [out]. Returns false at the end of the
     * stream. A chunk may produce no output while `atempo` fills up.
     */
    fun next(out: (ByteArray, Int) -> Unit): Boolean {
        val frame = grabber.grabSamples() ?: return false
        if (frame.samples == null) return true
        contentMs = grabber.timestamp / MICROS_PER_MS
        val tempo = filter
        if (tempo == null) {
            emit(frame, out)
        } else {
            tempo.push(frame)
            while (true) {
                val filtered = tempo.pullSamples() ?: break
                emit(filtered, out)
            }
        }
        return true
    }

    private fun emit(frame: Frame, out: (ByteArray, Int) -> Unit) {
        val shorts = (frame.samples[0] as ShortBuffer).duplicate().apply { rewind() }
        val length = shorts.remaining() * 2
        if (scratch.size < length) scratch = ByteArray(length)
        ByteBuffer.wrap(scratch, 0, length).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(shorts)
        out(scratch, length)
    }

    private fun rebuildFilter() {
        filter?.close()
        filter = if (speed == 1f) {
            null
        } else {
            FFmpegFrameFilter("atempo=$speed", CHANNELS).apply {
                sampleFormat = avutil.AV_SAMPLE_FMT_S16
                sampleRate = SAMPLE_RATE
                start()
            }
        }
    }

    override fun close() {
        filter?.close()
        grabber.close()
    }
}
