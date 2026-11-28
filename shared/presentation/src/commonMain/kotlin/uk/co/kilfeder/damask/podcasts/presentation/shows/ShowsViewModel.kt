package uk.co.kilfeder.damask.podcasts.presentation.shows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import uk.co.kilfeder.damask.podcasts.core.model.Show
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.STOP_TIMEOUT_MS
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import uk.co.kilfeder.damask.podcasts.presentation.format.Formats
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised

/** One subscribed show (design hand-off › ShowListPane). */
data class ShowRowUi(
    val showId: String,
    /** The feed's title; blank until the feed has been read. */
    val title: String,
    val artworkUrl: String?,
    val fallback: ArtworkFallback,
    /** Its genre's name. */
    val genre: String?,
    /** null = never listened. */
    val lastListened: Summarised?,
)

data class ShowsUiState(
    val loading: Boolean = true,
    /** Subscribed shows matching [query], A–Z. */
    val shows: List<ShowRowUi> = emptyList(),
    /** How many shows you subscribe to, matching or not. */
    val total: Int = 0,
    /** What's typed in "Search your shows"; blank = every show. */
    val query: String = "",
)

/** Shows: every subscribed show A–Z, to go straight to one (spec › Screens). */
class ShowsViewModel internal constructor(graph: AppGraph) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<ShowsUiState> = combine(graph.library, graph.rotation, query) { library, rotation, q ->
        val now = graph.wallClockMs()
        val needle = q.trim()
        ShowsUiState(
            loading = false,
            shows = library.subscribedShows
                .filter { needle.isEmpty() || it.title.contains(needle, ignoreCase = true) }
                .sortedWith(compareBy({ it.sortKey() }, { it.id.value }))
                .map { it.toRow(library, rotation, now) },
            total = library.subscribedShows.size,
            query = q,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ShowsUiState())

    /** "Search your shows": filters by name as you type. */
    fun onQueryChange(text: String) {
        query.value = text
    }
}

/** A–Z ignoring case and a leading "The "; shows with no name yet (unread feeds) go last. */
private fun Show.sortKey(): String = title.lowercase().removePrefix("the ").trim().ifEmpty { LAST }

/** Sorts after any name. */
private const val LAST = "\uFFFF"

internal fun Show.toRow(library: Library, rotation: Rotation, nowMs: Long) = ShowRowUi(
    showId = id.value,
    title = title,
    artworkUrl = imageUrl,
    fallback = ArtworkFallback.of(id.value, title),
    genre = library.effectiveCategory(this)?.let { id -> library.categories.firstOrNull { it.id == id }?.name },
    lastListened = rotation.history.lastListened(id)?.let { Formats.lastPlayed(it.wallMs, nowMs) },
)
