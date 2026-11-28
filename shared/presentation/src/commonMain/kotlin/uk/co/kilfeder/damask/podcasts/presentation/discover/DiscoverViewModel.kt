package uk.co.kilfeder.damask.podcasts.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.directory.DirectoryResult
import uk.co.kilfeder.damask.podcasts.core.directory.DirectoryShow
import uk.co.kilfeder.damask.podcasts.core.directory.deviceRegion
import uk.co.kilfeder.damask.podcasts.core.importer.ImportCandidate
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.sync.FeedUrl
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue

/** Wait this long after the last keystroke before searching (spec › Search: never per keystroke). */
private const val TYPING_PAUSE_MS = 500L

/** A search's raw outcome, before it's matched against your library. */
private sealed interface Search {
    data object Idle : Search
    /** [previous]: the last search's shows, kept on screen while this one runs (empty: none). */
    data class Running(val previous: List<DirectoryShow>) : Search
    data class Done(val result: DirectoryResult) : Search
}

/** Feeds being read, feeds that couldn't be, and a page waiting to open. */
private data class Progress(
    val fetching: Map<String, DiscoverRowState>,
    val problems: Map<String, String>,
    val openShowId: String?,
)

/**
 * Discover (spec › Search): searches Apple's podcast directory as you type. Subscribe reads the
 * feed and subscribes in the genre Apple gives (else the feed's own), with no sheet: you change
 * it on the show's page. Tapping a show you don't have opens its page at once, unsubscribed, to
 * listen before subscribing; its feed is read there ([ShowPreviews]).
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DiscoverViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    private val query = MutableStateFlow("")

    /** Bumped by Try again, to run the same search again. */
    private val attempt = MutableStateFlow(0)
    /** Rows whose feed is being read to subscribe. */
    private val fetching = MutableStateFlow<Map<String, DiscoverRowState>>(emptyMap())
    private val problems = MutableStateFlow<Map<String, String>>(emptyMap())
    private val openShowId = MutableStateFlow<String?>(null)
    private var lastShows: List<DirectoryShow> = emptyList()

    private val search = combine(
        query.debounce { if (it.isBlank()) 0 else TYPING_PAUSE_MS }.map { it.trim() }.distinctUntilChanged(),
        graph.settings.values.map { it.hideExplicit }.distinctUntilChanged(),
        attempt,
    ) { q, hideExplicit, _ -> q to hideExplicit }
        .flatMapLatest { (q, hideExplicit) ->
            flow {
                if (q.isEmpty()) {
                    lastShows = emptyList()
                    emit(Search.Idle)
                } else {
                    emit(Search.Running(lastShows))
                    val result = graph.directory.search(q, deviceRegion(), hideExplicit)
                    // What Subscribe looks a row up in.
                    lastShows = (result as? DirectoryResult.Found)?.shows.orEmpty()
                    emit(Search.Done(result))
                }
            }
        }

    private val progress = combine(fetching, problems, openShowId, ::Progress)

    // The typed text is part of the state as it changes (the field shows it back), not only when
    // a search finishes.
    val uiState: StateFlow<DiscoverUiState> =
        combine(query, search, graph.library, progress) { typed, search, library, progress ->
            DiscoverUiState(
                query = typed,
                results = results(search, library, progress.fetching, progress.problems),
                openShowId = progress.openShowId,
            )
        }.stateInUi(viewModelScope, DiscoverUiState())

    init {
        // Opened with search text from elsewhere ("Find more comedy"): take it once.
        viewModelScope.launch {
            graph.discoverSeed.collect { seed ->
                if (seed != null) {
                    query.value = seed
                    graph.discoverSeed.value = null
                }
            }
        }
    }

    fun onQueryChange(text: String) {
        query.value = text
    }

    /** Try again, after the directory couldn't be reached or was busy. */
    fun onRetry() = attempt.update { it + 1 }

    /** Subscribe: in Apple's genre for it (else its feed's), straight away. */
    fun onSubscribe(key: String) = withFeed(key, DiscoverRowState.SUBSCRIBING) { candidate ->
        // The app's scope: the subscription finishes even if you leave Discover straight away.
        graph.scope.launch {
            graph.onDb {
                graph.importer.confirm(listOf(candidate to candidate.suggestedCategory))
                // A genre gets its colour once it has a show.
                graph.genreSettings.assignMissingColours()
            }
        }
    }

    /** Tapping a show you don't have: its page opens now, and reads the feed itself. */
    fun onOpen(key: String) {
        val show = lastShows.firstOrNull { FeedUrl.normalise(it.feedUrl) == key } ?: return
        openShowId.value = graph.previews.open(show).value
    }

    /** The screen has opened [DiscoverUiState.openShowId]. */
    fun onShowOpened() {
        openShowId.value = null
    }

    /** Reads the feed for the result [key] (its row shows [why]), then [then]; a failure shows on the row. */
    private fun withFeed(key: String, why: DiscoverRowState, then: suspend (ImportCandidate) -> Unit) {
        val show = lastShows.firstOrNull { FeedUrl.normalise(it.feedUrl) == key } ?: return
        if (key in fetching.value) return
        fetching.update { it + (key to why) }
        problems.update { it - key }
        viewModelScope.launch {
            val candidate = graph.onDbSuspend { graph.importer.prepareFeed(show.title, show.feedUrl, show.genres) }
            if (candidate.feed == null) {
                problems.update { it + (key to (candidate.problem ?: "")) }
            } else {
                then(candidate)
            }
            fetching.update { it - key }
        }
    }

    private fun results(
        search: Search,
        library: Library,
        fetching: Map<String, DiscoverRowState>,
        problems: Map<String, String>,
    ): DiscoverResults = when (search) {
        Search.Idle -> DiscoverResults.Idle
        // Refining: keep the previous results (dimmed); a first search shows placeholders.
        is Search.Running -> if (search.previous.isEmpty()) {
            DiscoverResults.Searching
        } else {
            DiscoverResults.Found(rows(search.previous, library, fetching, problems), refreshing = true)
        }
        is Search.Done -> when (val result = search.result) {
            DirectoryResult.Busy -> DiscoverResults.Busy
            is DirectoryResult.Failed -> DiscoverResults.Failed
            is DirectoryResult.Found -> rows(result.shows, library, fetching, problems)
                .let { if (it.isEmpty()) DiscoverResults.NoMatch else DiscoverResults.Found(it) }
        }
    }

    /** [shows] as rows: which you have, which are being read, and any feed that couldn't be. */
    private fun rows(
        shows: List<DirectoryShow>,
        library: Library,
        fetching: Map<String, DiscoverRowState>,
        problems: Map<String, String>,
    ): List<DiscoverRowUi> {
        val subscribed = library.subscribedShows.associateBy { FeedUrl.normalise(it.feedUrl) }
        return shows.map { show ->
            val key = FeedUrl.normalise(show.feedUrl)
            val mine = subscribed[key]
            DiscoverRowUi(
                key = key,
                title = show.title,
                author = show.author,
                artworkUrl = show.artworkUrl,
                fallback = ArtworkFallback.of(show.title, GenreHue.GREY),
                state = when {
                    mine != null -> DiscoverRowState.SUBSCRIBED
                    else -> fetching[key] ?: DiscoverRowState.NEW
                },
                showId = mine?.id?.value,
                problem = problems[key]?.takeIf { mine == null },
            )
        }
    }
}
