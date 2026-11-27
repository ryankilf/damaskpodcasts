package uk.co.kilfeder.damask.podcasts.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import java.util.Properties

actual fun inMemoryDriver(): SqlDriver =
    JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties(), DamaskDatabase.Schema)
