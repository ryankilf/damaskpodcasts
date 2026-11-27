package uk.co.kilfeder.damask.podcasts.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// minSdk is 31 (Android 12), so dynamic colour is always available.
@Composable
internal actual fun dynamicColorScheme(darkTheme: Boolean): ColorScheme? {
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}

actual val supportsWallpaperColours: Boolean = true
