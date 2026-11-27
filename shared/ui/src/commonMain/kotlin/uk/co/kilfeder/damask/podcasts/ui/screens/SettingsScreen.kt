package uk.co.kilfeder.damask.podcasts.ui.screens

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
import androidx.compose.material3.TextButton
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
import uk.co.kilfeder.damask.podcasts.ui.resources.import_from_settings
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

@Composable
private fun SettingsContent(
    state: SettingsUiState,
    onHideExplicitChange: (Boolean) -> Unit,
    onWallpaperChange: (Boolean) -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        SwitchRow(
            title = stringResource(Res.string.settings_hide_explicit),
            body = stringResource(Res.string.settings_hide_explicit_body),
            checked = state.hideExplicit,
            onCheckedChange = onHideExplicitChange,
        )
        // Hidden where the platform has no dynamic colour (spec › Decisions › Colour and Material You).
        if (supportsWallpaperColours) {
            SwitchRow(
                title = stringResource(Res.string.settings_wallpaper),
                body = stringResource(Res.string.settings_wallpaper_body),
                checked = state.useWallpaperColours,
                onCheckedChange = onWallpaperChange,
            )
        }
        TextButton(onClick = onImport, modifier = Modifier.padding(horizontal = Spacing.Space2)) {
            Text(stringResource(Res.string.import_from_settings))
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Space4),
        modifier = modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .heightIn(min = Sizes.RowMinHeight)
            .padding(horizontal = Spacing.Space4, vertical = Spacing.Space2),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.Space1)) {
            Text(title, style = DamaskTextStyles.Title)
            Text(body, style = DamaskTextStyles.BodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        // The row handles the toggle, so the switch itself doesn't take clicks.
        Switch(checked = checked, onCheckedChange = null)
    }
}
