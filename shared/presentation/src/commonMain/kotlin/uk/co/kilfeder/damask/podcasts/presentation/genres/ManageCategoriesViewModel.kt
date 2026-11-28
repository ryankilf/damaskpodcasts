package uk.co.kilfeder.damask.podcasts.presentation.genres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.GenreColour
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi

/** One genre on Manage categories: its colour, name and how many shows it has. */
data class ManagedCategoryUi(
    val id: String,
    val name: String,
    val hue: GenreHue,
    val showCount: Int,
    /** Uncategorised: always grey, can't be renamed, recoloured or deleted. */
    val isSystem: Boolean,
)

data class ManageCategoriesUiState(
    val loading: Boolean = true,
    /** A–Z, Uncategorised last. */
    val categories: List<ManagedCategoryUi> = emptyList(),
    /** The colour a new category starts with: the least used. */
    val nextHue: GenreHue = GenreHue.RED,
)

/** The colours a user can pick (grey is only for Uncategorised). */
val PickableHues: List<GenreHue> = GenreHue.entries - GenreHue.GREY

/**
 * Manage categories (design hand-off › ManageCategories): recolour, rename and delete genres, and
 * set new ones up ahead of time. Deleting a genre moves its shows to Uncategorised.
 */
class ManageCategoriesViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    val uiState: StateFlow<ManageCategoriesUiState> = graph.rotation.map { rotation ->
        val library = rotation.library
        ManageCategoriesUiState(
            loading = false,
            categories = library.categories
                .sortedWith(compareBy({ it.isSystem }, { it.name.lowercase() }))
                .map {
                    ManagedCategoryUi(it.id.value, it.name, it.colour.toHue(), library.showsIn(it.id).size, it.isSystem)
                },
            nextHue = GenreColour.leastUsed(library.categories.mapNotNull { it.colour }).toHue(),
        )
    }.stateInUi(viewModelScope, ManageCategoriesUiState())

    fun onColourChange(id: String, hue: GenreHue) {
        val colour = hue.toColour() ?: return
        write { graph.genreSettings.setColour(CategoryId(id), colour) }
    }

    fun onRename(id: String, name: String) {
        if (name.isBlank()) return
        write { graph.genreSettings.rename(CategoryId(id), name) }
    }

    fun onDelete(id: String) = write { graph.genreSettings.delete(CategoryId(id)) }

    fun onCreate(name: String, hue: GenreHue) {
        val colour = hue.toColour() ?: return
        if (name.isBlank()) return
        write { graph.genreSettings.create(name, colour) }
    }

    private fun write(block: () -> Unit) {
        viewModelScope.launch { graph.onDb(block) }
    }
}
