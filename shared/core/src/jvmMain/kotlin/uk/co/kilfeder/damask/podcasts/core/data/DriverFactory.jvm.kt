package uk.co.kilfeder.damask.podcasts.core.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import java.io.File
import java.util.Properties

/** Opens (creating and migrating as needed) the desktop database at [file]. */
fun desktopDriverFactory(file: File = desktopDataDirectory().resolve(DATABASE_NAME)): DriverFactory =
    DriverFactory {
        file.parentFile?.mkdirs()
        JdbcSqliteDriver("jdbc:sqlite:${file.absolutePath}", Properties(), DamaskDatabase.Schema)
    }

/** The per-user app data directory for this OS. */
fun desktopDataDirectory(): File {
    val os = System.getProperty("os.name").orEmpty().lowercase()
    val home = File(System.getProperty("user.home"))
    return when {
        "mac" in os -> home.resolve("Library/Application Support/Damask")
        "win" in os -> File(System.getenv("APPDATA") ?: home.resolve("AppData/Roaming").path).resolve("Damask")
        else -> File(System.getenv("XDG_DATA_HOME") ?: home.resolve(".local/share").path).resolve("damask")
    }
}
