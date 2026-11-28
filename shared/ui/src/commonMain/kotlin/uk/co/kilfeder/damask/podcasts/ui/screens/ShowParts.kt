package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.presentation.episodes.NextUpUi
import uk.co.kilfeder.damask.podcasts.ui.components.PlayNextButton
import uk.co.kilfeder.damask.podcasts.ui.components.DropdownChip
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised
import uk.co.kilfeder.damask.podcasts.presentation.shows.Cadence
import uk.co.kilfeder.damask.podcasts.presentation.genres.CategoryOptionUi
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowPageUi
import uk.co.kilfeder.damask.podcasts.ui.components.Artwork
import uk.co.kilfeder.damask.podcasts.ui.components.ExactValueText
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_last
import uk.co.kilfeder.damask.podcasts.ui.resources.show_cadence_daily
import uk.co.kilfeder.damask.podcasts.ui.resources.show_cadence_fortnightly
import uk.co.kilfeder.damask.podcasts.ui.resources.show_cadence_monthly
import uk.co.kilfeder.damask.podcasts.ui.resources.show_cadence_weekly
import uk.co.kilfeder.damask.podcasts.ui.resources.show_genre_label
import uk.co.kilfeder.damask.podcasts.ui.resources.show_last_exact
import uk.co.kilfeder.damask.podcasts.ui.resources.show_meta_sep
import uk.co.kilfeder.damask.podcasts.ui.resources.show_more
import uk.co.kilfeder.damask.podcasts.ui.resources.show_more_label
import uk.co.kilfeder.damask.podcasts.ui.resources.show_subscribe
import uk.co.kilfeder.damask.podcasts.ui.resources.show_usual_day
import uk.co.kilfeder.damask.podcasts.ui.resources.shows_untitled
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/** Runs of whitespace, paragraph breaks included. */
private val WHITESPACE = Regex("\\s+")

/** Lines of description shown before "More". */
private const val DESCRIPTION_LINES = 3

/** Artwork, name, maker, "Weekly · 86 episodes · Last played today", and the genre chip. */
@Composable
internal fun ShowDetails(
    show: ShowPageUi,
    genres: List<CategoryOptionUi>,
    onGenreChange: (String) -> Unit,
    create: CreateGenre,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space4),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(horizontal = Spacing.Space4),
    ) {
        val artShape = MaterialTheme.shapes.medium
        Artwork(show.artworkUrl, show.fallback, size = Sizes.ShowHeaderArtwork, shape = artShape)
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Space1), modifier = Modifier.weight(1f)) {
            Text(
                text = show.title.ifBlank { stringResource(Res.string.shows_untitled) },
                style = DamaskTextStyles.TitleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = titleModifier.semantics { heading() },
            )
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            if (show.author.isNotBlank()) {
                Text(
                    text = show.author,
                    style = DamaskTextStyles.BodySmall,
                    color = muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val facts = showFacts(show)
            if (facts.summary.isNotEmpty()) {
                ExactValueText(value = facts, style = DamaskTextStyles.BodySmall, color = muted)
            }
            if (show.subscribed) GenreMenuChip(show, genres, onGenreChange, create)
        }
    }
}

/**
 * The facts line, with the exact values for long press (publish day, last played). No episode
 * count: we only know what the feed lists and what we keep, neither of which is the show's.
 */
@Composable
private fun showFacts(show: ShowPageUi): Summarised {
    val cadence = when (show.cadence) {
        Cadence.DAILY -> stringResource(Res.string.show_cadence_daily)
        Cadence.WEEKLY -> stringResource(Res.string.show_cadence_weekly)
        Cadence.FORTNIGHTLY -> stringResource(Res.string.show_cadence_fortnightly)
        Cadence.MONTHLY -> stringResource(Res.string.show_cadence_monthly)
        null -> null
    }
    val last = show.lastPlayed.takeIf { show.subscribed }
    val summary = listOfNotNull(cadence, last?.let { stringResource(Res.string.genres_last, it.summary) })
    val exact = listOfNotNull(
        show.usualDay?.let { stringResource(Res.string.show_usual_day, it) } ?: cadence,
        last?.let { stringResource(Res.string.show_last_exact, it.exact) },
    )
    val sep = stringResource(Res.string.show_meta_sep)
    return Summarised(summary.joinToString(sep), exact.joinToString(sep))
}

/** "History ▾": moves the show to another genre, in the same sheet Import uses. */
@Composable
private fun GenreMenuChip(
    show: ShowPageUi,
    genres: List<CategoryOptionUi>,
    onGenreChange: (String) -> Unit,
    create: CreateGenre,
    modifier: Modifier = Modifier,
) {
    val name = show.genre.orEmpty()
    val description = stringResource(Res.string.show_genre_label, name)
    CategoryPicker(show.title, genres, show.genreId, onGenreChange, create) { open ->
        DropdownChip(name, Icons.Rounded.Category, description, open, modifier)
    }
}

/** The patterned Resume / Play button (or Subscribe), then the description. */
@Composable
internal fun ShowActions(
    show: ShowPageUi,
    nextUp: NextUpUi?,
    playing: Boolean,
    onPlayPause: (EpisodeId) -> Unit,
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Space3),
        modifier = modifier.fillMaxWidth().padding(Spacing.Space4),
    ) {
        when {
            !show.subscribed -> Button(onClick = onSubscribe, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text(stringResource(Res.string.show_subscribe), modifier = Modifier.padding(start = Spacing.Space2))
            }
            nextUp != null -> PlayNextButton(nextUp, playing, onPlayPause)
        }
        if (show.description.isNotBlank()) Description(show.description)
    }
}


/** Three lines and "More", which opens the whole description in place. */
@Composable
private fun Description(text: String, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflows by remember { mutableStateOf(false) }
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val canExpand = !expanded && overflows
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Space1),
        modifier = if (canExpand) {
            modifier.clickable(role = Role.Button, onClickLabel = stringResource(Res.string.show_more_label)) {
                expanded = true
            }
        } else {
            modifier
        },
    ) {
        // Collapsed, paragraphs run together so the three lines are all text.
        val preview = remember(text) { text.replace(WHITESPACE, " ") }
        Text(
            text = if (expanded) text else preview,
            style = DamaskTextStyles.Body,
            color = muted,
            maxLines = if (expanded) Int.MAX_VALUE else DESCRIPTION_LINES,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
        )
        if (canExpand) {
            Text(
                text = stringResource(Res.string.show_more),
                style = DamaskTextStyles.Label,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}


