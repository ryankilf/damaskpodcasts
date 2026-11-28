package uk.co.kilfeder.damask.podcasts.presentation.importer

import uk.co.kilfeder.damask.podcasts.presentation.genres.categoryOptions
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
import uk.co.kilfeder.damask.podcasts.core.sync.ShowIds
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import uk.co.kilfeder.damask.podcasts.presentation.genres.CategoryOptionUi

data class ImportRowUi(
    val index: Int,
    val title: String,
    val artworkUrl: String?,
    val fallback: ArtworkFallback,
    val categoryId: String,
    /** Why the feed couldn't be read; it can still be subscribed and is retried on refresh. */
    val problem: String?,
    /**
     * Unticked shows aren't subscribed. Ticked by default, except shows whose feed couldn't be
     * read: those start unticked with the reason beside them, and can still be ticked.
     */
    val selected: Boolean = true,
)

/** The most recent show to be read (or saved), for the import's cover card. */
data class LatestShowUi(
    /** null when neither the OPML nor the feed names it. */
    val name: String?,
    val artworkUrl: String,
)

/** Import: choose an OPML file, confirm or change every show's category, subscribe. */
sealed interface ImportUiState {
    data object ChooseFile : ImportUiState

    /** Fetching feeds: [done] of [total] read so far; [latest] is the newest one with artwork. */
    data class Reading(
        val done: Int = 0,
        val total: Int = 0,
        val latest: LatestShowUi? = null,
    ) : ImportUiState

    data class Review(val rows: List<ImportRowUi>, val categories: List<CategoryOptionUi>) : ImportUiState {
        val selectedCount: Int get() = rows.count { it.selected }
        val allSelected: Boolean get() = rows.isNotEmpty() && rows.all { it.selected }
    }

    /** Saving shows and their episodes: [done] of [total]; [latest] is the newest saved with artwork. */
    data class Subscribing(
        val done: Int = 0,
        val total: Int = 0,
        val latest: LatestShowUi? = null,
    ) : ImportUiState

    /** Finished: the screen hands [count] to the app (which goes to Home) and resets. */
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
                candidates = graph.onDbSuspend { graph.importer.prepare(opml, listener = ReadTracker()) }
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
        state.value = ImportUiState.Subscribing(0, selected.size)
        viewModelScope.launch {
            val choices = selected.map { candidates[it.index] to CategoryId(it.categoryId) }
            var latest: LatestShowUi? = null
            graph.onDb {
                graph.importer.confirm(choices) { done, total ->
                    val row = selected[done - 1]
                    row.artworkUrl?.let { latest = LatestShowUi(row.title.ifBlank { null }, it) }
                    state.value = ImportUiState.Subscribing(done, total, latest)
                }
            }
            state.value = ImportUiState.Done(choices.size)
        }
    }

    fun onStartAgain() {
        candidates = emptyList()
        state.value = ImportUiState.ChooseFile
    }

    /** Turns importer events into Reading states: the count, and the newest show with artwork. */
    private inner class ReadTracker : ImportListener {
        private var total = 0
        private var done = 0
        private var latest: LatestShowUi? = null

        override fun onStarted(entries: List<OpmlEntry>) {
            total = entries.size
            publish()
        }

        override fun onFeedRead(index: Int, candidate: ImportCandidate) {
            done++
            val artwork = candidate.imageUrl
            if (candidate.feed != null && artwork != null) {
                latest = LatestShowUi(candidate.title.ifBlank { null }, artwork)
            }
            publish()
        }

        private fun publish() {
            state.value = ImportUiState.Reading(done, total, latest)
        }
    }

    private suspend fun review(categoryIds: List<String>): ImportUiState.Review {
        val categories = graph.library.first().categoryOptions()
        val rows = candidates.mapIndexed { index, c ->
            ImportRowUi(
                index = index,
                title = c.title,
                artworkUrl = c.imageUrl,
                // The ID the show will have once subscribed, so it keeps its colour.
                fallback = ArtworkFallback.of(ShowIds.fromFeedUrl(c.entry.feedUrl).value, c.title),
                categoryId = categoryIds[index],
                problem = c.problem,
                selected = c.problem == null,
            )
        }
        return ImportUiState.Review(rows, categories)
    }
}
