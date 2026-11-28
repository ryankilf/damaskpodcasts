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

/** A preview's progress, for its show page while the show isn't stored yet. */
sealed interface PreviewState {
    data object Loading : PreviewState

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

    /** Starts reading [show]'s feed and returns the ID its page opens with. */
    fun open(show: DirectoryShow): ShowId {
        val id = ShowIds.fromFeedUrl(show.feedUrl)
        if (states.value[id] == PreviewState.Loading) return id
        states.update { it + (id to PreviewState.Loading) }
        graph.scope.launch {
            val candidate = graph.onDbSuspend { graph.importer.prepareFeed(show.title, show.feedUrl, show.genres) }
            if (candidate.feed == null) {
                states.update { it + (id to PreviewState.Failed(candidate.problem.orEmpty())) }
            } else {
                graph.onDb { graph.importer.preview(candidate) }
                states.update { it - id }
            }
        }
        return id
    }
}
