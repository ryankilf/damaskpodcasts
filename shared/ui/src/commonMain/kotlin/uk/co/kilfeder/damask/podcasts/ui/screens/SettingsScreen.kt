package uk.co.kilfeder.damask.podcasts.ui.screens

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
import uk.co.kilfeder.damask.podcasts.ui.resources.settings_wallpaper
import uk.co.kilfeder.damask.podcasts.ui.resources.settings_wallpaper_body
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
    SettingsContent(
        state = state,
        onHideExplicitChange = viewModel::onHideExplicitChanged,
        onWallpaperChange = viewModel::onUseWallpaperColoursChanged,
        onImport = onImport,
        modifier = modifier,
    )
}

/**
 * Settings in groups (design hand-off › Settings): Appearance, Library, Search. Account (sync,
 * subscription) and About (licences, version) join when those exist.
 */
@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onHideExplicitChange: (Boolean) -> Unit,
    onWallpaperChange: (Boolean) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        // Hidden where the platform has no dynamic colour (spec › Decisions › Colour and Material You).
        if (supportsWallpaperColours) {
            GroupHeading(stringResource(Res.string.settings_group_appearance))
            SwitchRow(
                icon = Icons.Rounded.Palette,
                title = stringResource(Res.string.settings_wallpaper),
                body = stringResource(Res.string.settings_wallpaper_body),
                checked = state.useWallpaperColours,
                onCheckedChange = onWallpaperChange,
            )
        }
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
