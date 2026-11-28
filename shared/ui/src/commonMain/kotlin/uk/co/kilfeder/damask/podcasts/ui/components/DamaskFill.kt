package uk.co.kilfeder.damask.podcasts.ui.components

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
 * The damask diaper drawn behind a filled play or pause button: tone-on-tone diamonds in [color]
 * at 16% (or [alpha]). [pitch] is the design's `--damask-pitch`: the repeat of its 18-unit tile, so
 * diamonds in a row are one pitch apart (9dp on 36–48dp buttons, 14dp on the 80dp Now Playing button).
 * Only filled play/pause buttons get it, so the pattern means "press here to listen". Clip the
 * button to its shape before this, so the pattern stays inside it.
 */
internal fun Modifier.damaskFill(color: Color, pitch: Dp, alpha: Float = DIAPER_ALPHA): Modifier = drawBehind {
    val step = pitch.toPx() / 2
    val halfWidth = step * HALF_WIDTH
    val halfHeight = step * HALF_HEIGHT
    val path = Path()
    val columns = (size.width / step).toInt() + 1
    val rows = (size.height / step).toInt() + 1
    for (m in -1..columns) {
        for (n in -1..rows) {
            if ((m + n) % 2 != 0) continue
            val centre = Offset(m * step, n * step)
            path.moveTo(centre.x, centre.y - halfHeight)
            path.lineTo(centre.x + halfWidth, centre.y)
            path.lineTo(centre.x, centre.y + halfHeight)
            path.lineTo(centre.x - halfWidth, centre.y)
            path.close()
        }
    }
    drawPath(path, color, alpha = alpha)
}

/**
 * A filled primary button's surface: clipped to [shape], filled with [fill], and the damask
 * pattern in [pattern] over it. Play buttons in rows, the show page's Resume / Play.
 */
internal fun Modifier.damaskButton(
    fill: Color,
    pattern: Color,
    pitch: Dp,
    shape: Shape = CircleShape,
    alpha: Float = DIAPER_ALPHA,
): Modifier = clip(shape).background(fill).damaskFill(pattern, pitch, alpha)

/** Fainter pattern under text (Resume / Play), so the words stay easy to read. */
internal const val DIAPER_UNDER_TEXT_ALPHA = 0.08f
