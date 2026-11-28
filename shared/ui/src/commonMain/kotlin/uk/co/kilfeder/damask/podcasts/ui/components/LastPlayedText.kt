package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_last
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_never
import uk.co.kilfeder.damask.podcasts.ui.resources.row_meta
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles

/**
 * "Last played 4 days ago" (the exact time on long press), or "Not played yet". [before] and
 * [after] join it with a dot: "History · Last played …", "Last played … · 4 shows".
 */
@Composable
internal fun LastPlayedText(
    last: Summarised?,
    modifier: Modifier = Modifier,
    before: String? = null,
    after: String? = null,
) {
    var text = if (last == null) {
        stringResource(Res.string.genres_never)
    } else {
        stringResource(Res.string.genres_last, last.summary)
    }
    before?.let { text = stringResource(Res.string.row_meta, it, text) }
    after?.let { text = stringResource(Res.string.row_meta, text, it) }
    val style = DamaskTextStyles.BodySmall
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    if (last == null) {
        Text(text, style = style, color = color, maxLines = 1, modifier = modifier)
    } else {
        ExactValueText(value = last, display = text, style = style, color = color, modifier = modifier)
    }
}
