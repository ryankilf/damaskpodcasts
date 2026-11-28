package uk.co.kilfeder.damask.podcasts.presentation.app

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import uk.co.kilfeder.damask.podcasts.presentation.genres.ManageGenresViewModel
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.data.GenreSettings
import app.cash.sqldelight.coroutines.asFlow
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.withContext
import uk.co.kilfeder.damask.podcasts.core.data.ShowSettings
import uk.co.kilfeder.damask.podcasts.core.data.CategoryPrefill
import uk.co.kilfeder.damask.podcasts.core.data.DeviceClock
import uk.co.kilfeder.damask.podcasts.core.data.DriverFactory
import uk.co.kilfeder.damask.podcasts.core.data.EpisodeLookup
import uk.co.kilfeder.damask.podcasts.core.data.FeedStore
import uk.co.kilfeder.damask.podcasts.core.data.LibraryRepository
import uk.co.kilfeder.damask.podcasts.core.data.UserStateStore
import uk.co.kilfeder.damask.podcasts.core.data.createDatabase
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.feed.FeedFetcher
import uk.co.kilfeder.damask.podcasts.core.feed.HttpClients
import uk.co.kilfeder.damask.podcasts.core.feed.FeedRefresher
import uk.co.kilfeder.damask.podcasts.core.importer.OpmlImporter
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.core.rules.ListeningRules
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeViewModel
import uk.co.kilfeder.damask.podcasts.core.directory.AppleDirectory
import uk.co.kilfeder.damask.podcasts.presentation.discover.AddFeedViewModel
import uk.co.kilfeder.damask.podcasts.presentation.discover.DiscoverViewModel
import uk.co.kilfeder.damask.podcasts.presentation.discover.ShowPreviews
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreQueueViewModel
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenresViewModel
import uk.co.kilfeder.damask.podcasts.presentation.home.HomeViewModel
import uk.co.kilfeder.damask.podcasts.presentation.importer.ImportViewModel
import uk.co.kilfeder.damask.podcasts.presentation.played.PlayedViewModel
import uk.co.kilfeder.damask.podcasts.presentation.playback.AudioEngine
import uk.co.kilfeder.damask.podcasts.presentation.playback.MiniPlayerViewModel
import uk.co.kilfeder.damask.podcasts.presentation.playback.NoAudioEngine
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaybackController
import uk.co.kilfeder.damask.podcasts.presentation.settings.SettingsViewModel
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowViewModel
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowsViewModel
import kotlin.time.Clock

/**
 * The app's object graph: one per process, created by each platform's entry point
 * (`AppGraph.create(...)` in androidMain / jvmMain / iosMain).
 *
 * Every database read and write runs on [dbDispatcher], a single thread, because the stores and
 * the HLC clock aren't thread-safe. [library] re-emits whenever a table the rotation reads changes,
 * so screens update by themselves after any write.
 */
// One ViewModel factory per screen, so the function count grows with the screens, not complexity.
@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
class AppGraph internal constructor(
    driverFactory: DriverFactory,
    internal val wallClockMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    fetcher: FeedFetcher = FeedFetcher.create(),
    internal val directory: AppleDirectory = AppleDirectory.create(),
    engineFactory: () -> AudioEngine = { NoAudioEngine },
    mainDispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    internal val dbDispatcher = Dispatchers.IO.limitedParallelism(1)
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    internal val rules = ListeningRules()

    internal val db: DamaskDatabase = driverFactory.createDatabase()
    private val clock = DeviceClock(db, wallClockMs)
    internal val userState = UserStateStore(db, clock)
    internal val feeds = FeedStore(db, clock, rules)
    internal val showSettings = ShowSettings(db, clock)
    internal val genreSettings = GenreSettings(db, clock)
    internal val settings = Settings(db)
    internal val lookup = EpisodeLookup(db)
    internal val repository = LibraryRepository(db)
    internal val importer = OpmlImporter(fetcher, feeds, CategoryPrefill(db))
    internal val refresher = FeedRefresher(fetcher, feeds)
    internal val previews by lazy { ShowPreviews(this) }

    /** Artwork client: the shared HTTP client (retries, timeouts, http -> https), following redirects. */
    val imageHttpClient: HttpClient get() = HttpClients.images

    /** Runs [block] on the database thread. */
    internal suspend fun <T> onDb(block: () -> T): T = withContext(dbDispatcher) { block() }

    /**
     * Runs suspending work confined to the database thread. Network calls inside still overlap
     * (they suspend), while every database access stays on the one thread.
     */
    internal suspend fun <T> onDbSuspend(block: suspend () -> T): T = withContext(dbDispatcher) { block() }

    init {
        // Genres in use get their colours (existing libraries, and any left without one).
        scope.launch { onDb { genreSettings.assignMissingColours() } }
    }

    /** The latest snapshot, reloaded whenever any table it reads changes. */
    internal val library: Flow<Library> = combine(
        db.libraryQueries.selectEpisodes().asFlow(),
        db.showQueries.selectAll().asFlow(),
        db.categoryQueries.selectLive().asFlow(),
        db.showAliasQueries.selectAll().asFlow(),
        db.feedQueries.selectShowFeeds().asFlow(),
    ) { _, _, _, _, _ -> }
        .mapLatest { onDb { repository.load() } }
        .shareIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), replay = 1)

    /** When the app last checked feeds for new episodes (null: never), for Home's "Checked …". */
    internal val lastChecked: Flow<Long?> = db.feedFetchStateQueries.selectLastChecked().asFlow()
        .map { query -> onDb { query.executeAsOneOrNull()?.last_checked } }

    /** Search text for Discover to open with ("Find more comedy" on an empty genre); taken once. */
    internal val discoverSeed = MutableStateFlow<String?>(null)

    /** The one playback controller; the engine is created on first use, on the main thread. */
    val playback: PlaybackController by lazy {
        PlaybackController(this, engineFactory(), CoroutineScope(SupervisorJob() + mainDispatcher))
    }

    /** A rotation over the current snapshot, computed off the main thread. */
    internal val rotation: Flow<Rotation> = library
        .mapLatest { withContext(Dispatchers.Default) { Rotation(it, rules, wallClockMs()) } }


    // ViewModel factories: the UI creates every ViewModel through these.
    fun appViewModel() = AppViewModel(this)

    fun homeViewModel() = HomeViewModel(this)

    fun genresViewModel() = GenresViewModel(this)

    fun genreQueueViewModel(categoryId: String) = GenreQueueViewModel(this, categoryId)

    fun playedViewModel() = PlayedViewModel(this)

    fun episodeViewModel(id: EpisodeId) = EpisodeViewModel(this, id)

    fun miniPlayerViewModel() = MiniPlayerViewModel(this)

    fun showsViewModel() = ShowsViewModel(this)

    fun showViewModel(showId: String) = ShowViewModel(this, showId)

    fun importViewModel() = ImportViewModel(this)

    fun settingsViewModel() = SettingsViewModel(this)

    fun manageGenresViewModel() = ManageGenresViewModel(this)

    fun discoverViewModel() = DiscoverViewModel(this)

    fun addFeedViewModel() = AddFeedViewModel(this)

    /** Each platform adds its own `create` (it knows how to open the database). */
    companion object
}

/**
 * A rotation over a snapshot read now, for decisions made straight after a write (autoplay).
 * [AppGraph.rotation] may still be holding the snapshot from before it.
 */
internal suspend fun AppGraph.freshRotation(): Rotation =
    Rotation(onDb { repository.load() }, rules, wallClockMs())

/**
 * Refreshes every subscribed feed. Network fetches run concurrently; the refresher's writes go
 * through the database thread because the whole run is confined to it.
 */
internal suspend fun AppGraph.refreshFeeds() = withContext(dbDispatcher) { refresher.refreshAll() }

/** Refreshes just these shows (after switching them to oldest first, say). */
internal suspend fun AppGraph.refreshShows(ids: Collection<ShowId>) =
    withContext(dbDispatcher) { refresher.refresh(ids) }
