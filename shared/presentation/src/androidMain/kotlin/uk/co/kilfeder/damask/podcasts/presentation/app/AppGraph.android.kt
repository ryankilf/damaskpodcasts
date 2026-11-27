package uk.co.kilfeder.damask.podcasts.presentation.app

import android.content.Context
import uk.co.kilfeder.damask.podcasts.core.data.androidDriverFactory

/** Platform bootstrap: the only Android API presentation touches (see LayerBoundaryTest). */
fun AppGraph.Companion.create(context: Context): AppGraph = AppGraph(androidDriverFactory(context))
