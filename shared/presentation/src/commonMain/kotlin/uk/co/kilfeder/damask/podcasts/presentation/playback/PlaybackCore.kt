package uk.co.kilfeder.damask.podcasts.presentation.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext
import uk.co.kilfeder.damask.podcasts.core.playback.Autoplay
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.freshRotation
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId

/** How often the position on screen moves on while playing. */
private const val TICK_MS = 500L

/**
 * The playback rules (spec › Finishing and autoplay, Sync › Write triggers): resume where you left
 * off, save on the write triggers, finish on stop in the last couple of minutes or at the true
 * end, then autoplay what the episode's context says comes next. Engine events are handled the
 * same whoever caused them: the app, the notification, headphones or the car.
 *
 * Everything runs under one lock on the main thread, where the engine must be called.
 */
internal class PlaybackCore(
    private val graph: AppGraph,
    private val engine: AudioEngine,
    private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<NowPlaying?>(null)
    val state: StateFlow<NowPlaying?> = _state.asStateFlow()

    private val mutex = Mutex()
    private var session: ListeningSession? = null
    private val ticker = Ticker(scope, TICK_MS) { locked { tick() } }
    private var speed = 1f

    init {
        engine.setListener { event -> scope.launch { locked { onEvent(event) } } }
    }

    suspend fun locked(block: suspend PlaybackCore.() -> Unit) = mutex.withLock { block() }

    fun isCurrent(key: EpisodeKey) = session?.key == key

    fun setSpeed(value: Float) {
        speed = value
        engine.setSpeed(value)
        _state.update { it?.copy(speed = value) }
    }

    suspend fun start(key: EpisodeKey, requested: PlayContext?) {
        if (session?.key == key) {
            engine.play()
            return
        }
        session?.let { if (_state.value?.isPlaying == true) it.save(engine.positionMs, stopped = true) }
        val library = graph.library.first()
        val plan = PlaybackPlan.of(graph, library, key, requested, speed)
        if (plan == null) {
            stopWithoutSaving()
            return
        }
        session = ListeningSession(graph, key, plan.context, plan.categoryId).also { it.durationMs = plan.durationMs }
        engine.load(plan.request, playWhenReady = true)
        _state.value = NowPlaying(
            id = EpisodeId(key.showId.value, key.guid),
            isPlaying = false,
            buffering = true,
            positionMs = plan.request.startMs,
            durationMs = plan.durationMs,
            speed = speed,
        )
    }

    fun stopWithoutSaving() {
        stopTicker()
        session = null
        engine.clear()
        _state.value = null
    }

    private suspend fun onEvent(event: EngineEvent) {
        val current = session ?: return
        when (event) {
            is EngineEvent.Prepared -> onPrepared(current, event.durationMs)
            is EngineEvent.PlayingChanged -> onPlayingChanged(current, event.playing)
            is EngineEvent.BufferingChanged -> _state.update { it?.copy(buffering = event.buffering) }
            is EngineEvent.Seeked -> {
                current.seeked(event.fromMs, event.toMs, playing = _state.value?.isPlaying == true)
                _state.update { it?.copy(positionMs = event.toMs) }
            }
            EngineEvent.Ended -> onEnded(current)
            is EngineEvent.Failed -> {
                stopTicker()
                _state.update { it?.copy(isPlaying = false, buffering = false, error = event.message) }
            }
        }
    }

    private suspend fun onPrepared(current: ListeningSession, durationMs: Long?) {
        if (durationMs == null || durationMs <= 0) return
        current.durationMs = durationMs
        graph.onDb { graph.userState.reportPlayerDuration(current.key, durationMs) }
        _state.update { it?.copy(durationMs = durationMs) }
    }

    private suspend fun onPlayingChanged(current: ListeningSession, playing: Boolean) {
        // Engines may repeat a state (Media3 after a load); only real changes start or end a stretch.
        if (_state.value?.isPlaying == playing && (playing || current.hasStarted)) return
        current.hasStarted = true
        _state.update { it?.copy(isPlaying = playing, buffering = false, error = null) }
        if (playing) {
            current.startStretch(engine.positionMs)
            ticker.start()
        } else {
            stopTicker()
            current.save(engine.positionMs, stopped = true)
        }
    }

    private suspend fun onEnded(current: ListeningSession) {
        stopTicker()
        current.finish(current.durationMs ?: engine.positionMs)
        val next = Autoplay.next(graph.freshRotation(), current.context, current.key)
        session = null
        if (next == null) stopWithoutSaving() else start(next.key, current.context)
    }

    private suspend fun tick() {
        val position = engine.positionMs
        _state.update { it?.copy(positionMs = position) }
        val current = session ?: return
        if (current.isSaveDue()) current.save(position, stopped = false, continueFromMs = position)
    }

    private fun stopTicker() {
        ticker.stop()
        _state.update { it?.copy(positionMs = engine.positionMs) }
    }
}

/** Runs [onTick] every [periodMs] between [start] and [stop]. */
private class Ticker(
    private val scope: CoroutineScope,
    private val periodMs: Long,
    private val onTick: suspend () -> Unit,
) {
    private var job: Job? = null

    fun start() {
        stop()
        job = scope.launch {
            while (isActive) {
                delay(periodMs)
                onTick()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
