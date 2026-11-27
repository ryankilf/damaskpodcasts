package uk.co.kilfeder.damask.podcasts.presentation.app

import android.content.Context
import uk.co.kilfeder.damask.podcasts.core.data.androidDriverFactory
import uk.co.kilfeder.damask.podcasts.presentation.playback.AudioEngine

/** Platform bootstrap: the only Android API presentation touches (see LayerBoundaryTest). */
fun AppGraph.Companion.create(context: Context, engineFactory: () -> AudioEngine): AppGraph =
    AppGraph(androidDriverFactory(context), engineFactory = engineFactory)
