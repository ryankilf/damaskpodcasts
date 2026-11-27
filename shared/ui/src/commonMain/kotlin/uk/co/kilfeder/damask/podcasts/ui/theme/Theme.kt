package uk.co.kilfeder.damask.podcasts.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * App theme. UI code reads `MaterialTheme.colorScheme` roles only, never hex values, so the scheme
 * can be swapped at runtime (enforced by the NoHardcodedColors detekt rule).
 *
 * @param useWallpaperColours Settings › Appearance › "Use wallpaper colours". Ignored where the
 * platform has no dynamic colour; the Damask brand scheme is used instead.
 */
@Composable
fun DamaskTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useWallpaperColours: Boolean = true,
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

internal fun brandColorScheme(darkTheme: Boolean): ColorScheme =
    if (darkTheme) DamaskDarkColors else DamaskLightColors

/** The wallpaper-derived scheme, or null where the platform doesn't support dynamic colour. */
@Composable
internal expect fun dynamicColorScheme(darkTheme: Boolean): ColorScheme?

/** Whether to show the "Use wallpaper colours" setting at all. */
expect val supportsWallpaperColours: Boolean
