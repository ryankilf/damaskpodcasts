package uk.co.kilfeder.damask.podcasts.core.data

import app.cash.sqldelight.db.SqlDriver

/** A fresh in-memory database with the schema applied, on each platform's real SQLite driver. */
expect fun inMemoryDriver(): SqlDriver
