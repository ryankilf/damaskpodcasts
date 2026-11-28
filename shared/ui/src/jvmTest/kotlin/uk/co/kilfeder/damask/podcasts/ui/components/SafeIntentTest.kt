package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SafeIntentTest {
    private class Opener(private val fail: Boolean) : UriHandler {
        val opened = mutableListOf<String>()

        override fun openUri(uri: String) {
            if (fail) throw IllegalArgumentException("No Activity found to handle Intent")
            opened += uri
        }
    }

    private fun open(opener: Opener, uri: String): List<String> {
        val messages = mutableListOf<String>()
        runComposeUiTest {
            setContent {
                @Suppress("UnsafeIntentLaunch") // the test stands in for the platform opener
                CompositionLocalProvider(
                    LocalUriHandler provides opener,
                    LocalShowMessage provides { messages += it },
                ) {
                    rememberSafeIntent()(uri)
                }
            }
            waitForIdle()
        }
        return messages
    }

    @Test
    fun noAppForItShowsAMessageInsteadOfCrashing() {
        val messages = open(Opener(fail = true), "mailto:someone@example.com")
        assertEquals(listOf("Couldn’t find an app to open that."), messages)
    }

    @Test
    fun opensWhenThereIsAnApp() {
        val opener = Opener(fail = false)
        assertEquals(emptyList(), open(opener, "https://example.com"))
        assertEquals(listOf("https://example.com"), opener.opened)
    }
}
