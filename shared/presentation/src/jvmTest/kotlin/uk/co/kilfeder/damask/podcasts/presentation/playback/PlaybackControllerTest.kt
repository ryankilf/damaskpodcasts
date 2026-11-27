package uk.co.kilfeder.damask.podcasts.presentation.playback

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import uk.co.kilfeder.damask.podcasts.core.data.desktopDriverFactory
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val MINUTE = 60_000L
private const val HOUR = 60 * MINUTE
private const val START_MS = 1_790_000_000_000L

/** Plays nothing; tests drive it by setting the position and sending events. */
private class FakeEngine : AudioEngine {
    override val available = true
    override var positionMs = 0L
    override var durationMs: Long? = null
    var emit: (EngineEvent) -> Unit = {}
    val loads = mutableListOf<MediaRequest>()
    var cleared = 0

    override fun setListener(listener: (EngineEvent) -> Unit) {
        emit = listener
    }

    override fun load(request: MediaRequest, playWhenReady: Boolean) {
        loads += request
        positionMs = request.startMs
    }

    override fun play() = emit(EngineEvent.PlayingChanged(true))

    override fun pause() = emit(EngineEvent.PlayingChanged(false))

    override fun seekTo(positionMs: Long) {
        val from = this.positionMs
        this.positionMs = positionMs
        emit(EngineEvent.Seeked(from, positionMs))
    }

    override fun setSpeed(speed: Float) = Unit

    override fun clear() {
        cleared++
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackControllerTest {
    private val file = File.createTempFile("damask-playback", ".db").also { it.delete() }
    private var now = START_MS
    private val engine = FakeEngine()
    private val graph = AppGraph(
        desktopDriverFactory(file),
        wallClockMs = { now },
        engineFactory = { engine },
        mainDispatcher = Dispatchers.Default.limitedParallelism(1),
    )
    private val playback = graph.playback

    @AfterTest
    fun cleanUp() {
        file.delete()
    }

    private fun seed() {
        val db = graph.db
        listOf("history", "news").forEach { db.categoryQueries.upsert(it, it, is_system = 0, deleted = 0, hlc = 1) }
        mapOf("hist" to "history", "pol" to "news").forEach { (show, category) ->
            val url = "https://$show.example/feed"
            db.showQueries.upsert(show, url, url, category, null, null, 10, 1, 1)
        }
        listOf("hist" to "h1", "pol" to "p1").forEach { (show, guid) ->
            val published = START_MS - 86_400_000
            val audio = "https://cdn.example/$guid.mp3"
            db.feedQueries.upsertEpisode(show, guid, "Episode $guid", published, audio, null, HOUR, null)
        }
    }

    private fun position(show: String, guid: String) =
        graph.db.episodeStateQueries.selectPosition(show, guid).executeAsOneOrNull()

    private fun played(show: String, guid: String) =
        graph.db.episodeStateQueries.selectPlayed(show, guid).executeAsOneOrNull()

    private suspend fun awaitThat(message: String, check: () -> Boolean) {
        withTimeout(5_000) { while (!check()) delay(20) }
        assertTrue(check(), message)
    }

    private suspend fun startPlaying(id: EpisodeId, context: PlayContext?) {
        playback.play(id, context)
        awaitThat("loaded") { playback.state.value?.id == id }
        engine.emit(EngineEvent.Prepared(HOUR))
        engine.emit(EngineEvent.PlayingChanged(true))
        awaitThat("playing") { playback.state.value?.isPlaying == true }
    }

    @Test
    fun pausingSavesPositionContextAndListeningTime() = runBlocking {
        seed()
        startPlaying(EpisodeId("hist", "h1"), PlayContext.UpNext)
        assertEquals("https://cdn.example/h1.mp3", engine.loads.single().uri)
        now += 10 * MINUTE
        engine.positionMs = 10 * MINUTE
        engine.pause()
        awaitThat("position saved") { position("hist", "h1")?.position_ms == 10 * MINUTE }
        assertEquals("UP_NEXT", position("hist", "h1")?.play_context)
        val stats = graph.db.listenStatsQueries.selectByDevice(graph.db.deviceQueries.selectDevice().executeAsOne().id)
            .executeAsList().single()
        assertEquals("history", stats.category_id)
        assertEquals(10 * MINUTE, stats.wall_ms)
        assertEquals(10 * MINUTE, stats.content_ms)
    }

    @Test
    fun resumingUsesTheSavedPositionAndContext() = runBlocking {
        seed()
        startPlaying(EpisodeId("hist", "h1"), PlayContext.UpNext)
        engine.positionMs = 20 * MINUTE
        engine.pause()
        awaitThat("saved") { position("hist", "h1")?.position_ms == 20 * MINUTE }
        startPlaying(EpisodeId("pol", "p1"), PlayContext.None)
        playback.play(EpisodeId("hist", "h1"), context = null)
        awaitThat("back on h1") { playback.state.value?.id == EpisodeId("hist", "h1") }
        assertEquals(20 * MINUTE, engine.loads.last().startMs)
    }

    @Test
    fun stoppingInTheLastTwoMinutesFinishesTheEpisode() = runBlocking {
        seed()
        startPlaying(EpisodeId("hist", "h1"), PlayContext.None)
        engine.positionMs = HOUR - MINUTE
        engine.pause()
        awaitThat("finished") { played("hist", "h1")?.played_by == "FINISHED" }
    }

    @Test
    fun reachingTheEndFinishesAndAutoplaysTheNextUpNextEpisode() = runBlocking {
        seed()
        startPlaying(EpisodeId("hist", "h1"), PlayContext.UpNext)
        engine.positionMs = HOUR
        engine.emit(EngineEvent.Ended)
        awaitThat("next loaded") { playback.state.value?.id == EpisodeId("pol", "p1") }
        assertEquals("FINISHED", played("hist", "h1")?.played_by)
        assertEquals("https://cdn.example/p1.mp3", engine.loads.last().uri)
    }

    @Test
    fun reachingTheEndWithNoContextStops() = runBlocking {
        seed()
        startPlaying(EpisodeId("hist", "h1"), PlayContext.None)
        engine.emit(EngineEvent.Ended)
        awaitThat("stopped") { playback.state.value == null }
        assertEquals(1, engine.cleared)
    }

    @Test
    fun seekingDoesNotCountSkippedTime() = runBlocking {
        seed()
        startPlaying(EpisodeId("hist", "h1"), PlayContext.None)
        now += MINUTE
        engine.positionMs = MINUTE
        engine.seekTo(30 * MINUTE)
        awaitThat("seek saved") { position("hist", "h1")?.position_ms == 30 * MINUTE }
        now += MINUTE
        engine.positionMs = 31 * MINUTE
        engine.pause()
        awaitThat("pause saved") { position("hist", "h1")?.position_ms == 31 * MINUTE }
        val stats = graph.db.listenStatsQueries.selectByDevice(graph.db.deviceQueries.selectDevice().executeAsOne().id)
            .executeAsList().single()
        assertEquals(2 * MINUTE, stats.content_ms)
    }

    @Test
    fun markingThePlayingEpisodePlayedStopsWithoutSavingOverIt() = runBlocking {
        seed()
        startPlaying(EpisodeId("hist", "h1"), PlayContext.None)
        playback.onMarkedPlayed(EpisodeId("hist", "h1"))
        awaitThat("stopped") { playback.state.value == null }
        assertNull(position("hist", "h1"))
    }
}
