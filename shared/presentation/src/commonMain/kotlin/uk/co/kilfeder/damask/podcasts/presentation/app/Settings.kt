package uk.co.kilfeder.damask.podcasts.presentation.app

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase

/** This device's preferences (the `app_setting` table). */
data class AppSettings(
    /** Self-declared 18+ (spec › Compliance). Store age signals come with v1. */
    val ageConfirmed: Boolean = false,
    /** "Hide explicit": a plain preference, off by default. */
    val hideExplicit: Boolean = false,
    /** Settings › Appearance › "Use wallpaper colours", on by default where supported. */
    val useWallpaperColours: Boolean = true,
)

internal class Settings(private val db: DamaskDatabase) {
    val values: Flow<AppSettings> = db.appSettingQueries.selectAll().asFlow().mapToList(Dispatchers.Default)
        .map { rows ->
            val map = rows.associate { it.key to it.value_ }
            AppSettings(
                ageConfirmed = map[AGE_CONFIRMED] == TRUE,
                hideExplicit = map[HIDE_EXPLICIT] == TRUE,
                useWallpaperColours = map[WALLPAPER_COLOURS] != FALSE,
            )
        }

    fun confirmAge() = put(AGE_CONFIRMED, true)

    fun setHideExplicit(value: Boolean) = put(HIDE_EXPLICIT, value)

    fun setUseWallpaperColours(value: Boolean) = put(WALLPAPER_COLOURS, value)

    private fun put(key: String, value: Boolean) = db.appSettingQueries.put(key, if (value) TRUE else FALSE)

    private companion object {
        const val AGE_CONFIRMED = "age_confirmed"
        const val HIDE_EXPLICIT = "hide_explicit"
        const val WALLPAPER_COLOURS = "use_wallpaper_colours"
        const val TRUE = "true"
        const val FALSE = "false"
    }
}
