package uk.co.kilfeder.damask.podcasts.presentation.genres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import uk.co.kilfeder.damask.podcasts.core.model.GenreColour
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi

/** One genre on Manage genres: its colour, name and how many shows it has. */
data class ManagedGenreUi(
    val id: String,
    val name: String,
    val hue: GenreHue,
    val showCount: Int,
    /** Uncategorised: always grey, can't be renamed, recoloured or deleted. */
    val isSystem: Boolean,
)

data class ManageGenresUiState(
    val loading: Boolean = true,
    /** A–Z, Uncategorised last. */
    val categories: List<ManagedGenreUi> = emptyList(),
    /** The colour a new category starts with: the least used. */
    val nextHue: GenreHue = GenreHue.RED,
)

/** The colours a user can pick (grey is only for Uncategorised). */
val PickableHues: List<GenreHue> = GenreHue.entries - GenreHue.GREY

/**
 * Manage genres (design hand-off › ManageCategories): recolour, rename and delete genres, and
 * set new ones up ahead of time. Deleting a genre moves its shows to Uncategorised.
 */
class ManageGenresViewModel internal constructor(
    private val graph: AppGraph,
) : ViewModel(), GenreEdits by GenreEditor(graph) {
    val uiState: StateFlow<ManageGenresUiState> = graph.rotation.map { rotation ->
        val library = rotation.library
        ManageGenresUiState(
            loading = false,
            categories = library.categories
                .sortedWith(compareBy({ it.isSystem }, { it.name.lowercase() }))
                .map {
                    ManagedGenreUi(it.id.value, it.name, it.colour.toHue(), library.showsIn(it.id).size, it.isSystem)
                },
            nextHue = GenreColour.leastUsed(library.categories.mapNotNull { it.colour }).toHue(),
        )
    }.stateInUi(viewModelScope, ManageGenresUiState())
}
