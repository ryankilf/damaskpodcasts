package uk.co.kilfeder.damask.podcasts.ui.screens

import uk.co.kilfeder.damask.podcasts.ui.resources.row_meta
import uk.co.kilfeder.damask.podcasts.ui.resources.settings_footer_love
import uk.co.kilfeder.damask.podcasts.ui.resources.settings_footer_by
import uk.co.kilfeder.damask.podcasts.ui.resources.settings_footer_built
import uk.co.kilfeder.damask.podcasts.ui.resources.theme_damask
import uk.co.kilfeder.damask.podcasts.ui.resources.theme_your_colours
import uk.co.kilfeder.damask.podcasts.ui.resources.theme_and_colours
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.foundation.layout.BoxWithConstraints
import uk.co.kilfeder.damask.podcasts.ui.resources.settings_import_body
import uk.co.kilfeder.damask.podcasts.ui.resources.settings_group_search
import uk.co.kilfeder.damask.podcasts.ui.resources.settings_group_library
import uk.co.kilfeder.damask.podcasts.ui.resources.settings_group_appearance
import uk.co.kilfeder.damask.podcasts.ui.resources.import_subscriptions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Explicit
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph
import uk.co.kilfeder.damask.podcasts.presentation.settings.SettingsUiState
import uk.co.kilfeder.damask.podcasts.presentation.settings.SettingsViewModel
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.settings_hide_explicit
import uk.co.kilfeder.damask.podcasts.ui.resources.settings_hide_explicit_body
import uk.co.kilfeder.damask.podcasts.ui.theme.DamaskTextStyles
import uk.co.kilfeder.damask.podcasts.ui.theme.Sizes
import uk.co.kilfeder.damask.podcasts.ui.theme.Spacing
import uk.co.kilfeder.damask.podcasts.ui.theme.supportsWallpaperColours

@Composable
internal fun SettingsScreen(
    graph: AppGraph,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel { graph.settingsViewModel() },
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var themeOpen by remember { mutableStateOf(false) }
    SettingsContent(
        state = state,
        onHideExplicitChange = viewModel::onHideExplicitChanged,
        onImport = onImport,
        onOpenTheme = { themeOpen = true },
        modifier = modifier,
    )
    if (themeOpen) {
        ThemeSheet(
            mode = state.themeMode,
            yourColours = state.useWallpaperColours,
            onModeChange = viewModel::onThemeModeChanged,
            onYourColoursChange = viewModel::onUseWallpaperColoursChanged,
            onDismiss = { themeOpen = false },
        )
    }
}

/**
 * Settings in groups (design hand-off › Settings): Appearance (Theme and colours, a sheet),
 * Library, Search; then the footer, pinned to the bottom while Settings fits on one screen.
 * Account (sync, subscription) and About (licences, version) join when those exist.
 */
@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onHideExplicitChange: (Boolean) -> Unit,
    onImport: () -> Unit,
    onOpenTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        Column(
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.verticalScroll(rememberScrollState()).heightIn(min = maxHeight),
        ) {
            Column {
                GroupHeading(stringResource(Res.string.settings_group_appearance))
                LinkRow(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(Res.string.theme_and_colours),
                    body = themeSummary(state),
                    onClick = onOpenTheme,
                )
                GroupHeading(stringResource(Res.string.settings_group_library))
                LinkRow(
                    icon = Icons.Rounded.FileUpload,
                    title = stringResource(Res.string.import_subscriptions),
                    body = stringResource(Res.string.settings_import_body),
                    onClick = onImport,
                )
                GroupHeading(stringResource(Res.string.settings_group_search))
                SwitchRow(
                    icon = Icons.Rounded.Explicit,
                    title = stringResource(Res.string.settings_hide_explicit),
                    body = stringResource(Res.string.settings_hide_explicit_body),
                    checked = state.hideExplicit,
                    onCheckedChange = onHideExplicitChange,
                )
            }
            Footer()
        }
    }
}

/** "Automatic · Your colours" (Your colours only where the device has them). */
@Composable
private fun themeSummary(state: SettingsUiState): String {
    val mode = stringResource(state.themeMode.label)
    if (!supportsWallpaperColours) return mode
    val colours = stringResource(
        if (state.useWallpaperColours) Res.string.theme_your_colours else Res.string.theme_damask,
    )
    return stringResource(Res.string.row_meta, mode, colours)
}

/** "Built with ♥ by Damask Engineering", small, at the foot of Settings. */
@Composable
private fun Footer(modifier: Modifier = Modifier) {
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space1, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth().padding(Spacing.Space6),
    ) {
        Text(stringResource(Res.string.settings_footer_built), style = DamaskTextStyles.BodySmall, color = muted)
        Icon(
            Icons.Rounded.Favorite,
            contentDescription = stringResource(Res.string.settings_footer_love),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(Sizes.FooterHeart),
        )
        Text(stringResource(Res.string.settings_footer_by), style = DamaskTextStyles.BodySmall, color = muted)
    }
}

/** A group's label, in primary (design hand-off › Settings). */
@Composable
private fun GroupHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = DamaskTextStyles.Label,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = Spacing.Space4, end = Spacing.Space4, top = Spacing.Space4, bottom = Spacing.Space1)
            .semantics { heading() },
    )
}

/** A row that opens something: icon, title, a line under it, and a chevron. */
@Composable
private fun LinkRow(
    icon: ImageVector,
    title: String,
    body: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingRow(icon, title, body, modifier.clickable(role = Role.Button, onClick = onClick)) {
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val toggle = modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
    SettingRow(icon, title, body, toggle) {
        // The row handles the toggle, so the switch itself doesn't take clicks.
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    end: @Composable () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space4),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Sizes.RowMinHeight)
            .padding(horizontal = Spacing.Space4, vertical = Spacing.Space2),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Space1 / 2)) {
            Text(title, style = DamaskTextStyles.Body, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = DamaskTextStyles.BodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        end()
    }
}
