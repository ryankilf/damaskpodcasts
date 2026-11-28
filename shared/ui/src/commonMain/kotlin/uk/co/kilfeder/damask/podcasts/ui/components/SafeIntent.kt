package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalUriHandler
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.no_app_for_action

/** Shows a short message in the app's snackbar. The shell provides it; previews and tests get a no-op. */
internal val LocalShowMessage = staticCompositionLocalOf<(String) -> Unit> { {} }

/**
 * Hands a link or address (https:, mailto:, ...) to another app. When the device has nothing
 * that can open it (no mail client, no browser), it says so in a snackbar instead of crashing.
 *
 * The only way the app opens other apps: the `UnsafeIntentLaunch` lint rejects `startActivity`,
 * `openUri` and `LocalUriHandler` anywhere else.
 */
@Composable
internal fun rememberSafeIntent(): (String) -> Unit {
    // The one sanctioned use of the platform's opener; everything else comes through here.
    @Suppress("UnsafeIntentLaunch")
    val handler = LocalUriHandler.current
    val showMessage = LocalShowMessage.current
    val noApp = stringResource(Res.string.no_app_for_action)
    return remember(handler, showMessage, noApp) {
        { uri ->
            // Android reports "no app" as IllegalArgumentException (wrapping ActivityNotFound);
            // desktop as IOException or UnsupportedOperationException. Whatever the platform
            // throws, the answer for the listener is the same.
            @Suppress("UnsafeIntentLaunch", "TooGenericExceptionCaught", "SwallowedException")
            try {
                handler.openUri(uri)
            } catch (e: Exception) {
                showMessage(noApp)
            }
        }
    }
}
