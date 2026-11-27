// Copied from spec-and-design/damask-design/compose/DamaskColors.kt (generated from tokens.json).
// Re-copy when the design system changes; keep the derived roles at the bottom of each scheme.
// Default (brand) schemes only:
// on Android 12+ with "Use wallpaper colours" on, use dynamicLightColorScheme/dynamicDarkColorScheme instead.
package uk.co.kilfeder.damask.podcasts.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val DamaskLightColors = lightColorScheme(
    primary = Color(0xFF006A63),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC0EBE5),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFF4A6360),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E4),
    onSecondaryContainer = Color(0xFF324B48),
    tertiary = Color(0xFF5557A1),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE1DFFF),
    onTertiaryContainer = Color(0xFF0F0D5B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    surface = Color(0xFFF4FBF8),
    onSurface = Color(0xFF161D1C),
    onSurfaceVariant = Color(0xFF3F4947),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F3),
    surfaceContainer = Color(0xFFE9EFED),
    surfaceContainerHigh = Color(0xFFE3EAE7),
    surfaceContainerHighest = Color(0xFFDDE4E2),
    outline = Color(0xFF6F7977),
    outlineVariant = Color(0xFFBEC9C6),
    inverseSurface = Color(0xFF2B3231),
    inverseOnSurface = Color(0xFFECF2F0),
    // Not in tokens.json: derived from existing roles so nothing falls back to Material's baseline purple.
    background = Color(0xFFF4FBF8),
    onBackground = Color(0xFF161D1C),
    surfaceVariant = Color(0xFFDDE4E2),
    surfaceTint = Color(0xFF006A63),
)

internal val DamaskDarkColors = darkColorScheme(
    primary = Color(0xFF51DBCE),
    onPrimary = Color(0xFF003733),
    primaryContainer = Color(0xFF254D49),
    onPrimaryContainer = Color(0xFFC0EBE5),
    secondary = Color(0xFFB1CCC8),
    onSecondary = Color(0xFF1C3532),
    secondaryContainer = Color(0xFF324B48),
    onSecondaryContainer = Color(0xFFCCE8E4),
    tertiary = Color(0xFFC1C1FF),
    onTertiary = Color(0xFF26276F),
    tertiaryContainer = Color(0xFF3D3E87),
    onTertiaryContainer = Color(0xFFE1DFFF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    surface = Color(0xFF0E1514),
    onSurface = Color(0xFFDDE4E2),
    onSurfaceVariant = Color(0xFFBEC9C6),
    surfaceContainerLowest = Color(0xFF090F0E),
    surfaceContainerLow = Color(0xFF161D1C),
    surfaceContainer = Color(0xFF1A2120),
    surfaceContainerHigh = Color(0xFF252B2A),
    surfaceContainerHighest = Color(0xFF303635),
    outline = Color(0xFF899391),
    outlineVariant = Color(0xFF3F4947),
    inverseSurface = Color(0xFFDDE4E2),
    inverseOnSurface = Color(0xFF2B3231),
    // Not in tokens.json: derived from existing roles so nothing falls back to Material's baseline purple.
    background = Color(0xFF0E1514),
    onBackground = Color(0xFFDDE4E2),
    surfaceVariant = Color(0xFF303635),
    surfaceTint = Color(0xFF51DBCE),
)
