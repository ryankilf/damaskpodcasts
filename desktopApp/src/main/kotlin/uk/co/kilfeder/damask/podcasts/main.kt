package uk.co.kilfeder.damask.podcasts

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.create
import uk.co.kilfeder.damask.podcasts.ui.App

fun main() {
    val graph = AppGraph.create()
    application {
        Window(onCloseRequest = ::exitApplication, title = "Damask") {
            App(graph)
        }
    }
}
