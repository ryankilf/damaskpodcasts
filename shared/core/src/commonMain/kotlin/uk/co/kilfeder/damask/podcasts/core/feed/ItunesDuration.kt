package uk.co.kilfeder.damask.podcasts.core.feed

import kotlin.math.roundToLong

/** `itunes:duration`: `HH:MM:SS`, `MM:SS`, or seconds (sometimes fractional). */
internal object ItunesDuration {
    private const val SECOND_MS = 1_000.0
    private const val SIXTY = 60
    private const val MAX_PARTS = 3

    fun parseMs(raw: String): Long? {
        val parts = raw.trim().split(':')
        if (parts.size > MAX_PARTS) return null
        val numbers = parts.map { it.trim().toDoubleOrNull() ?: return null }
        if (numbers.any { it < 0 }) return null
        val seconds = numbers.fold(0.0) { total, part -> total * SIXTY + part }
        return (seconds * SECOND_MS).roundToLong().takeIf { it > 0 }
    }
}
