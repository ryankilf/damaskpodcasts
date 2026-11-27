package uk.co.kilfeder.damask.podcasts.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runDesktopComposeUiTest
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.app.create
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

/**
 * Opt-in tour of the real app at phone size: age gate, import, Home, Genres. Renders offscreen
 * and writes PNGs; uses a throwaway database and (with an OPML file) real network fetches.
 *
 *     ./gradlew :shared:ui:jvmTest --tests '*ScreenshotTour*' \
 *         -Pdamask.screenshots=llm-scrapbook/screens -Pdamask.opml=llm-scrapbook/podcasts_opml.xml
 */
@OptIn(ExperimentalTestApi::class)
class ScreenshotTour {
    @Test
    fun tour() {
        val out = System.getProperty("damask.screenshots")?.let(::File) ?: return
        out.mkdirs()
        val db = File.createTempFile("damask-tour", ".db").also { it.delete() }
        val graph = AppGraph.create(db)
        val owner = object : ViewModelStoreOwner {
            override val viewModelStore = ViewModelStore()
        }
        runDesktopComposeUiTest(width = PHONE_WIDTH, height = PHONE_HEIGHT) {
            setContent {
                CompositionLocalProvider(LocalViewModelStoreOwner provides owner) { App(graph) }
            }
            waitForText("I’m 18 or over")
            shoot(out, "1-age-gate")
            onNodeWithText("I’m 18 or over").performClick()

            waitForText("Choose OPML file")
            shoot(out, "2-import")

            val opml = System.getProperty("damask.opml")?.let(::File) ?: return@runDesktopComposeUiTest
            val import = graph.importViewModel()
            import.onFileChosen(opml.readText())
            waitUntil(timeoutMillis = IMPORT_TIMEOUT_MS) {
                import.uiState.value is uk.co.kilfeder.damask.podcasts.presentation.importer.ImportUiState.Review
            }
            import.onConfirm()
            waitForText("Up Next", IMPORT_TIMEOUT_MS)
            settle(ARTWORK_SETTLE_MS) // let artwork arrive
            shoot(out, "3-home")

            onNodeWithText("Genres").performClick()
            waitForText("shows")
            settle(ARTWORK_SETTLE_MS)
            shoot(out, "4-genres")
        }
        db.delete()
    }

    private fun ComposeUiTest.waitForText(
        text: String,
        timeoutMs: Long = UI_TIMEOUT_MS,
        allowMissing: Boolean = false,
    ) {
        val found = { onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
        try {
            waitUntil(timeoutMillis = timeoutMs) { found() }
        } catch (e: ComposeTimeoutException) {
            if (!allowMissing) {
                val dir = System.getProperty("damask.screenshots")
                if (dir != null) shoot(File(dir), "failed-waiting-for-${text.filter(Char::isLetter)}")
                throw IllegalStateException("'$text' never appeared", e)
            }
        }
    }

    /** Waits in real time while letting the UI keep running (e.g. for artwork to load). */
    private fun ComposeUiTest.settle(ms: Long) {
        val until = System.currentTimeMillis() + ms
        waitUntil(timeoutMillis = ms + UI_TIMEOUT_MS) { System.currentTimeMillis() >= until }
    }

    private fun SemanticsNodeInteractionsProvider.shoot(dir: File, name: String) {
        ImageIO.write(onRoot().captureToImage().toAwtImage(), "png", File(dir, "$name.png"))
    }

    private companion object {
        const val PHONE_WIDTH = 412
        const val PHONE_HEIGHT = 900
        const val UI_TIMEOUT_MS = 10_000L
        const val IMPORT_TIMEOUT_MS = 120_000L
        const val ARTWORK_SETTLE_MS = 4_000L
    }
}
