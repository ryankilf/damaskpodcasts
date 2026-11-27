package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** `android.R.integer.config_mediumAnimTime` (400ms on stock Android). */
@Composable
internal actual fun platformMediumAnimationMillis(): Int =
    LocalContext.current.resources.getInteger(android.R.integer.config_mediumAnimTime)
