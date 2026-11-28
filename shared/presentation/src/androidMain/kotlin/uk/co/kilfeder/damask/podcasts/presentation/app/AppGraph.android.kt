package uk.co.kilfeder.damask.podcasts.presentation.app

import android.content.Context
import uk.co.kilfeder.damask.podcasts.core.data.androidDriverFactory
import uk.co.kilfeder.damask.podcasts.core.feed.HttpClients
import uk.co.kilfeder.damask.podcasts.presentation.playback.AudioEngine

/** Platform bootstrap: the only Android API presentation touches (see LayerBoundaryTest). */
fun AppGraph.Companion.create(context: Context, engineFactory: () -> AudioEngine): AppGraph {
    // Debug builds log every HTTP request to Logcat (tag DamaskHttp); release builds never do.
    HttpClients.logRequests =
        (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
    return AppGraph(androidDriverFactory(context), engineFactory = engineFactory)
}
