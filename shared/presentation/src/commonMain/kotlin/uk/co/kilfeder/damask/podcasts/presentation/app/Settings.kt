package uk.co.kilfeder.damask.podcasts.presentation.app

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase

/** Settings › Theme and colours › Mode. Automatic follows the system; it's the default. */
enum class ThemeMode { LIGHT, DARK, AUTOMATIC }

/** This device's preferences (the `app_setting` table). */
data class AppSettings(
    /** "Hide explicit": a plain preference, off by default. */
    val hideExplicit: Boolean = false,
    /** Settings › Theme and colours › "Your colours" (Material You). Off by default: Damask's own colours. */
    val useWallpaperColours: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.AUTOMATIC,
)

internal class Settings(private val db: DamaskDatabase) {
    val values: Flow<AppSettings> = db.appSettingQueries.selectAll().asFlow().mapToList(Dispatchers.Default)
        .map { rows ->
            val map = rows.associate { it.key to it.value_ }
            AppSettings(
                hideExplicit = map[HIDE_EXPLICIT] == TRUE,
                useWallpaperColours = map[WALLPAPER_COLOURS] == TRUE,
                themeMode = ThemeMode.entries.firstOrNull { it.name == map[THEME_MODE] } ?: ThemeMode.AUTOMATIC,
            )
        }

    fun setHideExplicit(value: Boolean) = put(HIDE_EXPLICIT, value)

    fun setUseWallpaperColours(value: Boolean) = put(WALLPAPER_COLOURS, value)

    fun setThemeMode(value: ThemeMode) = db.appSettingQueries.put(THEME_MODE, value.name)

    private fun put(key: String, value: Boolean) = db.appSettingQueries.put(key, if (value) TRUE else FALSE)

    private companion object {
        const val HIDE_EXPLICIT = "hide_explicit"
        const val WALLPAPER_COLOURS = "use_wallpaper_colours"
        const val THEME_MODE = "theme_mode"
        const val TRUE = "true"
        const val FALSE = "false"
    }
}
