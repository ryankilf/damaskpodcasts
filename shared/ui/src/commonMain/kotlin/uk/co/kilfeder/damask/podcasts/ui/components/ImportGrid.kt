package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.importer.ImportTileUi
import uk.co.kilfeder.damask.podcasts.presentation.importer.TileStatus
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.import_tile_failed
import uk.co.kilfeder.damask.podcasts.ui.resources.import_tile_read
import uk.co.kilfeder.damask.podcasts.ui.resources.import_tile_saved
import uk.co.kilfeder.damask.podcasts.ui.resources.import_tile_unnamed
import uk.co.kilfeder.damask.podcasts.ui.resources.import_tile_waiting
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

private const val DIM_ALPHA = 0.38f
private const val FADE_IN_MS = 400

/** How much taller than square a tile may grow to fill leftover height. */
private const val MAX_STRETCH = 1.15f

/**
 * One tile per show, artwork fading in as each feed is read; failed feeds become dimmed outlines.
 * While reading, tiles appear in the order their artwork finishes loading, filling from the
 * top-left with no holes.
 * While saving, each tile's artwork starts dimmed and brightens once the show is saved. A solid
 * mosaic: tiles with no gaps or rounding (deliberately unlike the rest of the app), sized to fill
 * the space once complete (see [gridLayout]).
 *
 * Each tile is labelled with the show and its state, so the grid can be explored with a screen
 * reader, but nothing announces itself: the counter below stays the live progress.
 */
@Composable
internal fun ImportGrid(tiles: List<ImportTileUi>, modifier: Modifier = Modifier, revealAsLoaded: Boolean = true) {
    val shown = if (revealAsLoaded) rememberRevealOrder(tiles) else tiles
    // Anchored top-start: the mosaic grows from the top-left and never shifts as tiles are added.
    BoxWithConstraints(contentAlignment = Alignment.TopStart, modifier = modifier) {
        // Sized for every show from the start, so the mosaic doesn't resize as it grows.
        val layout = gridLayout(tiles.size, maxWidth, maxHeight)
        Column {
            shown.take(layout.columns * layout.rows).chunked(layout.columns).forEach { row ->
                Row {
                    row.forEach { tile -> Tile(tile, tiles.size, Modifier.size(layout.tileWidth, layout.tileHeight)) }
                }
            }
        }
    }
}

/**
 * The tiles in the order their artwork finished downloading, so the mosaic fills from the
 * top-left with no holes. A tile joins once its image is loaded, or straight away when its feed
 * failed or has no artwork.
 */
@Composable
private fun rememberRevealOrder(tiles: List<ImportTileUi>): List<ImportTileUi> {
    val context = LocalPlatformContext.current
    val loader = SingletonImageLoader.get(context)
    val order = remember { mutableStateListOf<Int>() }
    tiles.forEach { tile ->
        key(tile.position) {
            LaunchedEffect(tile.status, tile.artworkUrl) {
                if (tile.position in order || tile.status == TileStatus.WAITING) return@LaunchedEffect
                tile.artworkUrl?.let { loader.execute(ImageRequest.Builder(context).data(it).build()) }
                if (tile.position !in order) order += tile.position
            }
        }
    }
    val byPosition = tiles.associateBy { it.position }
    return order.mapNotNull { byPosition[it] }
}

private data class GridLayout(val tileWidth: Dp, val tileHeight: Dp, val columns: Int, val rows: Int)

/**
 * Fills the space: tiles always span the full width, and grow up to [MAX_STRETCH] taller than
 * square to take up leftover height (artwork is cropped, so a little stretch is invisible). The
 * column count is the one giving the biggest tiles. Below [Sizes.ImportTileMin] the tiles stop
 * shrinking and as many shows as fit are shown.
 */
private fun gridLayout(count: Int, width: Dp, height: Dp): GridLayout {
    val n = count.coerceAtLeast(1)
    val best = (1..n)
        .map { columns -> columns to (n + columns - 1) / columns }
        .maxBy { (columns, rows) -> minOf(width / columns, height / rows) }
    var (columns, rows) = best
    if (minOf(width / columns, height / rows) < Sizes.ImportTileMin) {
        columns = (width / Sizes.ImportTileMin).toInt().coerceAtLeast(1)
        rows = (height / Sizes.ImportTileMin).toInt().coerceAtLeast(1)
    }
    val tileWidth = width / columns
    val tileHeight = minOf(height / rows, tileWidth * MAX_STRETCH)
    return GridLayout(tileWidth, tileHeight, columns, rows)
}

@Composable
private fun Tile(tile: ImportTileUi, total: Int, modifier: Modifier = Modifier) {
    val name = tile.name ?: stringResource(Res.string.import_tile_unnamed, tile.position, total)
    val label = when (tile.status) {
        TileStatus.WAITING -> stringResource(Res.string.import_tile_waiting, name)
        TileStatus.READ -> stringResource(Res.string.import_tile_read, name)
        TileStatus.FAILED -> stringResource(Res.string.import_tile_failed, name)
        TileStatus.SAVED -> stringResource(Res.string.import_tile_saved, name)
    }
    // Waiting tiles with artwork (while saving) show it dimmed, brightening once saved.
    val brightness by animateFloatAsState(if (tile.status == TileStatus.WAITING) DIM_ALPHA else 1f)
    // Each tile fades in as it appears. Its image is already in memory by then, and Coil doesn't
    // crossfade memory-cache hits, so the tile does it.
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(FADE_IN_MS)) }
    Box(
        modifier = modifier
            .graphicsLayer { alpha = appear.value }
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .semantics { contentDescription = label },
    ) {
        if (tile.status == TileStatus.FAILED) {
            Box(
                Modifier
                    .matchParentSize()
                    .alpha(DIM_ALPHA)
                    .border(Sizes.ProgressRingStroke, MaterialTheme.colorScheme.outline),
            )
        } else if (tile.artworkUrl != null) {
            AsyncImage(
                model = tile.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize().alpha(brightness),
            )
        }
    }
}
