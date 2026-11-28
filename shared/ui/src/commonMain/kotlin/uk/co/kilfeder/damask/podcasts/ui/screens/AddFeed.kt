package uk.co.kilfeder.damask.podcasts.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.discover.AddFeedProblem
import uk.co.kilfeder.damask.podcasts.presentation.discover.AddFeedUiState
import uk.co.kilfeder.damask.podcasts.presentation.discover.AddFeedViewModel
import uk.co.kilfeder.damask.podcasts.ui.components.SelectableListRow
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_checking
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_confirm
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_hint
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_intro
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_label
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_needs_login
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_not_link
import uk.co.kilfeder.damask.podcasts.ui.resources.add_feed_unreadable
import uk.co.kilfeder.damask.podcasts.ui.resources.search_clear
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing

/**
 * "Add a feed by URL", its own screen in Discover (the shell gives it the title and back): the
 * field is focused with the keyboard up, Go or Add reads the feed, and the show opens: one you
 * have in Shows ([onOpenShow]), any other as a preview to listen first ([onPreviewShow]), which
 * replaces this screen so Back goes to Discover. A feed that needs a login will get username and
 * password fields here, below the address, when private feeds come.
 */
@Composable
internal fun AddFeedScreen(
    graph: AppGraph,
    onOpenShow: (String) -> Unit,
    onPreviewShow: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddFeedViewModel = viewModel { graph.addFeedViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val openShow by rememberUpdatedState(onOpenShow)
    val preview by rememberUpdatedState(onPreviewShow)
    LaunchedEffect(state.added) {
        state.added?.let {
            viewModel.onOpened()
            if (it.subscribed) openShow(it.showId) else preview(it.showId)
        }
    }
    AddFeedContent(state, onAdd = viewModel::onAdd, onEdit = viewModel::onEdit, modifier = modifier)
}

@Composable
private fun AddFeedContent(
    state: AddFeedUiState,
    onAdd: (String) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var text by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    val problem = state.problem
    val submit = { if (text.isNotBlank() && !state.checking) onAdd(text) }
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.Space4),
        modifier = modifier.verticalScroll(rememberScrollState()).padding(Spacing.Space4),
    ) {
        Text(
            text = stringResource(Res.string.add_feed_intro),
            style = DamaskTextStyles.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                onEdit()
            },
            label = { Text(stringResource(Res.string.add_feed_label)) },
            singleLine = true,
            enabled = !state.checking,
            isError = problem != null,
            supportingText = { Text(problem?.let { problemText(it) } ?: stringResource(Res.string.add_feed_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Link, contentDescription = null) },
            trailingIcon = if (text.isEmpty() || state.checking) {
                null
            } else {
                {
                    IconButton(onClick = { text = "" }) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.search_clear))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { submit() }),
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
        )
        AddButton(checking = state.checking, enabled = text.isNotBlank(), onClick = submit)
    }
}

/** Add, or a spinner and "Checking the feed…" while it's read. */
@Composable
private fun AddButton(checking: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        enabled = enabled && !checking,
        modifier = modifier.fillMaxWidth().heightIn(min = Sizes.MinTouchTarget),
    ) {
        if (checking) {
            CircularProgressIndicator(
                strokeWidth = Sizes.ProgressRingStroke,
                modifier = Modifier.size(Sizes.IconRipple).padding(end = Spacing.Space2),
            )
            Text(stringResource(Res.string.add_feed_checking), modifier = Modifier.padding(start = Spacing.Space2))
        } else {
            Text(stringResource(Res.string.add_feed_confirm))
        }
    }
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
