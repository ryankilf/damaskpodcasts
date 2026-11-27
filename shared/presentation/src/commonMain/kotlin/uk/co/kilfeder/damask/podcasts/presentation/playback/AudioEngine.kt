package uk.co.kilfeder.damask.podcasts.presentation.playback

/** One episode for the platform engine to play. */
data class MediaRequest(
    /** Stable per episode ("showId/guid"); the engine echoes it in the media session. */
    val mediaId: String,
    /** A local file path (downloaded) or the feed's enclosure URL. */
    val uri: String,
    val startMs: Long,
    val title: String,
    val showTitle: String,
    val artworkUrl: String?,
    val speed: Float,
)

/** What the engine reports, whoever caused it (the app, a notification, headphones, the car). */
sealed interface EngineEvent {
    /** The media is ready; [durationMs] is null when the stream doesn't say. */
    data class Prepared(val durationMs: Long?) : EngineEvent

    data class PlayingChanged(val playing: Boolean) : EngineEvent

    data class BufferingChanged(val buffering: Boolean) : EngineEvent

    /** The position jumped from [fromMs] to [toMs] (a seek, from anywhere). */
    data class Seeked(val fromMs: Long, val toMs: Long) : EngineEvent

    /** Played to the true end of the stream. */
    data object Ended : EngineEvent

    /** Couldn't play; [message] is plain English for the user. */
    data class Failed(val message: String) : EngineEvent
}

/**
 * The platform's audio player: Media3 on Android, FFmpeg on desktop (to come), AVPlayer on iOS
 * (to come). It only plays; every rule (positions, finishing, autoplay, stats) lives in
 * [PlaybackController]. Called on the main thread only.
 */
interface AudioEngine {
    /** false where there's no engine yet: the UI shows playback controls disabled. */
    val available: Boolean

    val positionMs: Long

    /** null until known. */
    val durationMs: Long?

    fun setListener(listener: (EngineEvent) -> Unit)

    /** Loads [request]. Must then report [EngineEvent.PlayingChanged], even if nothing changed. */
    fun load(request: MediaRequest, playWhenReady: Boolean)

    fun play()

    fun pause()

    fun seekTo(positionMs: Long)

    fun setSpeed(speed: Float)

    /** Stops and forgets the current media. */
    fun clear()
}

/** No playback on this platform yet. */
object NoAudioEngine : AudioEngine {
    override val available = false
    override val positionMs = 0L
    override val durationMs: Long? = null

    override fun setListener(listener: (EngineEvent) -> Unit) = Unit

    override fun load(request: MediaRequest, playWhenReady: Boolean) = Unit

    override fun play() = Unit

    override fun pause() = Unit

    override fun seekTo(positionMs: Long) = Unit

    override fun setSpeed(speed: Float) = Unit

    override fun clear() = Unit
}
