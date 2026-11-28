package uk.co.kilfeder.damask.podcasts.playback

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

/** Decoded audio: 44.1 kHz, 16-bit signed, stereo, little-endian. */
internal const val SAMPLE_RATE = 44_100
internal const val CHANNELS = 2
internal const val BYTES_PER_FRAME = 4
internal const val BYTES_PER_MS = SAMPLE_RATE * BYTES_PER_FRAME / 1000.0

/** Where decoded PCM goes: the sound card, or (in tests) a counter. */
internal interface PcmSink {
    /** Blocks while the output buffer is full, which paces decoding to real time. */
    fun write(bytes: ByteArray, length: Int)

    /** Bytes written but not yet heard. */
    val bufferedBytes: Int

    fun start()

    fun stop()

    /** Drops anything not yet heard (after a seek). */
    fun flush()

    /** Blocks until everything written has been heard (at the end of an episode). */
    fun drain()

    fun close()
}

/** The default sound output through `javax.sound`. */
internal class JavaSoundSink : PcmSink {
    private val line: SourceDataLine = AudioSystem.getSourceDataLine(FORMAT).apply { open(FORMAT) }

    override val bufferedBytes: Int get() = line.bufferSize - line.available()

    override fun write(bytes: ByteArray, length: Int) {
        line.write(bytes, 0, length)
    }

    override fun start() = line.start()

    override fun stop() = line.stop()

    override fun flush() = line.flush()

    override fun drain() = line.drain()

    override fun close() = line.close()

    private companion object {
        val FORMAT = AudioFormat(SAMPLE_RATE.toFloat(), 16, CHANNELS, true, false)
    }
}
