package uk.co.kilfeder.damask.podcasts.core.data

import app.cash.sqldelight.driver.native.NativeSqliteDriver
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase

fun iosDriverFactory(): DriverFactory = DriverFactory {
    NativeSqliteDriver(DamaskDatabase.Schema, DATABASE_NAME)
}
