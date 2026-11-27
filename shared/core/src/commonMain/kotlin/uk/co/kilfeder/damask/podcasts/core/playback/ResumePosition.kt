package uk.co.kilfeder.damask.podcasts.core.playback

import uk.co.kilfeder.damask.podcasts.core.model.PositionState
import kotlin.math.abs

/**
 * Dynamic ad insertion guard (spec › Storage). Ads vary in length per fetch, so two devices' copies
 * of an episode can be offset. When the saved position came from a copy whose length differs from
 * the local one by more than [MISMATCH_TOLERANCE_MS], rewind [REWIND_MS]: you may hear a little
 * again, but never skip anything.
 */
object ResumePosition {
    const val MISMATCH_TOLERANCE_MS = 5_000L
    const val REWIND_MS = 30_000L

    fun resumeAtMs(saved: PositionState, localDurationMs: Long?): Long {
        val savedOn = saved.durationAtSaveMs ?: return saved.positionMs
        if (localDurationMs == null) return saved.positionMs
        val mismatched = abs(savedOn - localDurationMs) > MISMATCH_TOLERANCE_MS
        return if (mismatched) (saved.positionMs - REWIND_MS).coerceAtLeast(0) else saved.positionMs
    }
}
