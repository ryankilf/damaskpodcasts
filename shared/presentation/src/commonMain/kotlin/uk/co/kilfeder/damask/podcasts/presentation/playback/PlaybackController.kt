package uk.co.kilfeder.damask.podcasts.presentation.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.toKey

/** What's playing, for the mini-player and the episode screen. null = nothing loaded. */
data class NowPlaying(
    val id: EpisodeId,
    /** For system media controls (notification, Now Playing, MPRIS). */
    val title: String,
    val showTitle: String,
    val artworkUrl: String?,
    val isPlaying: Boolean,
    val buffering: Boolean,
    val positionMs: Long,
    val durationMs: Long?,
    val speed: Float,
    /** Plain English, when the episode couldn't play. */
    val error: String? = null,
)

/**
 * The one playback controller for the process: what the UI calls. The rules live in
 * [PlaybackCore]; this only hands intents to it on the main thread.
 */
class PlaybackController internal constructor(
    graph: AppGraph,
    private val engine: AudioEngine,
    private val scope: CoroutineScope,
) {
    private val core = PlaybackCore(graph, engine, scope)

    val state: StateFlow<NowPlaying?> = core.state

    /** false where the platform has no engine yet: controls show disabled. */
    val available: Boolean get() = engine.available

    /**
     * Plays [id]: resumes it if it's already loaded, otherwise stops the current episode (which
     * stays In Progress at its position) and starts this one. [context] null = the context saved
     * with the episode (resuming from In Progress), else [PlayContext.None].
     */
    fun play(id: EpisodeId, context: PlayContext?) {
        if (!engine.available) return
        scope.launch { core.locked { start(id.toKey(), context) } }
    }

    fun togglePlayPause() {
        val now = state.value ?: return
        if (now.isPlaying) engine.pause() else engine.play()
    }

    fun seekTo(positionMs: Long) {
        if (state.value == null) return
        engine.seekTo(positionMs.coerceAtLeast(0))
    }

    /** Plays or pauses [id] if it's the one loaded, else starts it (same rules as [play]). */
    fun playOrToggle(id: EpisodeId, context: PlayContext?) {
        if (state.value?.id == id) togglePlayPause() else play(id, context)
    }

    fun skipBack() = skipBy(-SKIP_BACK_MS)

    fun skipForward() = skipBy(SKIP_FORWARD_MS)

    fun skipBy(deltaMs: Long) {
        val now = state.value ?: return
        val target = (engine.positionMs + deltaMs).coerceAtLeast(0)
        engine.seekTo(now.durationMs?.let { target.coerceAtMost(it) } ?: target)
    }

    fun setSpeed(value: Float) = core.setSpeed(value)

    /** Marking the playing episode played stops it without saving a position over the mark. */
    fun onMarkedPlayed(id: EpisodeId) {
        scope.launch { core.locked { if (isCurrent(id.toKey())) stopWithoutSaving() } }
    }

    companion object {
        /** The skip buttons everywhere: in the app, the notification, media keys, the car. */
        const val SKIP_BACK_MS = 10_000L
        const val SKIP_FORWARD_MS = 30_000L
    }
}
