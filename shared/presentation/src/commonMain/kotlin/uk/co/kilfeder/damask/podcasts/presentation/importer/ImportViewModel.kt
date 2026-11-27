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
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph

data class CategoryOptionUi(val id: String, val name: String)

data class ImportRowUi(
    val index: Int,
    val title: String,
    val artworkUrl: String?,
    val categoryId: String,
    /** Why the feed couldn't be read; it can still be subscribed and is retried on refresh. */
    val problem: String?,
)

/** Import: choose an OPML file, confirm or change every show's category, subscribe. */
sealed interface ImportUiState {
    data object ChooseFile : ImportUiState

    /** Fetching feeds: [done] of [total] read so far. */
    data class Reading(val done: Int = 0, val total: Int = 0) : ImportUiState

    data class Review(val rows: List<ImportRowUi>, val categories: List<CategoryOptionUi>) : ImportUiState

    /** Saving shows and their episodes: [done] of [total]. */
    data class Subscribing(val done: Int = 0, val total: Int = 0) : ImportUiState

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
                candidates = graph.onDbSuspend {
                    graph.importer.prepare(opml) { done, total -> state.value = ImportUiState.Reading(done, total) }
                }
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

    fun onConfirm() {
        val review = state.value as? ImportUiState.Review ?: return
        state.value = ImportUiState.Subscribing(0, review.rows.size)
        viewModelScope.launch {
            val choices = review.rows.map { candidates[it.index] to CategoryId(it.categoryId) }
            graph.onDb {
                graph.importer.confirm(choices) { done, total -> state.value = ImportUiState.Subscribing(done, total) }
            }
            state.value = ImportUiState.Done(choices.size)
        }
    }

    fun onStartAgain() {
        candidates = emptyList()
        state.value = ImportUiState.ChooseFile
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
