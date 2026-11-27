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
import androidx.compose.ui.unit.dp
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

/**
 * One tile per show, artwork fading in as each feed is read; failed feeds become dimmed outlines.
 * While reading, tiles appear in the order their artwork finishes loading, filling from the
 * top-left with no holes.
 * While saving, each tile's artwork starts dimmed and brightens once the show is saved. A solid
 * mosaic: square tiles with no gaps (deliberately unlike the rest of the app). Tiles are as large
 * as fit between [Sizes.ImportTileMin] and [Sizes.ImportTileMax]; a library too big for the
 * minimum shows as many as fit.
 *
 * Each tile is labelled with the show and its state, so the grid can be explored with a screen
 * reader, but nothing announces itself: the counter below stays the live progress.
 */
@Composable
internal fun ImportGrid(tiles: List<ImportTileUi>, modifier: Modifier = Modifier, revealAsLoaded: Boolean = true) {
    val shown = if (revealAsLoaded) rememberRevealOrder(tiles) else tiles
    // Top-aligned: the mosaic grows downwards and never shifts as tiles are added.
    BoxWithConstraints(contentAlignment = Alignment.TopCenter, modifier = modifier) {
        // Sized for every show from the start, so the mosaic doesn't resize as it grows.
        val layout = gridLayout(tiles.size, maxWidth, maxHeight)
        Column {
            shown.take(layout.columns * layout.rows).chunked(layout.columns).forEach { row ->
                Row {
                    row.forEach { tile -> Tile(tile, tiles.size, Modifier.size(layout.tile)) }
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

private data class GridLayout(val tile: Dp, val columns: Int, val rows: Int)

/** The largest tile size that fits every tile, or the minimum size if none does. */
private fun gridLayout(count: Int, width: Dp, height: Dp): GridLayout {
    val candidates = (Sizes.ImportTileMax.value.toInt() downTo Sizes.ImportTileMin.value.toInt()).map { it.dp }
    val layouts = candidates.map { size ->
        val columns = (width / size).toInt().coerceAtLeast(1)
        val rows = (height / size).toInt().coerceAtLeast(1)
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
