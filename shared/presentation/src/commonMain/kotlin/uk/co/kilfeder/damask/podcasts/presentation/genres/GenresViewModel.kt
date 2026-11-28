package uk.co.kilfeder.damask.podcasts.presentation.genres

import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import kotlinx.coroutines.flow.map
import uk.co.kilfeder.damask.podcasts.presentation.format.lastPlayed
import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised

data class GenreRowUi(
    val categoryId: String,
    val name: String,
    val showCount: Int,
    /** null = never listened (these sort first). */
    val lastListened: Summarised?,
    /** The genre's colour: the row's tint (and ring, when selected on tablets). */
    val hue: GenreHue,
    /** Up to [COVER_COUNT] covers, front first: the shows that come next in this genre's queue. */
    val covers: List<CoverUi>,
)

/** One show's cover in a genre row's stack. */
data class CoverUi(val url: String?, val fallback: ArtworkFallback)

/** Up to [COVER_COUNT] covers of the shows next in a genre's queue, front first. */
internal fun Rotation.covers(id: CategoryId): List<CoverUi> =
    coverShows(id, COVER_COUNT).map { CoverUi(it.imageUrl, ArtworkFallback.of(it.title, library.hueOf(it))) }

/** How many covers a genre row stacks. */
const val COVER_COUNT = 3

data class GenresUiState(
    val loading: Boolean = true,
    val genres: List<GenreRowUi> = emptyList(),
)

/** Genres: categories by LRP. Empty categories are hidden (see docs/decisions.md). */
class GenresViewModel internal constructor(graph: AppGraph) : ViewModel() {
    val uiState: StateFlow<GenresUiState> = graph.rotation.map { rotation ->
        val library = rotation.library
        val now = graph.wallClockMs()
        val rows = rotation.genres().mapNotNull { category ->
            val shows = library.showsIn(category.id)
            if (shows.isEmpty()) return@mapNotNull null
            GenreRowUi(
                categoryId = category.id.value,
                name = category.name,
                showCount = shows.size,
                lastListened = rotation.lastPlayed(category.id, now),
                covers = rotation.covers(category.id),
                hue = category.colour.toHue(),
            )
        }
        GenresUiState(loading = false, genres = rows)
    }.stateInUi(viewModelScope, GenresUiState())
}
