package uk.co.kilfeder.damask.podcasts

import android.app.Application
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.create

/** Holds the one [AppGraph] for the process. */
class DamaskApplication : Application() {
    val graph: AppGraph by lazy { AppGraph.create(this) }
}
