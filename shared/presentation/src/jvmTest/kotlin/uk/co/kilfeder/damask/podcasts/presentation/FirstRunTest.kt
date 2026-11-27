package uk.co.kilfeder.damask.podcasts.presentation

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import uk.co.kilfeder.damask.podcasts.core.data.DriverFactory
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.feed.FeedFetcher
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.AppMessage
import uk.co.kilfeder.damask.podcasts.presentation.navigation.NavState
import uk.co.kilfeder.damask.podcasts.presentation.navigation.Tab
import uk.co.kilfeder.damask.podcasts.presentation.app.Stage
import uk.co.kilfeder.damask.podcasts.presentation.importer.ImportUiState
import java.util.Properties
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Empty Home -> import -> Home -> mark played -> undo from Played, on a real database. */
@OptIn(ExperimentalCoroutinesApi::class)
class FirstRunTest {
    private val now = 1_795_780_800_000L // 27 Nov 2026

    private val feeds = mapOf(
        "https://history.example.com/feed" to feed("History Hour", "History", "h"),
        "https://news.example.com/feed" to feed("Daily News", "News", "n"),
    )

    private val client = HttpClient(
        MockEngine { request ->
            val body = feeds[request.url.toString()]
            if (body != null) respond(body, HttpStatusCode.OK) else respond("", HttpStatusCode.NotFound)
        },
    ) {
        followRedirects = false
        expectSuccess = false
    }

    private val graph = AppGraph(
        driverFactory = DriverFactory {
            JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), DamaskDatabase.Schema)
        },
        wallClockMs = { now },
        fetcher = FeedFetcher(client),
    )

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun firstRunThroughToUndo() = runBlocking {
        val app = graph.appViewModel()
        app.uiState.await { it.stage == Stage.MAIN }
        val home = graph.homeViewModel()
        home.uiState.await { !it.loading && !it.hasSubscriptions }

        val import = graph.importViewModel()
        import.onFileChosen(OPML)
        val review = import.uiState.await { it is ImportUiState.Review } as ImportUiState.Review
        assertEquals(listOf("Daily News", "History Hour"), review.rows.map { it.title })
        assertEquals(listOf("apple:news", "apple:history"), review.rows.map { it.categoryId })
        assertTrue(review.categories.last().name == "Uncategorised", "Uncategorised listed last")

        // Untick one show, re-tick it: only ticked shows are subscribed.
        import.onSelectionChanged(review.rows.first().index, false)
        assertEquals(1, (import.uiState.value as ImportUiState.Review).selectedCount)
        import.onSelectionChanged(review.rows.first().index, true)
        import.onSelectAllChanged(false)
        assertEquals(0, (import.uiState.value as ImportUiState.Review).selectedCount)
        import.onSelectAllChanged(true)
        assertTrue((import.uiState.value as ImportUiState.Review).allSelected)

        app.onNavigate(uk.co.kilfeder.damask.podcasts.presentation.navigation.Destination.Settings)
        import.onConfirm()
        import.uiState.await { it == ImportUiState.Done(2) }
        app.onImportFinished(2)
        val afterImport = app.uiState.await { it.message != null }
        assertEquals(AppMessage.ShowsAdded(2), afterImport.message)
        assertEquals(NavState(tab = Tab.HOME), afterImport.nav, "back on Home, Settings closed")
        app.onMessageShown()
        app.uiState.await { it.message == null }

        val upNext = home.uiState.await { it.upNext.size == 4 }.upNext
        assertEquals("History Hour", upNext.first().showTitle, "never-listened categories by name: History first")
        assertEquals("2 days ago", upNext.first().published.summary)

        home.onMarkPlayed(upNext.first().id)
        home.uiState.await { it.upNext.size == 3 }

        val played = graph.playedViewModel()
        val row = played.uiState.await { it.rows.size == 1 }.rows.single()
        assertTrue(row.isPlayed)
        played.onMarkUnplayed(row.episode.id)
        played.uiState.await { it.rows.singleOrNull()?.isPlayed == false }
        home.uiState.await { it.upNext.size == 4 }

        val genres = graph.genresViewModel().uiState.await { it.genres.isNotEmpty() }.genres
        assertEquals(listOf("History", "News"), genres.map { it.name }, "empty categories hidden")
    }

    /** Collects [this] while waiting, so WhileSubscribed flows stay live. */
    private suspend fun <T> StateFlow<T>.await(predicate: (T) -> Boolean): T = withTimeout(TIMEOUT_MS) {
        val job = launchIn(this)
        try {
            first(predicate)
        } finally {
            job.cancel()
        }
    }

    private companion object {
        const val TIMEOUT_MS = 10_000L

        val OPML = """
            <opml version="1.0"><body><outline text="feeds">
              <outline type="rss" text="History Hour" xmlUrl="https://history.example.com/feed"/>
              <outline type="rss" text="Daily News" xmlUrl="https://news.example.com/feed"/>
            </outline></body></opml>
        """.trimIndent()

        fun feed(title: String, category: String, prefix: String) = """
            <rss version="2.0" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd"><channel>
              <title>$title</title><itunes:category text="$category"/>
              <item><title>$title 1</title><guid>$prefix-1</guid><pubDate>Wed, 25 Nov 2026 08:00:00 GMT</pubDate>
                <enclosure url="https://cdn.example.com/$prefix-1.mp3"/><itunes:duration>45:00</itunes:duration></item>
              <item><title>$title 2</title><guid>$prefix-2</guid><pubDate>Mon, 23 Nov 2026 08:00:00 GMT</pubDate>
                <enclosure url="https://cdn.example.com/$prefix-2.mp3"/></item>
            </channel></rss>
        """.trimIndent()
    }
}
