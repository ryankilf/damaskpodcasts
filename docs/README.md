# Damask: how the app works

These docs explain how the code works and where things live. They sit between two other sources:

- **[The spec](spec.md)** is the source of truth for *intended behaviour*. If the code and the spec disagree, the spec wins (or the spec gets changed on purpose).
- **[The design hand-off](../spec-and-design/damask-design/README.md)** is the source of truth for *look*. Its `spec/v1-spec.md` is an older snapshot; [spec.md](spec.md) supersedes it.

The docs don't repeat either. They describe the implementation and record every place the code had to interpret the spec.

## Contents

| Doc | Covers |
|---|---|
| [Spec](spec.md) | What the app should do (product, behaviour, compliance, costs) |
| [Architecture](architecture.md) | Modules and layers, what may depend on what, how a future SwiftUI app fits |
| [Rotation](rotation.md) | The listening rules, the rotation (LRP), autoplay and retention, as implemented |
| [Data](data.md) | The SQLite schema, last-write-wins merging, HLC timestamps, migrations |
| [Feeds](feeds.md) | OPML import, fetching, parsing, refresh, category prefill |
| [Playback](playback.md) | The playback controller and its rules, the platform engines, Android's media service and cache |
| [Guardrails](guardrails.md) | Lint, architecture tests and build checks, and what each one enforces |
| [Branding](branding.md) | The app icon and how it's generated for every platform |
| [Decisions](decisions.md) | Where the code interprets or deviates from the spec, and why |

## Status

| Area | State |
|---|---|
| Core rules (rotation, listening, autoplay, retention) | Done, tested |
| Database (schema 4, data layer) | Done, tested |
| Playback | Shared controller done and tested; Android (Media3, background service, mini-player, controls) working on the emulator; desktop (FFmpeg spike) next; iOS not started; downloads, Android Auto and "stop after this episode" to come |
| OPML import, feed fetch / parse / refresh | Done, tested; a real 78-feed Pocket Casts export imports cleanly (2 feeds are dead upstream) |
| Presentation and screens | Import, Home, Genres, genre queue, Played, Settings done and tested; Search is a placeholder until the gateway merges; Now Playing waits on playback; tablet/desktop two-pane layouts not started |
| Playback (Android Media3, desktop FFmpeg), Android Auto | Not started |
| Downloads, background refresh scheduling | Not started |
| Search backend (`shared/api`, `server/search`, `server/importer`, `deploy/`) | Specified ([spec › Search](spec.md#search)); not started |
| `podcast_guid` aliasing | Specified; not started |
| 18+ age gate, "Hide explicit" | Specified ([spec › Compliance](spec.md#compliance)); not started |
| Sync, sign-in, billing | v1, not started |

## Keeping these docs current

Update the relevant doc in the same commit as the code change. When code interprets the spec in a new way, add an entry to [decisions.md](decisions.md).
