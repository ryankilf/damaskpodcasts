package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.animation.core.AnimationConstants
import androidx.compose.runtime.Composable

/** No OS constant here, so Compose's default (300ms). */
@Composable
internal actual fun platformMediumAnimationMillis(): Int = AnimationConstants.DefaultDurationMillis
