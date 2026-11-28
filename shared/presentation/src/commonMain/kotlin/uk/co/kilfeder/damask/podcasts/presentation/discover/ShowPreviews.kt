package uk.co.kilfeder.damask.podcasts.presentation.discover

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.directory.DirectoryShow
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.sync.ShowIds
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph

/** What Apple told us about a show: enough for its page's header before its feed is read. */
data class PreviewHeader(val title: String, val author: String, val artworkUrl: String?)

/** A preview's progress, for its show page while the show isn't stored yet. */
sealed interface PreviewState {
    /**
     * Its feed is being read, or it's stored and the library hasn't re-emitted yet (it does a
     * moment after the write): either way the page shows [header] with placeholder episodes.
     * It stays so until the library has the show, so the page never flashes "not found".
     */
    data class Loading(val header: PreviewHeader) : PreviewState

    data class Failed(val reason: String) : PreviewState
}

/**
 * Shows opened from Discover that you don't have: their page opens at once (a show's ID comes
 * from its feed URL, so it's known before the feed is read), and the feed is read and stored
 * unsubscribed here, on the app's scope, while the page shows a spinner or why it failed.
 */
internal class ShowPreviews(private val graph: AppGraph) {
    private val states = MutableStateFlow<Map<ShowId, PreviewState>>(emptyMap())
    val state: StateFlow<Map<ShowId, PreviewState>> = states.asStateFlow()
    private val reading = mutableSetOf<ShowId>()

    /** Starts reading [show]'s feed (once at a time) and returns the ID its page opens with. */
    fun open(show: DirectoryShow): ShowId {
        val id = ShowIds.fromFeedUrl(show.feedUrl)
        if (!reading.add(id)) return id
        states.update { it + (id to PreviewState.Loading(PreviewHeader(show.title, show.author, show.artworkUrl))) }
        graph.scope.launch {
            val candidate = graph.onDbSuspend { graph.importer.prepareFeed(show.title, show.feedUrl, show.genres) }
            if (candidate.feed == null) {
                states.update { it + (id to PreviewState.Failed(candidate.problem.orEmpty())) }
            } else {
                // Left as Loading: the page switches to the stored show once the library has it.
                graph.onDb { graph.importer.preview(candidate) }
            }
            reading.remove(id)
        }
        return id
    }
}
