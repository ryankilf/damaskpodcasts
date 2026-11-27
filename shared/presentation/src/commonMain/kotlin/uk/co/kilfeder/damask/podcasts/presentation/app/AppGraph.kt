package uk.co.kilfeder.damask.podcasts.presentation.app

import app.cash.sqldelight.coroutines.asFlow
import io.ktor.client.HttpClient
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
import uk.co.kilfeder.damask.podcasts.core.data.CategoryPrefill
import uk.co.kilfeder.damask.podcasts.core.data.DeviceClock
import uk.co.kilfeder.damask.podcasts.core.data.DriverFactory
import uk.co.kilfeder.damask.podcasts.core.data.EpisodeNotes
import uk.co.kilfeder.damask.podcasts.core.data.FeedStore
import uk.co.kilfeder.damask.podcasts.core.data.LibraryRepository
import uk.co.kilfeder.damask.podcasts.core.data.UserStateStore
import uk.co.kilfeder.damask.podcasts.core.data.createDatabase
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.feed.FeedFetcher
import uk.co.kilfeder.damask.podcasts.core.feed.FeedRefresher
import uk.co.kilfeder.damask.podcasts.core.importer.OpmlImporter
import uk.co.kilfeder.damask.podcasts.core.rotation.Library
import uk.co.kilfeder.damask.podcasts.core.rotation.Rotation
import uk.co.kilfeder.damask.podcasts.core.rules.ListeningRules
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeViewModel
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreQueueViewModel
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenresViewModel
import uk.co.kilfeder.damask.podcasts.presentation.home.HomeViewModel
import uk.co.kilfeder.damask.podcasts.presentation.importer.ImportViewModel
import uk.co.kilfeder.damask.podcasts.presentation.played.PlayedViewModel
import uk.co.kilfeder.damask.podcasts.presentation.settings.SettingsViewModel
import kotlin.time.Clock

/**
 * The app's object graph: one per process, created by each platform's entry point
 * (`AppGraph.create(...)` in androidMain / jvmMain / iosMain).
 *
 * Every database read and write runs on [dbDispatcher], a single thread, because the stores and
 * the HLC clock aren't thread-safe. [library] re-emits whenever a table the rotation reads changes,
 * so screens update by themselves after any write.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppGraph internal constructor(
    driverFactory: DriverFactory,
    internal val wallClockMs: () -> Long = { Clock.System.now().toEpochMilliseconds() },
    fetcher: FeedFetcher = FeedFetcher.create(),
) {
    internal val dbDispatcher = Dispatchers.IO.limitedParallelism(1)
    internal val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    internal val rules = ListeningRules()

    internal val db: DamaskDatabase = driverFactory.createDatabase()
    private val clock = DeviceClock(db, wallClockMs)
    internal val userState = UserStateStore(db, clock)
    internal val feeds = FeedStore(db, clock, rules)
    internal val settings = Settings(db)
    internal val notes = EpisodeNotes(db)
    private val repository = LibraryRepository(db)
    internal val importer = OpmlImporter(fetcher, feeds, CategoryPrefill(db))
    internal val refresher = FeedRefresher(fetcher, feeds)

    /** Artwork client: the platform engine with the same http -> https upgrade as feeds. */
    val imageHttpClient: HttpClient by lazy { FeedFetcher.createImageClient() }

    /** Runs [block] on the database thread. */
    internal suspend fun <T> onDb(block: () -> T): T = withContext(dbDispatcher) { block() }

    /**
     * Runs suspending work confined to the database thread. Network calls inside still overlap
     * (they suspend), while every database access stays on the one thread.
     */
    internal suspend fun <T> onDbSuspend(block: suspend () -> T): T = withContext(dbDispatcher) { block() }

    /** The latest snapshot, reloaded whenever any table it reads changes. */
    internal val library: Flow<Library> = combine(
        db.libraryQueries.selectEpisodes().asFlow(),
        db.showQueries.selectAll().asFlow(),
        db.categoryQueries.selectLive().asFlow(),
        db.showAliasQueries.selectAll().asFlow(),
        db.feedQueries.selectShowFeeds().asFlow(),
    ) { _, _, _, _, _ -> }
        .mapLatest { onDb { repository.load() } }
        .shareIn(scope, SharingStarted.WhileSubscribed(SHARE_TIMEOUT_MS), replay = 1)

    /** A rotation over the current snapshot, computed off the main thread. */
    internal val rotation: Flow<Rotation> = library
        .mapLatest { withContext(Dispatchers.Default) { Rotation(it, rules, wallClockMs()) } }

    /**
     * Refreshes every subscribed feed. Network fetches run concurrently; the refresher's writes go
     * through the database thread because the whole run is confined to it.
     */
    internal suspend fun refreshFeeds() = withContext(dbDispatcher) { refresher.refreshAll() }

    // ViewModel factories: the UI creates every ViewModel through these.
    fun appViewModel() = AppViewModel(this)

    fun homeViewModel() = HomeViewModel(this)

    fun genresViewModel() = GenresViewModel(this)

    fun genreQueueViewModel(categoryId: String) = GenreQueueViewModel(this, categoryId)

    fun playedViewModel() = PlayedViewModel(this)

    fun episodeViewModel(id: EpisodeId) = EpisodeViewModel(this, id)

    fun importViewModel() = ImportViewModel(this)

    fun settingsViewModel() = SettingsViewModel(this)

    companion object {
        private const val SHARE_TIMEOUT_MS = 5_000L
    }
}
