package uk.co.kilfeder.damask.podcasts.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.fail

/**
 * WCAG 2.x contrast check for every foreground/background pair in the Damask brand schemes.
 * AA requires 4.5:1 for normal text, 3:1 for large text and non-text UI (outlines, icons).
 *
 * Wallpaper (Material You) schemes are generated on device and can't be checked here; Material's
 * tonal palettes are built to meet these ratios.
 */
class ColorContrastTest {

    @Test
    fun lightSchemeMeetsWcagAa() = assertContrast("light", brandColorScheme(darkTheme = false))

    @Test
    fun darkSchemeMeetsWcagAa() = assertContrast("dark", brandColorScheme(darkTheme = true))

    private fun assertContrast(name: String, scheme: ColorScheme) {
        val failures = scheme.pairs().mapNotNull { pair ->
            val ratio = contrastRatio(pair.foreground, pair.background)
            if (ratio < pair.minimum) {
                "${pair.label}: ${ratio.format()}:1 (needs ${pair.minimum}:1)"
            } else {
                null
            }
        }
        if (failures.isNotEmpty()) {
            fail("$name colour scheme fails WCAG AA:\n" + failures.joinToString("\n"))
        }
    }

    private data class Pair(
        val label: String,
        val foreground: Color,
        val background: Color,
        val minimum: Double = TEXT,
    )

    private fun ColorScheme.pairs() = listOf(
        Pair("onPrimary/primary", onPrimary, primary),
        Pair("onPrimaryContainer/primaryContainer", onPrimaryContainer, primaryContainer),
        Pair("onSecondary/secondary", onSecondary, secondary),
        Pair("onSecondaryContainer/secondaryContainer", onSecondaryContainer, secondaryContainer),
        Pair("onTertiary/tertiary", onTertiary, tertiary),
        Pair("onTertiaryContainer/tertiaryContainer", onTertiaryContainer, tertiaryContainer),
        Pair("onError/error", onError, error),
        Pair("onErrorContainer/errorContainer", onErrorContainer, errorContainer),
        Pair("onBackground/background", onBackground, background),
        Pair("onSurface/surface", onSurface, surface),
        Pair("onSurfaceVariant/surfaceVariant", onSurfaceVariant, surfaceVariant),
        Pair("onSurfaceVariant/surface", onSurfaceVariant, surface),
        Pair("inverseOnSurface/inverseSurface", inverseOnSurface, inverseSurface),
        Pair("primary/surface (text buttons, links)", primary, surface),
        Pair("error/surface (error text)", error, surface),
        Pair("outline/surface (field borders)", outline, surface, NON_TEXT),
        // Design-system pairings (design-system/README.md).
        Pair("primary/primaryContainer (In Progress section)", primary, primaryContainer),
        Pair("tertiary/surface (category labels)", tertiary, surface),
    ) + surfaceContainers().flatMap { (name, container) ->
        listOf(
            Pair("onSurface/$name", onSurface, container),
            Pair("onSurfaceVariant/$name", onSurfaceVariant, container),
            Pair("primary/$name", primary, container),
            Pair("tertiary/$name", tertiary, container),
        )
    }

    private fun ColorScheme.surfaceContainers() = listOf(
        "surfaceContainerLowest" to surfaceContainerLowest,
        "surfaceContainerLow" to surfaceContainerLow,
        "surfaceContainer" to surfaceContainer,
        "surfaceContainerHigh" to surfaceContainerHigh,
        "surfaceContainerHighest" to surfaceContainerHighest,
    )

    private fun contrastRatio(a: Color, b: Color): Double {
        val la = a.luminance().toDouble()
        val lb = b.luminance().toDouble()
        return (max(la, lb) + LUMINANCE_OFFSET) / (min(la, lb) + LUMINANCE_OFFSET)
    }

    private fun Double.format(): String = (kotlin.math.round(this * 100) / 100).toString()

    private companion object {
        const val TEXT = 4.5
        const val NON_TEXT = 3.0
        const val LUMINANCE_OFFSET = 0.05
    }
}
