package uk.co.kilfeder.damask.podcasts.presentation.format

/** How far through: 0..1, or null when the length isn't known. */
internal fun fraction(positionMs: Long?, totalMs: Long?): Float? {
    if (positionMs == null || totalMs == null || totalMs <= 0) return null
    return (positionMs.toFloat() / totalMs).coerceIn(0f, 1f)
}
