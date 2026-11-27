package uk.co.kilfeder.damask.podcasts.core.data

import app.cash.sqldelight.db.SqlDriver
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase

/**
 * Opens the platform's SQLite driver, creating or migrating the schema. Implementations:
 * `androidDriverFactory(context)`, `desktopDriverFactory()`, `iosDriverFactory()`.
 */
fun interface DriverFactory {
    fun create(): SqlDriver
}

const val DATABASE_NAME = "damask.db"

fun DriverFactory.createDatabase(): DamaskDatabase = DamaskDatabase(create())
