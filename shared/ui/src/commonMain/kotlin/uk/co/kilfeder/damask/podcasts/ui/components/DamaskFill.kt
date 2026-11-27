package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.Dp

/** The diaper pattern's opacity on filled play buttons (design-system README › Damask). */
private const val DIAPER_ALPHA = 0.16f

/** A diamond's half-width and half-height as fractions of the pitch (4 and 7 on a 9-unit grid). */
private const val HALF_WIDTH = 4f / 9f
private const val HALF_HEIGHT = 7f / 9f

/**
 * The damask diaper drawn behind a filled play or pause button: tone-on-tone diamonds in [color]
 * at 16%, every [pitch] (about 9dp on 36–48dp buttons, 14dp on the 80dp Now Playing button).
 * Only filled play/pause buttons get it, so the pattern means "press here to listen". Clip the
 * button to its shape before this, so the pattern stays inside it.
 */
internal fun Modifier.damaskFill(color: Color, pitch: Dp): Modifier = drawBehind {
    val step = pitch.toPx()
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
    drawPath(path, color, alpha = DIAPER_ALPHA)
}
