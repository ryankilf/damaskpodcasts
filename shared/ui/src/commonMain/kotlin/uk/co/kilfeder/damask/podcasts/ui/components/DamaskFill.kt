package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp

/** The diaper pattern's opacity on filled play buttons (design-system README › Damask). */
private const val DIAPER_ALPHA = 0.16f

/**
 * A diamond's half-width and half-height as fractions of the grid step (4 and 7 on a 9-unit step).
 * The design's tile is 18 units square, scaled to the pitch, so the step is half the pitch.
 */
private const val HALF_WIDTH = 4f / 9f
private const val HALF_HEIGHT = 7f / 9f

/**
 * The damask diaper: diamonds in [color] at [alpha] (play buttons pass a solid tone from
 * [damaskTone]; the empty-state tile a faint wash). [pitch] is the design's `--damask-pitch`:
 * the repeat of its 18-unit tile, so
 * diamonds in a row are one pitch apart (9dp on 36–48dp buttons, 14dp on the 80dp Now Playing button).
 * Only filled play/pause buttons get it, so the pattern means "press here to listen". Clip the
 * button to its shape before this, so the pattern stays inside it. [drift] (0..1, read while
 * drawing) slides it sideways by up to two pitches, which repeats seamlessly: skeleton rows drift.
 */
internal fun Modifier.damaskFill(
    color: Color,
    pitch: Dp,
    alpha: Float = DIAPER_ALPHA,
    drift: () -> Float = { 0f },
): Modifier = drawBehind {
    val step = pitch.toPx() / 2
    val shift = drift() * step * DRIFT_STEPS
    val halfWidth = step * HALF_WIDTH
    val halfHeight = step * HALF_HEIGHT
    val path = Path()
    val columns = (size.width / step).toInt() + 1
    val rows = (size.height / step).toInt() + 1
    // Start far enough left that the drift (up to two pitches, four steps) never leaves a gap.
    for (m in -DRIFT_STEPS - 1..columns) {
        for (n in -1..rows) {
            if ((m + n) % 2 != 0) continue
            val centre = Offset(m * step + shift, n * step)
            path.moveTo(centre.x, centre.y - halfHeight)
            path.lineTo(centre.x + halfWidth, centre.y)
            path.lineTo(centre.x, centre.y + halfHeight)
            path.lineTo(centre.x - halfWidth, centre.y)
            path.close()
        }
    }
    drawPath(path, color, alpha = alpha)
}

/** How many grid steps a full drift slides the pattern (two pitches: it then repeats exactly). */
private const val DRIFT_STEPS = 2

/** How far a play button's pattern moves from its fill towards its content colour. */
internal const val DAMASK_TONE = 0.09f

/** Gentler under words (Resume / Play), so the text stays easy to read. */
internal const val DAMASK_TONE_UNDER_TEXT = 0.04f

/**
 * A play button's pattern colour, worked out from its own colours rather than a palette, so it
 * suits any theme (wallpaper colours, the user's own): the fill nudged a little towards
 * [onFill]. The two tones stay close, so text and icons on top read cleanly.
 */
internal fun damaskTone(fill: Color, onFill: Color, strength: Float = DAMASK_TONE): Color =
    lerp(fill, onFill, strength)

/**
 * A filled play button's surface: clipped to [shape], filled with [fill], and the damask in a
 * tone just off it ([damaskTone]). Row play buttons, the show and genre pages' Resume / Play.
 */
internal fun Modifier.damaskButton(
    fill: Color,
    onFill: Color,
    pitch: Dp,
    shape: Shape = CircleShape,
    strength: Float = DAMASK_TONE,
): Modifier = clip(shape).background(fill).damaskFill(damaskTone(fill, onFill, strength), pitch, alpha = 1f)
