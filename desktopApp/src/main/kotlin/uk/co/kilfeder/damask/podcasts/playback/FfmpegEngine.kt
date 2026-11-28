package uk.co.kilfeder.damask.podcasts.playback

import uk.co.kilfeder.damask.podcasts.presentation.playback.AudioEngine
import uk.co.kilfeder.damask.podcasts.presentation.playback.EngineEvent
import uk.co.kilfeder.damask.podcasts.presentation.playback.MediaRequest
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread

private val log: System.Logger = System.getLogger("damask.playback")

/** Plain English for the UI when an episode can't be opened or decoded. */
private const val COULD_NOT_PLAY = "This episode couldn’t be played. Check your connection and try again."

/**
 * The shared [AudioEngine] on desktop (spec › Desktop playback): FFmpeg through JavaCV, PCM out
 * through `javax.sound`. One playback thread owns the decoder and the sound line; the UI's calls
 * are queued to it, and it reports back with [EngineEvent]s (the shared controller hops them to
 * the main thread). Opening a stream happens on that thread, so the UI never waits on the network.
 */
class FfmpegEngine internal constructor(private val newSink: () -> PcmSink) : AudioEngine {
    constructor() : this(::JavaSoundSink)

    override val available = true

    private val commands = LinkedBlockingQueue<(Playback) -> Unit>()
    private var emit: (EngineEvent) -> Unit = {}

    @Volatile
    private var current: Playback? = null

    override val positionMs: Long get() = current?.heardMs() ?: 0

    override val durationMs: Long? get() = current?.decoder?.durationMs

    init {
        thread(name = "damask-playback", isDaemon = true) { run() }
    }

    override fun setListener(listener: (EngineEvent) -> Unit) {
        emit = listener
    }

    override fun load(request: MediaRequest, playWhenReady: Boolean) = post { it.load(request, playWhenReady) }

    override fun play() = post { it.setPlaying(true) }

    override fun pause() = post { it.setPlaying(false) }

    override fun seekTo(positionMs: Long) = post { it.seekTo(positionMs) }

    override fun setSpeed(speed: Float) = post { it.setSpeed(speed) }

    override fun clear() = post { it.clear() }

    private fun post(command: (Playback) -> Unit) {
        commands.put(command)
    }

    private fun run() {
        val playback = Playback()
        current = playback
        while (true) {
            // Wait for a command while idle or paused; otherwise just check for one between chunks.
            val command = if (playback.isPlaying) commands.poll() else commands.take()
            command?.invoke(playback)
            if (playback.isPlaying) playback.step()
        }
    }

    /** The playback thread's state. */
    internal inner class Playback {
        var decoder: FfmpegDecoder? = null
            private set
        private var sink: PcmSink? = null
        var isPlaying = false
            private set
        private var speed = 1f

        /** What's being heard now: the decoded position less what's still in the output buffer. */
        fun heardMs(): Long {
            val decoded = decoder?.contentMs ?: return 0
            val bufferedMs = (sink?.bufferedBytes ?: 0) / BYTES_PER_MS
            return (decoded - bufferedMs * speed).toLong().coerceAtLeast(0)
        }

        fun load(request: MediaRequest, playWhenReady: Boolean) {
            clear()
            speed = request.speed
            try {
                decoder = FfmpegDecoder(request.uri).also {
                    it.speed = speed
                    if (request.startMs > 0) it.seekTo(request.startMs)
                }
                sink = newSink()
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                log.log(System.Logger.Level.WARNING, "Couldn't open ${request.mediaId}", e)
                clear()
                emit(EngineEvent.Failed(COULD_NOT_PLAY))
                return
            }
            emit(EngineEvent.Prepared(decoder?.durationMs))
            setPlaying(playWhenReady, force = true)
        }

        fun setPlaying(playing: Boolean, force: Boolean = false) {
            if (decoder == null || (playing == isPlaying && !force)) return
            isPlaying = playing
            if (playing) sink?.start() else sink?.stop()
            emit(EngineEvent.PlayingChanged(playing))
        }

        fun seekTo(positionMs: Long) {
            val active = decoder ?: return
            val from = heardMs()
            active.seekTo(positionMs)
            sink?.flush()
            emit(EngineEvent.Seeked(from, positionMs))
        }

        fun setSpeed(value: Float) {
            speed = value
            decoder?.speed = value
        }

        /** Decodes one chunk into the sound line (which blocks while full); handles the end. */
        fun step() {
            val active = decoder ?: return
            val more = try {
                active.next { bytes, length -> sink?.write(bytes, length) }
            } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
                log.log(System.Logger.Level.WARNING, "Decoding failed", e)
                isPlaying = false
                emit(EngineEvent.Failed(COULD_NOT_PLAY))
                return
            }
            if (!more) {
                sink?.drain()
                isPlaying = false
                emit(EngineEvent.Ended)
            }
        }

        fun clear() {
            isPlaying = false
            sink?.close()
            sink = null
            decoder?.close()
            decoder = null
        }
    }
}
