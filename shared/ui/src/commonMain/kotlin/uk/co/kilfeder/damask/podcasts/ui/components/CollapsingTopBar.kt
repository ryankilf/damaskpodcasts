package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.animation.fadeOut
import androidx.compose.animation.fadeIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Motion
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * The big heading → small sticky heading pattern (show and genre pages): a bar that stays put
 * with back and actions, always the page's colour. Once the page's big title has scrolled under
 * it, the bar shows the page small: [art] and [title] fade in, and [collapsedAction] (the round
 * play button, standing in for the big one that has scrolled away) appears before [actions].
 * Screen readers already have the big title, so the small one is hidden from them.
 */
@Composable
internal fun CollapsingTopBar(
    state: CollapsingTitleState,
    title: String,
    modifier: Modifier = Modifier,
    background: Color = MaterialTheme.colorScheme.surface,
    onBack: (() -> Unit)? = null,
    art: (@Composable () -> Unit)? = null,
    collapsedAction: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val collapsed = state.collapsed
    val shownAlpha by animateFloatAsState(
        targetValue = if (collapsed) 1f else 0f,
        animationSpec = tween(if (collapsed) Motion.FADE_IN_MS else Motion.FADE_OUT_MS),
        label = "barTitle",
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .heightIn(min = Sizes.PaneHeaderHeight)
            .padding(horizontal = Spacing.Space1),
    ) {
        if (onBack != null) BackButton(onBack) else Spacer(Modifier.width(Spacing.Space3))
        // Always there, taking the free space so the actions stay at the end; it fades in and out.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Space3),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = Spacing.Space1)
                .graphicsLayer { alpha = shownAlpha }
                .clearAndSetSemantics { },
        ) {
            art?.invoke()
            Text(
                text = title,
                style = DamaskTextStyles.Title,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        // Only there when collapsed, so a hidden button can't be pressed.
        // Room after it: as far from the edge as the back arrow when it's last, apart from a menu
        // when not. (The filled circle reaches its bounds; an icon button's glyph doesn't.)
        AnimatedVisibility(
            visible = collapsed && collapsedAction != null,
            enter = fadeIn(tween(Motion.FADE_IN_MS)),
            exit = fadeOut(tween(Motion.FADE_OUT_MS)),
            modifier = Modifier.padding(end = Spacing.Space3),
        ) { collapsedAction?.invoke() }
        actions()
    }
}
