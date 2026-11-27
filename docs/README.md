# Damask: how the app works

These docs explain how the code works and where things live. They sit between two other sources:

- **[The spec](../spec-and-design/spec.md)** is the source of truth for *intended behaviour*. If the code and the spec disagree, the spec wins (or the spec gets changed on purpose).
- **[The design hand-off](../spec-and-design/damask-design/README.md)** is the source of truth for *look*.

The docs don't repeat either. They describe the implementation and record every place the code had to interpret the spec.

## Contents

| Doc | Covers |
|---|---|
| [Architecture](architecture.md) | Modules and layers, what may depend on what, how a future SwiftUI app fits |
| [Rotation](rotation.md) | The listening rules, the rotation (LRP), autoplay and retention, as implemented |
| [Data](data.md) | The SQLite schema, last-write-wins merging, HLC timestamps, migrations |
| [Feeds](feeds.md) | OPML import, fetching, parsing, refresh, category prefill |
| [Guardrails](guardrails.md) | Lint, architecture tests and build checks, and what each one enforces |
| [Decisions](decisions.md) | Where the code interprets or deviates from the spec, and why |

## Status

| Area | State |
|---|---|
| Core rules (rotation, listening, autoplay, retention) | Done, tested |
| Database (schema 2, data layer) | Done, tested |
| OPML import, feed fetch / parse / refresh | Done, tested; a real 78-feed Pocket Casts export imports cleanly (2 feeds are dead upstream) |
| Presentation (ViewModels), screens | Not started (template greeting screen only) |
| Playback (Android Media3, desktop FFmpeg), Android Auto | Not started |
| Downloads, background refresh scheduling | Not started |
| Search (hosted Podcast Index dataset) | Not started; design changing (see [decisions](decisions.md)), revised spec to follow |
| Sync, sign-in, billing | v1, not started |

## Keeping these docs current

Update the relevant doc in the same commit as the code change. When code interprets the spec in a new way, add an entry to [decisions.md](decisions.md).
