package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.runtime.Composable

/**
 * The OS's standard duration for a medium-length transition, where it has one; otherwise
 * Compose's default. On Android, Compose also applies the user's animation scale on top.
 */
@Composable
internal expect fun platformMediumAnimationMillis(): Int
