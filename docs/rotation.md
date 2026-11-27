# Rotation

The rules from the spec's *Core concepts*, *Episode lifecycle*, *Finishing and autoplay* and *Ordering rules*, as implemented in `shared/core`. Everything here is pure Kotlin with no I/O, tested in `shared/core/src/commonTest`.

## The pieces

| Code | Does |
|---|---|
| `model/` | `Show`, `Episode`, `Category`, `PlayContext`, `Hlc`. Plain data, no logic beyond defaults |
| `rules/ListeningRules` | Listen threshold, `countsAsListened`, `listenedAt`, in progress, finish-on-stop |
| `rotation/Library` | A read-time snapshot: categories, shows, episodes (aliases already resolved) |
| `rotation/ListenHistory` | `lastListened(category)` and `lastListened(show)` |
| `rotation/Rotation` | Every list: Genres, genre queue, Up Next, In Progress, Played, a show's own order |
| `playback/Autoplay` | What plays after an episode finishes |
| `playback/ResumePosition` | The 30 s rewind for mismatched ad-inserted copies |
| `retention/Retention` | Which feed-cache episodes to prune |

## Read-time ordering

Nothing about ordering is stored. A screen builds `Rotation(library, rules, nowMs)` from the current snapshot and asks for a list. Listening changes the snapshot, so the next `Rotation` orders differently. There is no queue state to update or sync.

## Listening

- **Threshold:** 30 % of the duration (the player's duration if known, else the feed's), or 10 minutes when the duration is unknown.
- **Counts as listened:** finished (`played_by = FINISHED`), or the *current* position is at or past the threshold. Scrubbing back below the line undoes it; marking unplayed resets the position to 0.
- **`listenedAt`:** the later of the finished time and the past-threshold position time.
- **`lastListened(group)`:** the latest `listenedAt` in the group. `null` means never, which sorts first.
- **In progress:** position > 0, not played (or re-listened since being played), and touched within 183 days. Serial shows never expire.
- **Finished on stop:** reached the true end, or stopped with ≤ 2 minutes left. A tail of 0 means only the true end counts; with an unknown duration, only the true end counts.

All the numbers live in `ListeningSettings`.

## Lists

All built by round-robin over groups sorted by LRP (`rotation/RoundRobin`). The unbounded lists are lazy `Sequence`s: page with `drop(offset).take(n)`. Generation stops when a full round adds nothing.

| List | Order |
|---|---|
| `genres()` | Categories by `lastListened`, never-listened first, then by name |
| `genreQueue(category)` | The category's in-progress episodes (most recently played first, re-listens of played episodes excluded), then one unplayed episode per show, shows by LRP (ties by ID), round-robin |
| `upNext()` | One candidate per category, categories by LRP, repeated; in-progress episodes excluded |
| `inProgress()` | All in-progress episodes, most recently played first |
| `played()` | Every episode with position or played state, newest activity first, unsubscribed shows included |
| `showOrder(show)` | All the show's episodes, newest-first (or oldest-first for serials / overrides) |

A show's category falls back to Uncategorised when its category is missing or tombstoned. Unsubscribed shows leave every list except Played.

## Autoplay

`Autoplay.next(rotation, context, finished, switches)`. The `Rotation` must be built *after* the finish was recorded, so Up Next has already moved the category just heard to the back.

| Context | Next |
|---|---|
| `UpNext` | First Up Next item |
| `Genre(id)` | First item of that genre queue (can be an in-progress episode) |
| `Show(id)` | The next unplayed episode after the finished one, in the show's order |
| `None` | Nothing |

The global switch and "stop after this episode" both return `null`.

## Retention

Applied after every feed fetch. Episodic shows keep the newest `keepCount` (default 10) feed-cache episodes; serial shows keep all. In-progress (unexpired) and downloaded episodes are never pruned. Only the local `episode` table is pruned; `episode_meta` keeps Played intact.
