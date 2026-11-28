package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.ui.resources.row_pause
import uk.co.kilfeder.damask.podcasts.ui.resources.pause
import uk.co.kilfeder.damask.podcasts.ui.components.PlayPauseIcon
import uk.co.kilfeder.damask.podcasts.ui.components.DropdownChip
import uk.co.kilfeder.damask.podcasts.ui.components.damaskButton
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.episodes.EpisodeId
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised
import uk.co.kilfeder.damask.podcasts.presentation.shows.Cadence
import uk.co.kilfeder.damask.podcasts.presentation.genres.CategoryOptionUi
import uk.co.kilfeder.damask.podcasts.presentation.shows.NextUpUi
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowOrder
import uk.co.kilfeder.damask.podcasts.presentation.shows.ShowPageUi
import uk.co.kilfeder.damask.podcasts.ui.components.Artwork
import uk.co.kilfeder.damask.podcasts.ui.components.ExactValueText
import uk.co.kilfeder.damask.podcasts.ui.components.damaskFill
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.genres_last
import uk.co.kilfeder.damask.podcasts.ui.resources.show_button_time
import uk.co.kilfeder.damask.podcasts.ui.resources.show_cadence_daily
import uk.co.kilfeder.damask.podcasts.ui.resources.show_cadence_fortnightly
import uk.co.kilfeder.damask.podcasts.ui.resources.show_cadence_monthly
import uk.co.kilfeder.damask.podcasts.ui.resources.show_cadence_weekly
import uk.co.kilfeder.damask.podcasts.ui.resources.show_episodes
import uk.co.kilfeder.damask.podcasts.ui.resources.show_episodes_heading
import uk.co.kilfeder.damask.podcasts.ui.resources.show_episodes_since
import uk.co.kilfeder.damask.podcasts.ui.resources.show_genre_label
import uk.co.kilfeder.damask.podcasts.ui.resources.show_last_exact
import uk.co.kilfeder.damask.podcasts.ui.resources.show_meta_sep
import uk.co.kilfeder.damask.podcasts.ui.resources.show_more
import uk.co.kilfeder.damask.podcasts.ui.resources.show_more_label
import uk.co.kilfeder.damask.podcasts.ui.resources.show_order_label
import uk.co.kilfeder.damask.podcasts.ui.resources.show_order_newest
import uk.co.kilfeder.damask.podcasts.ui.resources.show_order_oldest
import uk.co.kilfeder.damask.podcasts.ui.resources.play
import uk.co.kilfeder.damask.podcasts.ui.resources.show_play_label
import uk.co.kilfeder.damask.podcasts.ui.resources.show_resume
import uk.co.kilfeder.damask.podcasts.ui.resources.row_resume
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

/** The time beside "Resume" or "Play" is a little quieter than the word. */
private const val BUTTON_TIME_ALPHA = 0.8f

/** Artwork, name, maker, "Weekly · 86 episodes · Last played today", and the genre chip. */
@Composable
internal fun ShowDetails(
    show: ShowPageUi,
    genres: List<CategoryOptionUi>,
    onGenreChange: (String) -> Unit,
    modifier: Modifier = Modifier,
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
                modifier = Modifier.semantics { heading() },
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
            ExactValueText(value = showFacts(show), style = DamaskTextStyles.BodySmall, color = muted)
            if (show.subscribed) GenreMenuChip(show, genres, onGenreChange)
        }
    }
}

/** The facts line, with the exact values for long press (publish day, count since, last played). */
@Composable
private fun showFacts(show: ShowPageUi): Summarised {
    val cadence = when (show.cadence) {
        Cadence.DAILY -> stringResource(Res.string.show_cadence_daily)
        Cadence.WEEKLY -> stringResource(Res.string.show_cadence_weekly)
        Cadence.FORTNIGHTLY -> stringResource(Res.string.show_cadence_fortnightly)
        Cadence.MONTHLY -> stringResource(Res.string.show_cadence_monthly)
        null -> null
    }
    val count = pluralStringResource(Res.plurals.show_episodes, show.episodeCount, show.episodeCount)
    val last = show.lastPlayed.takeIf { show.subscribed }
    val summary = listOfNotNull(cadence, count, last?.let { stringResource(Res.string.genres_last, it.summary) })
    val exact = listOfNotNull(
        show.usualDay?.let { stringResource(Res.string.show_usual_day, it) } ?: cadence,
        show.since?.let {
            pluralStringResource(Res.plurals.show_episodes_since, show.episodeCount, show.episodeCount, it)
        } ?: count,
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
    modifier: Modifier = Modifier,
) {
    val name = show.genre.orEmpty()
    val description = stringResource(Res.string.show_genre_label, name)
    CategoryPicker(show.title, genres, show.genreId, onGenreChange) { open ->
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

/**
 * "Resume · 32 min left" or "Play · 47 min", and "Pause" while it plays. It plays in place; it
 * doesn't open the episode. Never the episode's name (feed titles aren't reliable enough for a
 * button); that's in the label read out, and on long press.
 */
@Composable
private fun PlayNextButton(
    next: NextUpUi,
    playing: Boolean,
    onPlayPause: (EpisodeId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val time = next.length
    val exact = time?.exact.orEmpty()
    val label = when {
        playing -> stringResource(Res.string.row_pause, next.title, exact)
        next.resume -> stringResource(Res.string.row_resume, next.title, exact)
        else -> stringResource(Res.string.show_play_label, next.title, exact)
    }
    val word = when {
        playing -> Res.string.pause
        next.resume -> Res.string.show_resume
        else -> Res.string.play
    }
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space2, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.MinTouchTarget)
            .damaskButton(colors.primary, colors.onPrimary, Sizes.DamaskPitchMedium)
            .clickable(role = Role.Button, onClickLabel = label) { onPlayPause(next.id) },
    ) {
        PlayPauseIcon(playing, tint = colors.onPrimary, description = "")
        Text(
            text = stringResource(word),
            style = DamaskTextStyles.Label,
            color = colors.onPrimary,
        )
        time?.let {
            ExactValueText(
                value = Summarised(it.summary, "${next.title} · ${it.exact}"),
                display = stringResource(Res.string.show_button_time, it.summary),
                style = DamaskTextStyles.Time,
                color = colors.onPrimary,
                modifier = Modifier.alpha(BUTTON_TIME_ALPHA),
            )
        }
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

/** "Episodes" with the order chip, which is also the order the show plays in. */
@Composable
internal fun EpisodesHeading(order: ShowOrder, onOrderChange: (ShowOrder) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val name = when (order) {
        ShowOrder.NEWEST_FIRST -> stringResource(Res.string.show_order_newest)
        ShowOrder.OLDEST_FIRST -> stringResource(Res.string.show_order_oldest)
    }
    val label = stringResource(Res.string.show_order_label, name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(start = Spacing.Space4, end = Spacing.Space4),
    ) {
        Text(
            text = stringResource(Res.string.show_episodes_heading),
            style = DamaskTextStyles.Title,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).semantics { heading() },
        )
        Box {
            DropdownChip(name, Icons.Rounded.SwapVert, label, onClick = { open = true })
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                val pick = { value: ShowOrder ->
                    open = false
                    onOrderChange(value)
                }
                OrderItem(ShowOrder.NEWEST_FIRST, Res.string.show_order_newest, order, pick)
                OrderItem(ShowOrder.OLDEST_FIRST, Res.string.show_order_oldest, order, pick)
            }
        }
    }
}

@Composable
private fun OrderItem(
    value: ShowOrder,
    text: org.jetbrains.compose.resources.StringResource,
    current: ShowOrder,
    onPick: (ShowOrder) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(stringResource(text)) },
        trailingIcon = if (value == current) {
            { Icon(Icons.Rounded.Check, contentDescription = null) }
        } else {
            null
        },
        onClick = { onPick(value) },
    )
}
