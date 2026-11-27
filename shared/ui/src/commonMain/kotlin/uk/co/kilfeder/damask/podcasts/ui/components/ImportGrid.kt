package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.importer.ImportTileUi
import uk.co.kilfeder.damask.podcasts.presentation.importer.TileStatus
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.import_tile_failed
import uk.co.kilfeder.damask.podcasts.ui.resources.import_tile_read
import uk.co.kilfeder.damask.podcasts.ui.resources.import_tile_unnamed
import uk.co.kilfeder.damask.podcasts.ui.resources.import_tile_waiting
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

private const val FAILED_ALPHA = 0.38f

/**
 * One tile per show, filling with artwork as each feed is read; failed feeds become dimmed
 * outlines. Tiles are as large as fit between [Sizes.ImportTileMin] and [Sizes.ImportTileMax];
 * a library too big for the minimum shows as many as fit.
 *
 * Each tile is labelled with the show and its state, so the grid can be explored with a screen
 * reader, but nothing announces itself: the counter below stays the live progress.
 */
@Composable
internal fun ImportGrid(tiles: List<ImportTileUi>, modifier: Modifier = Modifier) {
    BoxWithConstraints(contentAlignment = Alignment.Center, modifier = modifier) {
        val layout = gridLayout(tiles.size, maxWidth, maxHeight)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1)) {
            tiles.take(layout.columns * layout.rows).chunked(layout.columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Space1)) {
                    row.forEach { tile -> Tile(tile, tiles.size, Modifier.size(layout.tile)) }
                }
            }
        }
    }
}

private data class GridLayout(val tile: Dp, val columns: Int, val rows: Int)

/** The largest tile size that fits every tile, or the minimum size if none does. */
private fun gridLayout(count: Int, width: Dp, height: Dp): GridLayout {
    val gap = Spacing.Space1
    val candidates = (Sizes.ImportTileMax.value.toInt() downTo Sizes.ImportTileMin.value.toInt()).map { it.dp }
    val layouts = candidates.map { size ->
        val columns = ((width + gap) / (size + gap)).toInt().coerceAtLeast(1)
        val rows = ((height + gap) / (size + gap)).toInt().coerceAtLeast(1)
        GridLayout(size, columns, rows)
    }
    return layouts.firstOrNull { it.columns * it.rows >= count } ?: layouts.last()
}

@Composable
private fun Tile(tile: ImportTileUi, total: Int, modifier: Modifier = Modifier) {
    val name = tile.name ?: stringResource(Res.string.import_tile_unnamed, tile.position, total)
    val label = when (tile.status) {
        TileStatus.WAITING -> stringResource(Res.string.import_tile_waiting, name)
        TileStatus.READ -> stringResource(Res.string.import_tile_read, name)
        TileStatus.FAILED -> stringResource(Res.string.import_tile_failed, name)
    }
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .semantics { contentDescription = label },
    ) {
        when (tile.status) {
            TileStatus.WAITING -> Unit
            TileStatus.READ -> AsyncImage(
                model = tile.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
            TileStatus.FAILED -> Box(
                Modifier
                    .matchParentSize()
                    .alpha(FAILED_ALPHA)
                    .border(Sizes.ProgressRingStroke, MaterialTheme.colorScheme.outline, shape),
            )
        }
    }
}
