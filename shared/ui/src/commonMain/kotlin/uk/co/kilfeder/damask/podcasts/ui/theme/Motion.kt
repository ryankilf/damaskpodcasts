package uk.co.kilfeder.damask.podcasts.ui.theme

/**
 * Timings for moving between screens: Material's "fade through". The old screen fades out
 * quickly, then the new one fades in, so the two never sit visibly on top of each other.
 */
object Motion {
    /** A skeleton row's damask drifting one repeat sideways (design: 2.4 s, linear, forever). */
    const val SKELETON_DRIFT_MS = 2400

    /** The old screen fading out, in milliseconds. */
    const val FADE_OUT_MS = 90

    /** The new screen fading in, after the old one has gone. */
    const val FADE_IN_MS = 210

    /** Play ↔ pause on every play button: the old icon fades out first… */
    const val PLAY_PAUSE_OUT_MS = 60

    /** …then the new one fades and grows in, so the two never overlap. */
    const val PLAY_PAUSE_IN_MS = 120

    /** Where the new play/pause icon grows from. */
    const val PLAY_PAUSE_START_SCALE = 0.85f
}
