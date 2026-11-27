package uk.co.kilfeder.damask.podcasts.core.data

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase

fun androidDriverFactory(context: Context): DriverFactory = DriverFactory {
    AndroidSqliteDriver(DamaskDatabase.Schema, context.applicationContext, DATABASE_NAME)
}
