package uk.co.kilfeder.damask.podcasts.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// design-system `type` tokens. Platform font only (FontFamily.Default), per the spec's Fonts decision.

private fun style(size: Int, lineHeight: Int, weight: FontWeight, features: String? = null) = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    fontWeight = weight,
    fontFeatureSettings = features,
)

/** The named styles from the design system, for when a Material slot name would be misleading. */
object DamaskTextStyles {
    /** display: Now Playing only. */
    val Display = style(size = 32, lineHeight = 38, weight = FontWeight.Bold)
    val TitleLarge = style(size = 22, lineHeight = 28, weight = FontWeight.SemiBold)

    /** title: episode titles (two lines, then truncate). */
    val Title = style(size = 17, lineHeight = 24, weight = FontWeight.SemiBold)
    val Body = style(size = 15, lineHeight = 22, weight = FontWeight.Normal)

    /** body-sm: show name and date, in onSurfaceVariant. */
    val BodySmall = style(size = 13, lineHeight = 18, weight = FontWeight.Normal)
    val Label = style(size = 13, lineHeight = 16, weight = FontWeight.SemiBold)

    /** time: durations and time left, with tabular figures. */
    val Time = style(size = 12, lineHeight = 16, weight = FontWeight.Medium, features = "tnum")
}

/** Design tokens mapped onto Material 3 slots so stock components pick them up. */
internal val DamaskTypography = Typography(
    displaySmall = DamaskTextStyles.Display,
    titleLarge = DamaskTextStyles.TitleLarge,
    titleMedium = DamaskTextStyles.Title,
    bodyLarge = DamaskTextStyles.Body,
    bodyMedium = DamaskTextStyles.Body,
    bodySmall = DamaskTextStyles.BodySmall,
    labelLarge = DamaskTextStyles.Label,
    labelMedium = DamaskTextStyles.Label,
    labelSmall = DamaskTextStyles.Time,
)
