package uk.co.kilfeder.damask.podcasts.presentation.episodes

import uk.co.kilfeder.damask.podcasts.presentation.playback.toContext
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaySource
import uk.co.kilfeder.damask.podcasts.presentation.format.fraction
import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.format.Formats
import uk.co.kilfeder.damask.podcasts.presentation.playback.NowPlaying
import kotlin.math.roundToInt

/** The position bar under the artwork: "22:06" played, "−32:04" left. */
data class PlayerPositionUi(
    val played: String,
    /** null when the length isn't known. */
    val remaining: String?,
    /** 0..1. */
    val fraction: Float,
    /** For screen readers: "22:06 of 54:10, 41%". */
    val description: String,
)

/** A link from the show notes, listed under Links. [host] tells apart links with the same text. */
data class NotesLink(val label: String, val url: String, val host: String)

/**
 * One episode, laid out like the design's Now Playing (NowPlayingPane, NowPlayingNotes), as an
 * ordinary pushed screen for now. Playback isn't built yet, so the transport controls are shown
 * but disabled.
 */
data class EpisodeUiState(
    val loading: Boolean = true,
    /** null once loaded means the episode no longer exists (e.g. pruned and never played). */
    val episode: EpisodeRowUi? = null,
    /** The show's genre, for the chip; null when it has none. */
    val genre: String? = null,
    val genreId: String? = null,
    val position: PlayerPositionUi? = null,
    /** "1×": the speed button's label. */
    val speed: String = "1×",
    /** false where the platform has no player yet: controls show disabled. */
    val playbackAvailable: Boolean = false,
    /** This episode is the one loaded in the player. */
    val isCurrent: Boolean = false,
    val isPlaying: Boolean = false,
    val buffering: Boolean = false,
    /** Why it couldn't play, in plain English. */
    val error: String? = null,
    val isPlayed: Boolean = false,
    /** Show notes as styled blocks ([ShowNotes]); empty when the feed gave none. */
    val notes: List<NotesBlock> = emptyList(),
    /** The first line of the notes, for the peek under the controls. */
    val notesPreview: String? = null,
    val links: List<NotesLink> = emptyList(),
)

class EpisodeViewModel internal constructor(
    private val graph: AppGraph,
    private val id: EpisodeId,
) : ViewModel() {
    val uiState: StateFlow<EpisodeUiState> =
        combine(graph.rotation, graph.playback.state) { rotation, now ->
            val library = rotation.library
            val key = id.toKey()
            val episode = library.episode(key)
            if (episode == null) {
                EpisodeUiState(loading = false)
            } else {
                val notes = ShowNotes.parse(graph.onDb { graph.lookup.notes(key) })
                withPlayback(stateFor(episode, library, rotation, notes), now?.takeIf { it.id == id })
            }
        }.stateInUi(viewModelScope, EpisodeUiState())

    /** Plays or pauses this episode; starting it uses [source], else the context saved with it. */
    fun onPlayPause(source: PlaySource? = null) {
        graph.playback.playOrToggle(id, source?.toContext())
    }

    fun onSkipBack() = whenCurrent { graph.playback.skipBack() }

    fun onSkipForward() = whenCurrent { graph.playback.skipForward() }

    /** [fraction] 0..1 of the length. */
    fun onSeek(fraction: Float) = whenCurrent { now ->
        now.durationMs?.let { graph.playback.seekTo((it * fraction.coerceIn(0f, 1f)).toLong()) }
    }

    /** Steps through [SPEEDS]. */
    fun onCycleSpeed() {
        val current = graph.playback.state.value?.speed ?: 1f
        val next = SPEEDS.firstOrNull { it > current + SPEED_EPSILON } ?: SPEEDS.first()
        graph.playback.setSpeed(next)
    }

    fun onMarkPlayed() {
        viewModelScope.launch { graph.markPlayed(id) }
    }

    /** Also resets the position, per the spec. */
    fun onMarkUnplayed() {
        viewModelScope.launch { graph.markUnplayed(id) }
    }

    /** The Played tick: marks played or unplayed. */
    fun onPlayedChange(played: Boolean) = if (played) onMarkPlayed() else onMarkUnplayed()

    private fun stateFor(
        episode: Episode,
        library: Library,
        rotation: Rotation,
        notes: List<NotesBlock>,
    ): EpisodeUiState {
        val category = library.showsById[episode.key.showId]?.let(library::categoryOf)
        val positionMs = episode.position?.positionMs?.takeIf { rotation.isInProgress(episode) } ?: 0
        return EpisodeUiState(
            loading = false,
            episode = episode.toRow(library, rotation, graph.wallClockMs()),
            genre = category?.name,
            genreId = category?.id?.value,
            position = position(positionMs, episode.durationMs),
            isPlayed = episode.isPlayed,
            notes = notes,
            notesPreview = notes.firstOrNull()?.spans?.joinToString("") { it.text }?.lineSequence()?.firstOrNull(),
            links = ShowNotes.links(notes),
        )
    }

    private fun whenCurrent(block: (NowPlaying) -> Unit) {
        graph.playback.state.value?.takeIf { it.id == id }?.let(block)
    }

    private fun withPlayback(state: EpisodeUiState, now: NowPlaying?): EpisodeUiState {
        val base = state.copy(playbackAvailable = graph.playback.available)
        if (now == null) return base
        return base.copy(
            isCurrent = true,
            isPlaying = now.isPlaying,
            buffering = now.buffering,
            error = now.error,
            position = position(now.positionMs, now.durationMs),
            speed = speedLabel(now.speed),
        )
    }

    private companion object {
        const val SPEED_EPSILON = 0.01f

        /** The speed button steps through these, then wraps to the slowest. */
        val SPEEDS = listOf(0.8f, 1f, 1.2f, 1.5f, 1.8f, 2f)
    }
}

private fun speedLabel(speed: Float): String {
    val tenths = (speed * TENTHS).roundToInt()
    val text = if (tenths % TENTHS == 0) "${tenths / TENTHS}" else "${tenths / TENTHS}.${tenths % TENTHS}"
    return "$text×"
}

private fun position(positionMs: Long, totalMs: Long?): PlayerPositionUi {
    val played = Formats.clock(positionMs)
    if (totalMs == null || totalMs <= 0) return PlayerPositionUi(played, null, 0f, played)
    val fraction = fraction(positionMs, totalMs) ?: 0f
    val percent = (fraction * PERCENT).toInt()
    return PlayerPositionUi(
        played = played,
        remaining = "−" + Formats.clock(totalMs - positionMs),
        fraction = fraction,
        description = "$played of ${Formats.clock(totalMs)}, $percent%",
    )
}

private const val PERCENT = 100
private const val TENTHS = 10
