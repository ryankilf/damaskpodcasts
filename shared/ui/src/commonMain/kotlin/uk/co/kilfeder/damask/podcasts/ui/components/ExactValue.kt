package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised

/**
 * A summarised value ("3 days ago", "32 min left") that reveals the exact one on long press,
 * hover or focus, and reads it out to screen readers (spec › Decisions › Summarised values).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExactValueText(
    value: Summarised,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    display: String = value.summary,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = { PlainTooltip { Text(value.exact) } },
        state = rememberTooltipState(),
        modifier = modifier,
    ) {
        Text(
            text = display,
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { contentDescription = value.exact },
        )
    }
}
