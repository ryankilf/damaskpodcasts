package uk.co.kilfeder.damask.podcasts.ui.components

import uk.co.kilfeder.damask.podcasts.ui.theme.genreSwatch
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue
import uk.co.kilfeder.damask.podcasts.presentation.genres.PickableHues
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.colour_for
import uk.co.kilfeder.damask.podcasts.ui.resources.done
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_amber
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_blue
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_brown
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_cyan
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_green
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_grey
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_indigo
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_lime
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_magenta
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_orange
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_pink
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_purple
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_red
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_slate
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_teal
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_violet
import uk.co.kilfeder.damask.podcasts.ui.resources.hue_yellow
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing
import uk.co.kilfeder.damask.podcasts.ui.theme.genreTones

/** Swatches per row in the colour grid (design hand-off › ManageCategoriesColour: 4 × 4). */
private const val SWATCHES_PER_ROW = 4

/** A colour's name, for its label and what screen readers say. */
@Composable
internal fun hueName(hue: GenreHue): String = stringResource(HUE_NAMES.getValue(hue))

private val HUE_NAMES: Map<GenreHue, StringResource> = mapOf(
    GenreHue.RED to Res.string.hue_red,
    GenreHue.ORANGE to Res.string.hue_orange,
    GenreHue.AMBER to Res.string.hue_amber,
    GenreHue.YELLOW to Res.string.hue_yellow,
    GenreHue.LIME to Res.string.hue_lime,
    GenreHue.GREEN to Res.string.hue_green,
    GenreHue.TEAL to Res.string.hue_teal,
    GenreHue.CYAN to Res.string.hue_cyan,
    GenreHue.BLUE to Res.string.hue_blue,
    GenreHue.INDIGO to Res.string.hue_indigo,
    GenreHue.VIOLET to Res.string.hue_violet,
    GenreHue.PURPLE to Res.string.hue_purple,
    GenreHue.MAGENTA to Res.string.hue_magenta,
    GenreHue.PINK to Res.string.hue_pink,
    GenreHue.BROWN to Res.string.hue_brown,
    GenreHue.SLATE to Res.string.hue_slate,
    GenreHue.GREY to Res.string.hue_grey,
)

/**
 * Picking a genre's colour (design hand-off › ManageCategoriesColour): "Colour for History" with a
 * live preview chip, the 16 swatches in a 4 × 4 grid, each named, and Done. No free colour picker.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun GenreColourSheet(
    name: String,
    selected: GenreHue,
    onDone: (GenreHue) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pick by remember { mutableStateOf(selected) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Space4),
            modifier = Modifier.padding(start = Spacing.Space6, end = Spacing.Space6, bottom = Spacing.Space6),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.colour_for, name),
                    style = DamaskTextStyles.Title,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                GenreChip(name, pick)
            }
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space3), modifier = Modifier.selectableGroup()) {
                PickableHues.chunked(SWATCHES_PER_ROW).forEach { row ->
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        row.forEach { hue -> Swatch(hue, hue == pick, onClick = { pick = hue }) }
                    }
                }
            }
            Button(onClick = { onDone(pick) }, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(Res.string.done))
            }
        }
    }
}

/** One colour: a filled circle, ringed with a check when it's the one picked, and its name. */
@Composable
private fun Swatch(hue: GenreHue, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Space1),
        modifier = modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(Sizes.MinTouchTarget)
                .then(if (selected) Modifier.border(Sizes.GenreRingWidth, colors.onSurface, CircleShape) else Modifier)
                .padding(if (selected) Spacing.Space1 else Sizes.None)
                .genreSwatch(genreTones(hue), colors.outlineVariant, CircleShape),
        ) {
            if (selected) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(Sizes.SwatchCheck).background(colors.surface, CircleShape),
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = colors.onSurface)
                }
            }
        }
        Text(
            text = hueName(hue),
            style = DamaskTextStyles.BodySmall,
            color = if (selected) colors.onSurface else colors.onSurfaceVariant,
        )
    }
}
