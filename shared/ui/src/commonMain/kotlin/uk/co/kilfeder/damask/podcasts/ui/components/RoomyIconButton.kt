package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.DpSize
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

/**
 * Looks and ripples like an icon button, but its touch target is [size]: the room around it (a
 * row's full height, out to the screen edge), not just 48dp. The icon doesn't move or grow.
 */
@Composable
internal fun RoomyIconButton(
    onClick: () -> Unit,
    label: String,
    size: DpSize,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = null,
                indication = ripple(bounded = false, radius = Sizes.IconRipple),
                onClick = onClick,
            )
            .semantics { contentDescription = label },
    ) { content() }
}
