package uk.co.kilfeder.damask.podcasts

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import uk.co.kilfeder.damask.podcasts.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "DamaskPodcasts",
    ) {
        App()
    }
}
