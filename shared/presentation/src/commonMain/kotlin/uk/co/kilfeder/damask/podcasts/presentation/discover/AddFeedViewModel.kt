package uk.co.kilfeder.damask.podcasts.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.feed.FeedAddress
import uk.co.kilfeder.damask.podcasts.core.feed.NEEDS_LOGIN
import uk.co.kilfeder.damask.podcasts.core.sync.FeedUrl
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph

/** Why a pasted link couldn't be added. */
sealed interface AddFeedProblem {
    data object NotALink : AddFeedProblem

    /** The feed wants a username and password, which the app can't send yet. */
    data object NeedsLogin : AddFeedProblem

    /** It couldn't be fetched or isn't a podcast feed: [reason] says which. */
    data class Unreadable(val reason: String) : AddFeedProblem
}

/** A show to open once its feed has been read: in Shows if you have it, else its preview. */
data class AddedFeed(val showId: String, val subscribed: Boolean)

data class AddFeedUiState(
    val checking: Boolean = false,
    val problem: AddFeedProblem? = null,
    /** Read and stored: open it (then onOpened). */
    val added: AddedFeed? = null,
)

/**
 * "Add a feed by URL" (spec › Premium feeds): for private feeds, which search can't find, and
 * public ones missing from the directory. The feed is read and stored as a preview, like a
 * Discover result, so you can listen before subscribing; one you already have opens in Shows.
 * The address is a secret for a private feed: it goes only to its own host.
 */
class AddFeedViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    private val state = MutableStateFlow(AddFeedUiState())
    val uiState: StateFlow<AddFeedUiState> = state.asStateFlow()

    fun onAdd(text: String) {
        if (state.value.checking) return
        val url = FeedAddress.parse(text)
        if (url == null) {
            state.value = AddFeedUiState(problem = AddFeedProblem.NotALink)
            return
        }
        state.value = AddFeedUiState(checking = true)
        viewModelScope.launch {
            // Already yours (by the same address, however it's written): straight to it in Shows.
            val mine = graph.library.first().subscribedShows
                .firstOrNull { FeedUrl.normalise(it.feedUrl) == FeedUrl.normalise(url) }
            if (mine != null) {
                state.value = AddFeedUiState(added = AddedFeed(mine.id.value, subscribed = true))
                return@launch
            }
            val candidate = graph.onDbSuspend { graph.importer.prepareFeed("", url) }
            state.value = if (candidate.feed == null) {
                val problem = if (candidate.problem == NEEDS_LOGIN) {
                    AddFeedProblem.NeedsLogin
                } else {
                    AddFeedProblem.Unreadable(candidate.problem.orEmpty())
                }
                AddFeedUiState(problem = problem)
            } else {
                val id = graph.onDb { graph.importer.preview(candidate) }
                // Open it once the library has it, so its page never flashes "not found".
                val stored = graph.library.first { id in it.showsById }.showsById.getValue(id)
                AddFeedUiState(added = AddedFeed(id.value, subscribed = stored.subscribed))
            }
        }
    }

    /** The screen has opened [AddFeedUiState.added]; the dialog starts fresh next time. */
    fun onOpened() {
        state.value = AddFeedUiState()
    }

    /** Typing again clears the last problem. */
    fun onEdit() {
        if (state.value.problem != null) state.value = AddFeedUiState()
    }
}
