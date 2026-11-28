package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.ui.theme.isDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.ThemeMode
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.theme_and_colours
import uk.co.kilfeder.damask.podcasts.ui.resources.theme_automatic
import uk.co.kilfeder.damask.podcasts.ui.resources.theme_colours
import uk.co.kilfeder.damask.podcasts.ui.resources.theme_damask
import uk.co.kilfeder.damask.podcasts.ui.resources.theme_dark
import uk.co.kilfeder.damask.podcasts.ui.resources.theme_light
import uk.co.kilfeder.damask.podcasts.ui.resources.theme_mode
import uk.co.kilfeder.damask.podcasts.ui.resources.theme_your_colours
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing
import uk.co.kilfeder.damask.podcasts.ui.theme.brandColorScheme
import uk.co.kilfeder.damask.podcasts.ui.theme.dynamicColorScheme
import uk.co.kilfeder.damask.podcasts.ui.theme.supportsWallpaperColours

/** A mode's name in the control and in the Settings row. */
internal val ThemeMode.label: StringResource
    get() = when (this) {
        ThemeMode.LIGHT -> Res.string.theme_light
        ThemeMode.DARK -> Res.string.theme_dark
        ThemeMode.AUTOMATIC -> Res.string.theme_automatic
    }

/**
 * Theme and colours (design hand-off › Appearance), a sheet over Settings: Mode as a three-way
 * segmented control (Light · Dark · Automatic, which follows the system), then two colour tiles,
 * "Your colours" (Material You) and "Damask". "Your colours" is only offered where the device
 * supplies dynamic colour; elsewhere there's just Damask and Mode.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemeSheet(
    mode: ThemeMode,
    yourColours: Boolean,
    onModeChange: (ThemeMode) -> Unit,
    onYourColoursChange: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Space6),
            modifier = Modifier.padding(start = Spacing.Space6, end = Spacing.Space6, bottom = Spacing.Space8),
        ) {
            Text(
                stringResource(Res.string.theme_and_colours),
                style = DamaskTextStyles.Title,
                modifier = Modifier.semantics { heading() },
            )
            Section(stringResource(Res.string.theme_mode)) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEachIndexed { i, option ->
                        SegmentedButton(
                            selected = option == mode,
                            onClick = { onModeChange(option) },
                            shape = SegmentedButtonDefaults.itemShape(i, ThemeMode.entries.size),
                        ) { Text(stringResource(option.label)) }
                    }
                }
            }
            if (supportsWallpaperColours) {
                Section(stringResource(Res.string.theme_colours)) {
                    val dark = isDarkTheme()
                    val wallpaper = dynamicColorScheme(dark)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Space3),
                        modifier = Modifier.selectableGroup(),
                    ) {
                        if (wallpaper != null) {
                            val pickYours = { onYourColoursChange(true) }
                            ColourTile(Res.string.theme_your_colours, wallpaper, yourColours, pickYours)
                        }
                        val damask = brandColorScheme(dark)
                        ColourTile(Res.string.theme_damask, damask, !yourColours, { onYourColoursChange(false) })
                    }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space3), modifier = modifier) {
        Text(
            text = title,
            style = DamaskTextStyles.Label,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

/** A colour choice: three dots from its scheme and its name; the chosen one outlined in primary. */
@Composable
private fun RowScope.ColourTile(
    name: StringResource,
    scheme: ColorScheme,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Space3),
        modifier = modifier
            .weight(1f)
            .background(if (selected) colors.surfaceContainerHigh else colors.surfaceContainerLow, shape)
            .border(
                if (selected) Sizes.GenreRingWidth else Sizes.ArtworkHairline,
                if (selected) colors.primary else colors.outlineVariant,
                shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(Spacing.Space4),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Space1)) {
            listOf(scheme.primary, scheme.primaryContainer, scheme.tertiary).forEach {
                Box(Modifier.size(Sizes.ThemeDot).background(it, CircleShape))
            }
        }
        Text(stringResource(name), style = DamaskTextStyles.Title, color = colors.onSurface)
    }
}
