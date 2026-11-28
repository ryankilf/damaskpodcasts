package uk.co.kilfeder.damask.podcasts.ui.components

import uk.co.kilfeder.damask.podcasts.ui.resources.manage_categories
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import uk.co.kilfeder.damask.podcasts.ui.resources.Res
import uk.co.kilfeder.damask.podcasts.ui.resources.back
import uk.co.kilfeder.damask.podcasts.ui.resources.settings

/** The back arrow in every heading (it mirrors in right-to-left layouts). */
@Composable
internal fun BackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(Res.string.back))
    }
}

/** The cog that opens Settings (Home's heading on phones, the foot of the rail otherwise). */
@Composable
internal fun SettingsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Rounded.Settings, contentDescription = stringResource(Res.string.settings))
    }
}

/** The tune (sliders) icon in the Genres heading that opens Manage categories. */
@Composable
internal fun ManageCategoriesButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Rounded.Tune, contentDescription = stringResource(Res.string.manage_categories))
    }
}
