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
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.sync.FeedUrl
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.stateInUi
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue
import uk.co.kilfeder.damask.podcasts.presentation.genres.categoryOptions
import uk.co.kilfeder.damask.podcasts.presentation.genres.nextHue
import uk.co.kilfeder.damask.podcasts.presentation.genres.toColour

/** Wait this long after the last keystroke before searching (spec › Search: never per keystroke). */
private const val TYPING_PAUSE_MS = 500L

/** A search's raw outcome, before it's matched against your library. */
private sealed interface Search {
    data object Idle : Search
    data object Running : Search
    data class Done(val result: DirectoryResult) : Search
}

/** A show whose feed has been read, waiting for its genre. */
private data class Pending(val candidate: ImportCandidate)

/** Subscribing in progress: feeds being read, feeds that failed, and the show awaiting its genre. */
private data class Subscribing(
    val fetching: Set<String>,
    val problems: Map<String, String>,
    val pending: Pending?,
)

/**
 * Discover (spec › Search): searches Apple's podcast directory as you type, and subscribes to a
 * result the way import does: read its feed, confirm or change its genre, subscribe.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class DiscoverViewModel internal constructor(private val graph: AppGraph) : ViewModel() {
    private val query = MutableStateFlow("")
    private val fetching = MutableStateFlow<Set<String>>(emptySet())
    private val problems = MutableStateFlow<Map<String, String>>(emptyMap())
    private val pending = MutableStateFlow<Pending?>(null)
    private var lastShows: List<DirectoryShow> = emptyList()

    private val search = combine(
        query.debounce { if (it.isBlank()) 0 else TYPING_PAUSE_MS }.map { it.trim() }.distinctUntilChanged(),
        graph.settings.values.map { it.hideExplicit }.distinctUntilChanged(),
    ) { q, hideExplicit -> q to hideExplicit }
        .flatMapLatest { (q, hideExplicit) ->
            flow {
                if (q.isEmpty()) {
                    emit(Search.Idle)
                } else {
                    emit(Search.Running)
                    val result = graph.directory.search(q, deviceRegion(), hideExplicit)
                    // What Subscribe looks a row up in.
                    lastShows = (result as? DirectoryResult.Found)?.shows.orEmpty()
                    emit(Search.Done(result))
                }
            }
        }

    private val subscribing = combine(fetching, problems, pending, ::Subscribing)

    // The typed text is part of the state as it changes (the field shows it back), not only when
    // a search finishes.
    val uiState: StateFlow<DiscoverUiState> =
        combine(query, search, graph.library, subscribing) { typed, search, library, subscribing ->
            DiscoverUiState(
                query = typed,
                results = results(search, library, subscribing.fetching, subscribing.problems),
                choosing = subscribing.pending?.candidate?.let {
                    GenreChoiceUi(it.title, it.suggestedCategory.value)
                },
                genres = library.categoryOptions(),
                nextHue = library.nextHue(),
            )
        }.stateInUi(viewModelScope, DiscoverUiState())

    fun onQueryChange(text: String) {
        query.value = text
    }

    /** Subscribe (or tapping a show you don't have): read its feed, then ask for its genre. */
    fun onSubscribe(key: String) {
        val show = lastShows.firstOrNull { FeedUrl.normalise(it.feedUrl) == key } ?: return
        if (key in fetching.value) return
        fetching.update { it + key }
        problems.update { it - key }
        viewModelScope.launch {
            val candidate = graph.onDbSuspend { graph.importer.prepareFeed(show.title, show.feedUrl) }
            if (candidate.feed == null) {
                problems.update { it + (key to (candidate.problem ?: "")) }
            } else {
                pending.value = Pending(candidate)
            }
            fetching.update { it - key }
        }
    }

    /** A genre picked in the sheet: subscribes the show into it. */
    fun onGenreSelect(genreId: String) = subscribe { CategoryId(genreId) }

    /** "Create “…”" in the sheet: makes the genre and subscribes the show into it. */
    fun onCreateGenre(name: String, hue: GenreHue) {
        val colour = hue.toColour() ?: return
        if (name.isBlank()) return
        subscribe { graph.genreSettings.create(name, colour) }
    }

    /** Closing the genre sheet without choosing: nothing is subscribed. */
    fun onGenreDismiss() {
        pending.value = null
    }

    private fun subscribe(genre: () -> CategoryId) {
        val candidate = pending.value?.candidate ?: return
        pending.value = null
        // The app's scope: the subscription finishes even if you leave Discover straight away.
        graph.scope.launch {
            graph.onDb {
                graph.importer.confirm(listOf(candidate to genre()))
                // A genre gets its colour once it has a show.
                graph.genreSettings.assignMissingColours()
            }
        }
    }

    private fun results(
        search: Search,
        library: Library,
        fetching: Set<String>,
        problems: Map<String, String>,
    ): DiscoverResults = when (search) {
        Search.Idle -> DiscoverResults.Idle
        Search.Running -> DiscoverResults.Searching
        is Search.Done -> when (val result = search.result) {
            DirectoryResult.Busy -> DiscoverResults.Busy
            is DirectoryResult.Failed -> DiscoverResults.Failed
            is DirectoryResult.Found -> {
                val subscribed = library.subscribedShows.associateBy { FeedUrl.normalise(it.feedUrl) }
                result.shows
                    .map { show ->
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
                                key in fetching -> DiscoverRowState.FETCHING
                                else -> DiscoverRowState.NEW
                            },
                            showId = mine?.id?.value,
                            problem = problems[key]?.takeIf { mine == null },
                        )
                    }
                    .let { if (it.isEmpty()) DiscoverResults.NoMatch else DiscoverResults.Found(it) }
            }
        }
    }
}
