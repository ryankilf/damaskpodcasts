package uk.co.kilfeder.damask.podcasts.presentation.app

import uk.co.kilfeder.damask.podcasts.core.data.desktopDataDirectory
import uk.co.kilfeder.damask.podcasts.core.data.desktopDriverFactory
import uk.co.kilfeder.damask.podcasts.core.feed.HttpClients
import uk.co.kilfeder.damask.podcasts.presentation.playback.AudioEngine
import java.io.File

/** The desktop app's graph, with its database in the per-user data directory. */
fun AppGraph.Companion.create(engineFactory: () -> AudioEngine): AppGraph {
    // `./gradlew :desktopApp:run` sets this, so development runs log every HTTP request to stderr.
    HttpClients.logRequests = System.getProperty("damask.httpLog") == "true"
    return AppGraph(desktopDriverFactory(desktopDataDirectory().resolve("damask.db")), engineFactory = engineFactory)
}

/** A graph on a specific database file (tests and tools use a throwaway one). */
fun AppGraph.Companion.create(databaseFile: File): AppGraph = AppGraph(desktopDriverFactory(databaseFile))
