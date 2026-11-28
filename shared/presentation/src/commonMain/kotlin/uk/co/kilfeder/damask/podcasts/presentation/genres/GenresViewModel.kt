package uk.co.kilfeder.damask.podcasts.presentation.genres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.STOP_TIMEOUT_MS
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import uk.co.kilfeder.damask.podcasts.presentation.format.Formats
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised

data class GenreRowUi(
    val categoryId: String,
    val name: String,
    val showCount: Int,
    /** null = never listened (these sort first). */
    val lastListened: Summarised?,
    /** The genre heard least recently, flagged "Longest unplayed" (only when there's a choice). */
    val stalest: Boolean = false,
    /** Up to [COVER_COUNT] covers, front first: the shows that come next in this genre's queue. */
    val covers: List<CoverUi>,
)

/** One show's cover in a genre row's stack. */
data class CoverUi(val url: String?, val fallback: ArtworkFallback)

/** How many covers a genre row stacks. */
const val COVER_COUNT = 3

data class GenresUiState(
    val loading: Boolean = true,
    val genres: List<GenreRowUi> = emptyList(),
)

/** Genres: categories by LRP. Empty categories are hidden (see docs/decisions.md). */
class GenresViewModel internal constructor(graph: AppGraph) : ViewModel() {
    val uiState: StateFlow<GenresUiState> = combine(graph.library, graph.rotation) { library, rotation ->
        val now = graph.wallClockMs()
        val rows = rotation.genres().mapNotNull { category ->
            val shows = library.showsByCategory[category.id].orEmpty()
            if (shows.isEmpty()) return@mapNotNull null
            GenreRowUi(
                categoryId = category.id.value,
                name = category.name,
                showCount = shows.size,
                lastListened = rotation.history.lastListened(category.id)?.let { Formats.lastPlayed(it.wallMs, now) },
                covers = rotation.coverShows(category.id, COVER_COUNT)
                    .map { CoverUi(it.imageUrl, ArtworkFallback.of(it.id.value, it.title)) },
            )
        }
        // Genres come least recently played first, so the first is the one to flag.
        val flagged = rows.mapIndexed { i, row -> row.copy(stalest = i == 0 && rows.size > 1) }
        GenresUiState(loading = false, genres = flagged)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), GenresUiState())
}
