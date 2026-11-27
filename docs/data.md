# Data

SQLite through SQLDelight 2, in `shared/core`. The schema follows the spec's *Data model*.

## Where things are

| Path | Contains |
|---|---|
| `shared/core/src/commonMain/sqldelight/.../db/*.sq` | The current schema and its queries, one file per table group |
| `shared/core/src/commonMain/sqldelight/migrations/<n>.sqm` | Migration from schema *n* to *n + 1* |
| `shared/core/src/commonMain/sqldelight/databases/<n>.db` | Snapshot of every released schema, used to verify migrations |
| `core/data/` | Kotlin on top: driver factories, `LibraryRepository`, `UserStateStore`, `FeedStore`, `CategoryPrefill`, `DeviceClock` |

The SQL dialect is SQLite 3.30: Android 12 (minSdk 31) ships SQLite 3.32, so nothing newer may be used.

Database file: `damask.db`. Android and iOS use the platform default location. Desktop uses `~/Library/Application Support/Damask` (macOS), `%APPDATA%\Damask` (Windows) or `$XDG_DATA_HOME/damask`, defaulting to `~/.local/share/damask` (Linux).

## Tables

| Synced (user state) | Local only |
|---|---|
| `category`, `apple_category_map`, `show`, `show_alias` | `show_feed`, `show_feed_category`, `episode` (the feed cache) |
| `episode_meta`, `episode_position`, `episode_played` | `feed_fetch_state` (HTTP cache headers, feed health) |
| `listen_stats` | `device`, `outbox` (sync machinery) |

Nothing syncs yet. The private build is local-only, and nothing writes to `outbox` until sync lands.

## Last write wins

Every synced row carries an `hlc`. Each synced table's `upsert` only replaces a row when the incoming `hlc` is newer:

```sql
INSERT ... ON CONFLICT (...) DO UPDATE SET ... WHERE excluded.hlc > episode_position.hlc;
```

That is the same rule the server will use, so pulled changes merge locally the same way. Position and played state are separate tables so each needs only a row-level rule. The exception is `listen_stats.addLocal`, which adds this device's time to its own row. Only the owning device writes a row.

## HLC timestamps

`Hlc` packs wall-clock milliseconds into the high bits and a 16-bit counter into the low bits, so plain numeric comparison orders them. `DeviceClock` owns the device's `HlcClock` and persists `device.last_hlc` after every issue, so timestamps keep increasing across restarts even if the wall clock goes backwards. `receive()` is for HLCs pulled from the server.

## Seeded rows have fixed IDs

Rows every device creates for itself must have the same ID everywhere, or sync would duplicate them:

- Uncategorised: `uncategorised` (seeded by the schema).
- Apple's top-level categories: `apple:<name>`, e.g. `apple:healthandfitness` (seeded at import by `CategoryPrefill`).

Seeded rows use HLC 0, so any user rename, merge or delete wins over them.

## Reading: `LibraryRepository`

`load()` builds the rotation's `Library`. It reads every feed-cache episode, plus episodes that have left their feed but still have `episode_meta` (so Played survives pruning). Every read goes through `canonical(show_id)`: alias shows are dropped, their episodes are re-keyed to the root, and duplicate episodes merge, taking the newer position and played state.

## Writing user state: `UserStateStore`

`savePosition`, `markPlayed`, `markUnplayed` (also resets the position to 0) and `reportPlayerDuration`. Each runs in a transaction, takes a fresh HLC, and first makes sure the episode has its `episode_meta` record.

## Changing the schema

1. Edit the `.sq` files.
2. Add `migrations/<current>.sqm` with the same change. Don't use `REFERENCES` there: migrations are compiled without the main schema in scope. Foreign keys aren't enforced anyway (SQLite's default).
3. Run `./gradlew :shared:core:generateCommonMainDamaskDatabaseSchema` *on its own* to write the new `<n>.db` snapshot.
4. `./gradlew check` verifies the migration against the snapshots.
