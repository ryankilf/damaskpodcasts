package uk.co.kilfeder.damask.podcasts.presentation.episodes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.PlayedBy
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.STOP_TIMEOUT_MS
import uk.co.kilfeder.damask.podcasts.presentation.format.Formats

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
        combine(graph.library, graph.rotation) { library, rotation ->
            val key = id.toKey()
            val episode = library.episodesOf(key.showId).firstOrNull { it.key == key }
            if (episode == null) {
                EpisodeUiState(loading = false)
            } else {
                stateFor(episode, library, rotation, ShowNotes.parse(graph.onDb { graph.notes.of(key) }))
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), EpisodeUiState())

    fun onMarkPlayed() {
        viewModelScope.launch { graph.onDb { graph.userState.markPlayed(id.toKey(), PlayedBy.MARKED) } }
    }

    /** Also resets the position, per the spec. */
    fun onMarkUnplayed() {
        viewModelScope.launch { graph.onDb { graph.userState.markUnplayed(id.toKey()) } }
    }

    private fun stateFor(
        episode: Episode,
        library: Library,
        rotation: Rotation,
        notes: List<NotesBlock>,
    ): EpisodeUiState {
        val category = library.showsById[episode.key.showId]
            ?.let(library::effectiveCategory)
            ?.let { id -> library.categories.firstOrNull { it.id == id } }
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
            links = links(notes),
        )
    }

    private fun position(positionMs: Long, totalMs: Long?): PlayerPositionUi {
        val played = Formats.clock(positionMs)
        if (totalMs == null || totalMs <= 0) return PlayerPositionUi(played, null, 0f, played)
        val fraction = (positionMs.toFloat() / totalMs).coerceIn(0f, 1f)
        val percent = (fraction * PERCENT).toInt()
        return PlayerPositionUi(
            played = played,
            remaining = "−" + Formats.clock(totalMs - positionMs),
            fraction = fraction,
            description = "$played of ${Formats.clock(totalMs)}, $percent%",
        )
    }

    /** Every distinct link in the notes, in order, labelled with its text and host. */
    private fun links(notes: List<NotesBlock>): List<NotesLink> = notes
        .flatMap { it.spans }
        .mapNotNull { span -> span.link?.let { NotesLink(span.text.trim().ifEmpty { it }, it, host(it)) } }
        .distinctBy { it.url }

    private fun host(url: String): String = url
        .substringAfter("://", missingDelimiterValue = url.substringAfter(':'))
        .substringBefore('/')
        .substringBefore('?')
        .removePrefix("www.")

    private companion object {
        const val PERCENT = 100
    }
}
