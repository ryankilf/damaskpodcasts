package uk.co.kilfeder.damask.podcasts.presentation.genres

import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph

/**
 * Changing a genre: its colour, its name, deleting it (its shows move to Uncategorised), or
 * making a new one. Manage genres and a genre's own page both offer these (by [GenreEditor]).
 */
interface GenreEdits {
    fun onColourChange(id: String, hue: GenreHue)
    fun onRename(id: String, name: String)
    fun onDelete(id: String)
    fun onCreate(name: String, hue: GenreHue)
}

/**
 * The one implementation of [GenreEdits], which ViewModels delegate to. Writes run on the app's
 * scope, not the screen's, so deleting a genre from its own page finishes after the page closes.
 */
internal class GenreEditor(private val graph: AppGraph) : GenreEdits {
    override fun onColourChange(id: String, hue: GenreHue) {
        val colour = hue.toColour() ?: return
        write { graph.genreSettings.setColour(CategoryId(id), colour) }
    }

    override fun onRename(id: String, name: String) {
        if (name.isBlank()) return
        write { graph.genreSettings.rename(CategoryId(id), name) }
    }

    override fun onDelete(id: String) = write { graph.genreSettings.delete(CategoryId(id)) }

    override fun onCreate(name: String, hue: GenreHue) {
        val colour = hue.toColour() ?: return
        if (name.isBlank()) return
        write { graph.genreSettings.create(name, colour) }
    }

    private fun write(block: () -> Unit) {
        graph.scope.launch { graph.onDb(block) }
    }
}
