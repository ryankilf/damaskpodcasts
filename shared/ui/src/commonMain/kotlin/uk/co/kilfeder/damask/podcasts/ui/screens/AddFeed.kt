package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.discover.AddFeedProblem
import uk.co.kilfeder.damask.podcasts.presentation.discover.AddFeedUiState
import uk.co.kilfeder.damask.podcasts.ui.components.SelectableListRow
import uk.co.kilfeder.damask.podcasts.ui.components.TextEntryDialog
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_checking
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_confirm
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_hint
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_label
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_needs_login
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_not_link
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_unreadable
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes

/**
 * "Add a feed by URL" (design: Discover's idle row): paste a feed's link, Add; the app reads it
 * and opens the show. What went wrong shows under the field; typing again clears it.
 */
@Composable
internal fun AddFeedDialog(
    state: AddFeedUiState,
    onAdd: (String) -> Unit,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val problem = state.problem
    TextEntryDialog(
        title = stringResource(Res.string.add_feed),
        label = stringResource(Res.string.add_feed_label),
        confirm = stringResource(Res.string.add_feed_confirm),
        onConfirm = onAdd,
        onDismiss = onDismiss,
        modifier = modifier,
        message = when {
            state.checking -> stringResource(Res.string.add_feed_checking)
            problem != null -> problemText(problem)
            else -> stringResource(Res.string.add_feed_hint)
        },
        isError = problem != null,
        busy = state.checking,
        onValueChange = { onEdit() },
        keyboardType = KeyboardType.Uri,
    )
}

@Composable
private fun problemText(problem: AddFeedProblem): String = when (problem) {
    AddFeedProblem.NotALink -> stringResource(Res.string.add_feed_not_link)
    AddFeedProblem.NeedsLogin -> stringResource(Res.string.add_feed_needs_login)
    is AddFeedProblem.Unreadable -> stringResource(Res.string.add_feed_unreadable, problem.reason)
}

/** The "Add a feed by URL" row at the top of Discover before you search (design: Discover idle). */
@Composable
internal fun AddFeedRow(onClick: () -> Unit, modifier: Modifier = Modifier) {
    SelectableListRow(selected = false, onClick = onClick, minHeight = Sizes.MinTouchTarget, modifier = modifier) {
        Icon(Icons.Rounded.Link, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(Res.string.add_feed), style = DamaskTextStyles.Body, modifier = Modifier.weight(1f))
    }
}

/** Discover before you search: the add-a-feed row, then what Discover is for. */
@Composable
internal fun DiscoverIdle(onAddFeed: () -> Unit, modifier: Modifier = Modifier, emptyState: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.Top, modifier = modifier) {
        AddFeedRow(onAddFeed)
        emptyState()
    }
}

/** What the add-a-feed flow can ask for. */
internal class AddFeedIntents(
    val onAdd: (String) -> Unit,
    val onEdit: () -> Unit,
    /** Forget the last attempt (closed, or done). */
    val onReset: () -> Unit,
    val onClose: () -> Unit,
    val onOpenShow: (showId: String) -> Unit,
    val onPreviewShow: (showId: String) -> Unit,
)

/**
 * The dialog while [open], and opening the show once its feed is read: one you have in Shows,
 * any other as a preview in Discover, to listen before subscribing.
 */
@Composable
internal fun AddFeedFlow(open: Boolean, state: AddFeedUiState, intents: AddFeedIntents) {
    LaunchedEffect(state.added) {
        state.added?.let {
            if (it.subscribed) intents.onOpenShow(it.showId) else intents.onPreviewShow(it.showId)
            intents.onReset()
            intents.onClose()
        }
    }
    if (open) {
        AddFeedDialog(
            state = state,
            onAdd = intents.onAdd,
            onEdit = intents.onEdit,
            onDismiss = {
                intents.onClose()
                intents.onReset()
            },
        )
    }
}
