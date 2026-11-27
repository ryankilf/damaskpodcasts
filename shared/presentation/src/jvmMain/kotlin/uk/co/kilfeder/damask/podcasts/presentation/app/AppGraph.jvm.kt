package uk.co.kilfeder.damask.podcasts.presentation.app

import uk.co.kilfeder.damask.podcasts.core.data.desktopDataDirectory
import uk.co.kilfeder.damask.podcasts.core.data.desktopDriverFactory
import java.io.File

/** The desktop app's graph, with its database in the per-user data directory. */
fun AppGraph.Companion.create(): AppGraph = create(desktopDataDirectory().resolve("damask.db"))

/** A graph on a specific database file (tests and tools use a throwaway one). */
fun AppGraph.Companion.create(databaseFile: File): AppGraph = AppGraph(desktopDriverFactory(databaseFile))
