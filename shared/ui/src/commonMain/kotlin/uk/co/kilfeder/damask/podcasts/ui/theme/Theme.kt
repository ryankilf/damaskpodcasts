package uk.co.kilfeder.damask.podcasts.ui.theme

import androidx.compose.ui.graphics.luminance
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * App theme. UI code reads `MaterialTheme.colorScheme` roles only, never hex values, so the scheme
 * can be swapped at runtime (enforced by the NoHardcodedColors detekt rule).
 *
 * @param darkTheme from Settings › Theme and colours › Mode (Automatic follows the system).
 * @param useWallpaperColours Settings › Theme and colours › "Your colours". Ignored where the
 * platform has no dynamic colour. Off by default: the Damask brand scheme is the default.
 */
@Composable
fun DamaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useWallpaperColours: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dynamic = if (useWallpaperColours) dynamicColorScheme(darkTheme) else null
    MaterialTheme(
        colorScheme = dynamic ?: brandColorScheme(darkTheme),
        typography = DamaskTypography,
        shapes = DamaskShapes,
        content = content,
    )
}

/** Above this the page is light. */
private const val LIGHT_SURFACE_LUMINANCE = 0.5f

/** Whether the theme in use is dark, whatever chose it (the mode setting, the system). */
@Composable
internal fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.surface.luminance() <= LIGHT_SURFACE_LUMINANCE

internal fun brandColorScheme(darkTheme: Boolean): ColorScheme =
    if (darkTheme) DamaskDarkColors else DamaskLightColors

/** The wallpaper-derived scheme, or null where the platform doesn't support dynamic colour. */
@Composable
internal expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?

/** Whether to show the "Use wallpaper colours" setting at all. */
expect val supportsWallpaperColours: Boolean
