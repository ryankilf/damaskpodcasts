package uk.co.kilfeder.damask.podcasts.presentation.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.feed.FeedParseException
import uk.co.kilfeder.damask.podcasts.core.importer.ImportCandidate
import uk.co.kilfeder.damask.podcasts.core.importer.ImportListener
import uk.co.kilfeder.damask.podcasts.core.importer.OpmlEntry
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph

data class CategoryOptionUi(val id: String, val name: String)

/**
 * The categories matching a search: case-insensitive, and "&" matches "and" both ways, so
 * "health and" finds "Health & Fitness". An empty query matches everything.
 */
fun List<CategoryOptionUi>.matching(query: String): List<CategoryOptionUi> {
    val wanted = searchKey(query)
    return if (wanted.isEmpty()) this else filter { searchKey(it.name).contains(wanted) }
}

private fun searchKey(text: String): String =
    text.lowercase().replace("&", " and ").split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(" ")

data class ImportRowUi(
    val index: Int,
    val title: String,
    val artworkUrl: String?,
    val categoryId: String,
    /** Why the feed couldn't be read; it can still be subscribed and is retried on refresh. */
    val problem: String?,
    /** Ticked by default; unticked shows aren't subscribed. */
    val selected: Boolean = true,
)

enum class TileStatus {
    /** Not read (or, while saving, not saved) yet. */
    WAITING,
    READ,
    FAILED,

    /** Subscribed, while saving. */
    SAVED,
}

/** One show in the import grid. [name] is null when neither the OPML nor the feed names it. */
data class ImportTileUi(
    /** 1-based position in the file, for unnamed shows ("Show 12 of 78"). */
    val position: Int,
    val name: String?,
    val artworkUrl: String?,
    val status: TileStatus,
)

/** Import: choose an OPML file, confirm or change every show's category, subscribe. */
sealed interface ImportUiState {
    data object ChooseFile : ImportUiState

    /** Fetching feeds: [done] of [total] read so far; [tiles] fill in as each feed is read. */
    data class Reading(
        val done: Int = 0,
        val total: Int = 0,
        val tiles: List<ImportTileUi> = emptyList(),
    ) : ImportUiState

    data class Review(val rows: List<ImportRowUi>, val categories: List<CategoryOptionUi>) : ImportUiState {
        val selectedCount: Int get() = rows.count { it.selected }
        val allSelected: Boolean get() = rows.isNotEmpty() && rows.all { it.selected }
    }

    /** Saving shows and their episodes: [done] of [total]; each ticked show's tile brightens as it's saved. */
    data class Subscribing(
        val done: Int = 0,
        val total: Int = 0,
        val tiles: List<ImportTileUi> = emptyList(),
    ) : ImportUiState

    data class Done(val count: Int) : ImportUiState

    data class Failed(val message: String) : ImportUiState
}

class ImportViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    private val state = MutableStateFlow<ImportUiState>(ImportUiState.ChooseFile)
    val uiState: StateFlow<ImportUiState> = state.asStateFlow()

    private var candidates: List<ImportCandidate> = emptyList()

    fun onFileChosen(opml: String) {
        state.value = ImportUiState.Reading()
        viewModelScope.launch {
            state.value = try {
                candidates = graph.onDbSuspend { graph.importer.prepare(opml, listener = TileTracker()) }
                review(candidates.map { it.suggestedCategory.value })
            } catch (e: FeedParseException) {
                ImportUiState.Failed(e.message ?: "That file isn't an OPML export")
            }
        }
    }

    fun onCategoryChanged(index: Int, categoryId: String) {
        val current = state.value as? ImportUiState.Review ?: return
        state.value = current.copy(
            rows = current.rows.map { if (it.index == index) it.copy(categoryId = categoryId) else it },
        )
    }

    fun onSelectionChanged(index: Int, selected: Boolean) {
        val current = state.value as? ImportUiState.Review ?: return
        state.value = current.copy(
            rows = current.rows.map { if (it.index == index) it.copy(selected = selected) else it },
        )
    }

    /** Tick or untick every show at once. */
    fun onSelectAllChanged(selected: Boolean) {
        val current = state.value as? ImportUiState.Review ?: return
        state.value = current.copy(rows = current.rows.map { it.copy(selected = selected) })
    }

    fun onConfirm() {
        val review = state.value as? ImportUiState.Review ?: return
        if (review.selectedCount == 0) return
        val selected = review.rows.filter { it.selected }
        val tiles = selected.mapIndexed { i, row ->
            ImportTileUi(i + 1, name = row.title.ifBlank { null }, artworkUrl = row.artworkUrl, TileStatus.WAITING)
        }
        state.value = ImportUiState.Subscribing(0, selected.size, tiles)
        viewModelScope.launch {
            val choices = selected.map { candidates[it.index] to CategoryId(it.categoryId) }
            graph.onDb {
                graph.importer.confirm(choices) { done, total ->
                    val saved = tiles.mapIndexed { i, tile ->
                        if (i < done) tile.copy(status = TileStatus.SAVED) else tile
                    }
                    state.value = ImportUiState.Subscribing(done, total, saved)
                }
            }
            state.value = ImportUiState.Done(choices.size)
        }
    }

    fun onStartAgain() {
        candidates = emptyList()
        state.value = ImportUiState.ChooseFile
    }

    /** Turns importer events into Reading states: one tile per show, in file order. */
    private inner class TileTracker : ImportListener {
        private var tiles = emptyList<ImportTileUi>()
        private var done = 0

        override fun onStarted(entries: List<OpmlEntry>) {
            tiles = entries.mapIndexed { i, entry ->
                ImportTileUi(
                    position = i + 1,
                    name = entry.title.ifBlank { null },
                    artworkUrl = null,
                    status = TileStatus.WAITING,
                )
            }
            publish()
        }

        override fun onFeedRead(index: Int, candidate: ImportCandidate) {
            val read = candidate.feed != null
            tiles = tiles.toMutableList().also {
                it[index] = it[index].copy(
                    name = candidate.title.ifBlank { null },
                    artworkUrl = candidate.imageUrl,
                    status = if (read) TileStatus.READ else TileStatus.FAILED,
                )
            }
            done++
            publish()
        }

        private fun publish() {
            state.value = ImportUiState.Reading(done, tiles.size, tiles)
        }
    }

    private suspend fun review(categoryIds: List<String>): ImportUiState.Review {
        val categories = graph.library.first().categories
            .sortedWith(compareBy({ it.isSystem }, { it.name.lowercase() }))
            .map { CategoryOptionUi(it.id.value, it.name) }
        val rows = candidates.mapIndexed { index, c ->
            ImportRowUi(index, c.title, c.imageUrl, categoryIds[index], c.problem)
        }
        return ImportUiState.Review(rows, categories)
    }
}
