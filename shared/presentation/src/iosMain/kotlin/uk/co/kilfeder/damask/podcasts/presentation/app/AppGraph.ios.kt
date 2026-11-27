package uk.co.kilfeder.damask.podcasts.presentation.app

import uk.co.kilfeder.damask.podcasts.core.data.iosDriverFactory

fun AppGraph.Companion.create(): AppGraph = AppGraph(iosDriverFactory())
