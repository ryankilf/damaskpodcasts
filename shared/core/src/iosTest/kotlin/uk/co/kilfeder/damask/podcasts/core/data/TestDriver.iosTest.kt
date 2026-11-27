package uk.co.kilfeder.damask.podcasts.core.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.inMemoryDriver
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase

actual fun inMemoryDriver(): SqlDriver = inMemoryDriver(DamaskDatabase.Schema)
