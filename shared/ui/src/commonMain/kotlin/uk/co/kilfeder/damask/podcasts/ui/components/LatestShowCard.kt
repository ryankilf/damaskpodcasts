package uk.co.kilfeder.damask.podcasts.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.min
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.importer.LatestShowUi
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.import_latest
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** How long each cover stays still after its slide finishes. */
private const val DWELL_MS = 3_000L

/**
 * The newest show's cover on an elevated card, a companion to the import progress bar. A newer
 * show slides in from the end edge and pushes the current one out towards the start, at most
 * once per slide-plus-[DWELL_MS]; covers in between are skipped. Each image is downloaded before
 * it slides in. The slide uses the OS's standard duration ([platformMediumAnimationMillis]).
 *
 * Decorative for screen readers apart from its label ("Latest: …"); it never announces itself,
 * so the counter stays the live progress.
 */
@Composable
internal fun LatestShowCard(latest: LatestShowUi?, modifier: Modifier = Modifier) {
    val slideMs = platformMediumAnimationMillis()
    val shown = rememberPacedLatest(latest, slideMs)
    val fromEnd = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    BoxWithConstraints(contentAlignment = Alignment.Center, modifier = modifier) {
        val side = min(min(maxWidth, maxHeight * CARD_SHARE_OF_HEIGHT), Sizes.ImportCoverMax)
        AnimatedContent(
            targetState = shown,
            transitionSpec = {
                slideInHorizontally(tween(slideMs)) { width -> width * fromEnd } togetherWith
                    slideOutHorizontally(tween(slideMs)) { width -> -width * fromEnd }
            },
            label = "latest-show",
        ) { show ->
            if (show != null) Cover(show, side)
        }
    }
}

/** At most one change per slide + dwell; always moves on to the newest show available. */
@Composable
private fun rememberPacedLatest(latest: LatestShowUi?, slideMs: Int): LatestShowUi? {
    val context = LocalPlatformContext.current
    val loader = SingletonImageLoader.get(context)
    val newest by rememberUpdatedState(latest)
    var shown by remember { mutableStateOf<LatestShowUi?>(null) }
    LaunchedEffect(slideMs) {
        while (true) {
            val next = snapshotFlow { newest }.filterNotNull().filter { it != shown }.first()
            loader.execute(ImageRequest.Builder(context).data(next.artworkUrl).build())
            shown = next
            delay(slideMs + DWELL_MS)
        }
    }
    return shown
}

@Composable
private fun Cover(show: LatestShowUi, side: Dp, modifier: Modifier = Modifier) {
    val label = show.name?.let { stringResource(Res.string.import_latest, it) }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.Space2),
        modifier = modifier.semantics { label?.let { contentDescription = it } },
    ) {
        ElevatedCard(shape = MaterialTheme.shapes.medium, modifier = Modifier.size(side)) {
            AsyncImage(
                model = show.artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.aspectRatio(1f),
            )
        }
        show.name?.let {
            Text(
                text = it,
                style = DamaskTextStyles.Title,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.width(side),
            )
        }
    }
}

/** Leave room under the card for the show's name. */
private const val CARD_SHARE_OF_HEIGHT = 0.75f
