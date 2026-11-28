# Podcast App — v1 Spec

Nov 26, 2026 (search and compliance revised Nov 27) · @Ryan Kilfedder

## Purpose

A paid, closed-source podcast player that brings neglected genres back into rotation automatically, so a single genre doesn't swallow your listening.

- **Replaces Pocket Casts.** Its genre grouping (Smart Folders) is paywalled.
- **Fills a gap.** AntennaPod and Podcast Addict both import OPML, but they need manual tagging, and AntennaPod has only one queue. Neither app rotates genres.
- **Cheap to run.** The backend (Supabase) stays on its free tier until revenue covers Pro, and search runs on one small VPS of our own. Feeds are fetched on the device, and only user state syncs.

## Scope

Nothing is released publicly until sync exists. The private build is local-only and has no account, and it's for dogfooding. v1, the first public release, adds Supabase sync and sign-in. iOS comes in v2.

| Release | In scope |
| --- | --- |
| Private build (Android, desktop) | OPML import from Pocket Casts; genre assignment; Up Next, Genres, genre queues; In Progress, Now Playing, Played; stream and download; resume position; search through our search gateway (private token); local storage with SQLDelight; Android Auto (Media3 browse tree) |
| v1: first public release | Everything in the private build, plus Supabase sync and Google sign-in; search for every install, anonymous sessions included; daily background sync; 18+ store rating with store age signals; store billing; UK launch first |
| v2 | Android Automotive OS (standalone, using sync); iOS player with Sign in with Apple and CarPlay (requires Apple's CarPlay audio entitlement) |
| Later | Chapters; skip-silence |

## Core concepts

One rule orders everything: **unfinished first, then least recently played (LRP).**

- **Category.** Each show belongs to exactly one category. Categories are yours to create, rename, merge or split; Apple's top-level list is only the starting set. Multi-category membership would let heavily tagged shows win the rotation more often.
- **Assignment.** On import, the user must confirm or change each show's category. The suggestion is prefilled by mapping the feed's first `itunes:category` and subcategory through `apple_category_map`.
- **Played state belongs to the episode.** It is the same however you reached the episode, whether from Up Next, a genre queue or Played.
- **LRP primitive.** For any group of episodes, `lastListened(group)` = the latest time any episode in that group passed the listen threshold. A group never listened to sorts first. The same primitive runs over categories (Up Next and the Genres list) and over shows (inside a genre queue).

**Categories and language.** Feeds write `itunes:category` as a fixed English key from Apple's list, whatever language the show is in. The key is used only to look up a prefill in `apple_category_map` and is never displayed. Legacy or misspelled values are normalised before that lookup. The show's spoken language comes from the separate `<language>` tag and plays no part in rotation.

**Subcategories.** Categories are flat, with no hierarchy. The feed's full category and subcategory list is stored per show in `show_feed_category`, but only the import prefill reads it. To split a niche out (for example News / Politics), create a top-level category for it and add a mapping row. A later version could turn the stored values into tags or a hierarchy without having to re-fetch any feeds.

**Unplayed is not a backlog.** New episodes arrive faster than anyone could listen, so the app never treats unplayed episodes as a to-do list. Shows default to newest-first, and a show switched to oldest-first starts from its oldest episode, as the user chose. There are no unplayed counts or badges, and new subscriptions have no "start from" step.

**Retention and show type.** A show's defaults come from its feed's `itunes:type`, and any show can override them. Episodic shows (the default) play newest-first and keep the latest 10 episodes. Serial shows, meant to be heard in order like chapters, play oldest-first and keep every episode. Retention is applied when the feed is fetched and doesn't affect what's still on the server. **Pinned episodes are never pruned:** that covers anything currently in progress (expired in-progress episodes lose this protection), anything downloaded, and the minimal record the Played screen needs, whatever the keep setting says.

**Deleting and merging categories.** Merging reassigns the shows and repoints the mapping rows in one step. A category can be deleted only when it's empty. A built-in **Uncategorised** category (`is_system`) can't be renamed or deleted. It catches any show whose category is missing or tombstoned, which can happen if another device deleted it concurrently, and it takes part in the rotation like any other category.

## Episode lifecycle

An episode has three states. The 30% mark is an event that updates the rotation; it does not change the episode's state.

&#91;embedded content: episode lifecycle · 3 states, 1 rotation event\]

An unplayed episode can also be marked played directly. Marking an episode unplayed resets its position to 0, so it starts again from the beginning and stops counting towards the rotation.

## Finishing and autoplay

**Finished.** Audio always plays to the true end of the stream; nothing gets cut short. An episode is finished, and therefore played, when playback reaches the end of the stream, **or** when playback stops with 2 minutes or less remaining. Stopping includes pausing, stopping, switching episodes and closing the app.

- The tail is a setting, default 2 minutes; 0 means "only the true end counts".
- It's evaluated when playback stops, never continuously, so an episode isn't marked finished while you're still listening.
- With an unknown duration, only the end of the stream counts.

**Autoplay.** Each episode plays in the context it was started from, and the context decides what follows. The context is stored with the in-progress episode, so resuming from In Progress continues where you started.

| Started from | When it finishes, autoplay plays |
| --- | --- |
| Up Next | The next Up Next item, recomputed at that point so the category just heard has moved to the back |
| A genre queue | The next item in that genre queue |
| A show's page | The show's next episode in its own order (oldest-first for serial shows, newest-first otherwise) |
| Search, Played, a shared link | Nothing; playback stops |

- A global autoplay switch: off means playback always stops.
- "Stop after this episode" on Now Playing, which also works as a sleep option.
- Serial shows don't override the context: a serial heard via Up Next continues with Up Next. Serials are normally listened to from their show page anyway.

## Screens

The home screen leads with what you've started, then the cross-genre rotation.

| Screen | Contents | Order |
| --- | --- | --- |
| Home: In Progress | Episodes started but not played and touched within the last 6 months (serial shows never expire) | Most recently played first |
| Home: Up Next | One episode per category, repeated to fill; excludes in-progress episodes | Categories by LRP |
| Genres | List of categories, each opening its queue | LRP first |
| Genre queue | Like a show's page: the next shows' covers stacked, the genre's name, "Last played … · N shows", and a patterned Resume / Play button that plays through the genre from the start of its queue (it plays in place). Then the genre's in-progress episodes, then Up next with an order chip: Variety (one episode per show, interleaved, shows least recently played first), Newest first or Oldest first (every unplayed episode by date). The order is per genre and synced; it is also the order Home's Up Next takes that genre's episodes in, and what autoplay plays next within it | In-progress first, then the genre's order |
| Shows | Every subscribed show, with its genre and when it was last played; "Search your shows" filters by name as you type. Each opens its page | A–Z, ignoring a leading "The" |
| Show | Artwork, name, author, how often it publishes ("Weekly · Last played today", exact values on long press; no episode count, as we can't know it) and a genre chip that moves it to another genre. A patterned button resumes the episode in progress ("Resume · 32 min left") or plays the next unplayed one ("Play · 47 min"); it never names the episode, and that episode is highlighted in the list. The description shows three lines, and More opens the rest in place. Episodes carry an order chip (Newest first / Oldest first), which is also the order the show plays in; oldest first, the list opens at the first unplayed episode. Played episodes are dimmed, read "Played · 52 min" and have a replay button. The ⋮ menu has Copy feed URL and Unsubscribe | The show's order |
| Now Playing | The current episode, held until it is played. Starting another episode leaves this one in In Progress at its saved position. Tapping an episode row opens it, as an ordinary screen with the top bar and tabs, without playing it; play buttons (on rows, the show page, the player) play in place and never open anything. Play on an episode's screen starts it from wherever it was opened (Up Next, its genre, its show); while something is loaded, a mini-player sits above the tabs. Show notes open from the peek at the bottom as their own screen, with every link also listed under Links | — |
| Played | Log of every play session, finished episodes included; supports re-listening and undoing an accidental mark-played | Newest first |
| Import | OPML file, then a confirm-or-change category list, one row per show | Alphabetical |
| Discover | Search: results from our index of the Podcast Index catalogue, with their artwork; "Hide explicit" applies. Subscribing sends the show through the same category confirmation step | Index relevance, then popularity |

Design work lives in the [screen flow board](https://claude.ai/artifact/AEBbsBe7TLKUWNa2QKms3Q) and the [Podcast App design system](https://claude.ai/artifact/EZHMHvdS5nqQch8gN5a4SK).

### Motion

Moving between screens fades through: the old screen fades out (90ms), then the new one fades in (210ms), so the two never overlap. On one-pane layouts every screen change fades; on two-pane layouts changing tab fades the whole layout, and the detail pane fades on its own when the selection changes (a genre, a show, the playing episode, its notes). On one-pane layouts the heading is part of the screen and fades with it, so the old screen never jumps when the new one has a different heading (or none). The mini-player and the tabs stay still. Every play button swaps between play and pause with a quick fade-through: the old icon fades out (60ms), then the new one fades and grows in (120ms), centred, never overlapping (`PlayPauseCrossfade`). Android's animation scale (including "Remove animations") applies, as Compose follows it. Timings are in `theme/Motion.kt`; the helper is `components/FadeThrough.kt`.

## Ordering rules

Every list is built by round-robin over groups sorted by LRP. Ordering is computed at read time, so listening reorders everything with no state to update.

**Up Next**

1. Sort categories by `lastListened` ascending; categories never listened to come first.
2. From each category, take its best candidate: run the genre-queue rule below and take the first item that isn't in progress.
3. Emit one candidate per category in that order. Repeat for further rounds to fill the list.

Up Next is generated lazily, one page at a time. Generation stops when the page is full, or when a full round across every category adds nothing.

**Genre queue**

1. The category's in-progress episodes, most recently played first.
2. Then sort the category's shows by `lastListened` ascending.
3. Each show offers one episode: its newest or oldest unplayed episode, depending on that show's setting (default: newest for episodic shows, oldest for serial).
4. Round-robin across shows, repeated to fill the queue.

A genre queue is also generated a page at a time. Generation stops when the page is full, or when a full round across the genre's shows adds nothing.

**Genres list:** categories by `lastListened` ascending. Each row shows a stack of up to three covers: the shows whose episodes come next in that genre's queue, front first, then its other shows by LRP if the queue runs dry. The pictures change only as you listen, never at random.

**Worked example.** History was last listened to 9 days ago, Politics 2 days ago, and Tech 4 hours ago. Up Next leads with a History episode, then Politics, then Tech. If you pass 30% on the History episode, History becomes the freshest category. Up Next then reorders to Politics, Tech, History.

## Data model

The full feed cache stays on each device and is re-fetched there. What syncs is user state, plus a minimal record of every episode that has position or played state, so the Played screen survives on other devices and after a reinstall, even once the episode has left its feed.

- **Show IDs** are UUIDs: the UUIDv5 of the feed URL at first subscription, fixed for life. The URL itself can change.
- **Episodes** are keyed by `(show_id, guid)`. There's no derived episode UUID, so an aliased show resolves without remapping anything.
- **`hlc` columns** hold hybrid logical clock timestamps (see Sync).
- **Server copies** of the synced tables add `user_id` and `server_seq`.

The schema below is the local SQLDelight one.

```sql
-- ===== Synced: user state =====

CREATE TABLE category (
  id          TEXT PRIMARY KEY,          -- UUID
  name        TEXT NOT NULL,             -- user-editable; seeded from Apple's top level
  is_system   INTEGER NOT NULL DEFAULT 0, -- 1 = Uncategorised: can't be renamed or deleted
  deleted     INTEGER NOT NULL DEFAULT 0, -- tombstone
  hlc         INTEGER NOT NULL           -- last write wins, per record
);

-- Drives the import prefill only. Seeded from Apple's list; user-editable.
-- A row with a subcategory beats the '' (any) row for the same category.
CREATE TABLE apple_category_map (
  apple_category     TEXT NOT NULL,             -- e.g. 'News'
  apple_subcategory  TEXT NOT NULL DEFAULT '',  -- e.g. 'Politics'; '' = any
  category_id        TEXT NOT NULL REFERENCES category(id),
  deleted            INTEGER NOT NULL DEFAULT 0,  -- tombstone
  hlc                INTEGER NOT NULL,
  PRIMARY KEY (apple_category, apple_subcategory)
);

CREATE TABLE show (
  id             TEXT PRIMARY KEY,       -- UUIDv5(first feed_url); fixed for life
  feed_url       TEXT NOT NULL,          -- current URL; mutable; a secret for private feeds
  feed_url_norm  TEXT NOT NULL,          -- normalised for duplicate detection (see Sync)
  podcast_guid   TEXT,                   -- podcast:guid from the feed; same GUID = same show
  category_id    TEXT NOT NULL REFERENCES category(id),  -- exactly one
  episode_order  TEXT,                   -- NEWEST | OLDEST; NULL = auto from itunes:type
  keep_mode      TEXT,                   -- LATEST | ALL; NULL = auto from itunes:type
  keep_count     INTEGER NOT NULL DEFAULT 10,            -- used when keep_mode is LATEST
  subscribed     INTEGER NOT NULL DEFAULT 1,             -- 0 = unsubscribed (tombstone)
  hlc            INTEGER NOT NULL
);

-- Two show IDs found to be the same feed. Reads resolve alias_id to canonical_id.
CREATE TABLE show_alias (
  alias_id      TEXT PRIMARY KEY,
  canonical_id  TEXT NOT NULL,
  hlc           INTEGER NOT NULL
);

-- Minimal record for any episode with position or played state; lets Played outlive the feed.
CREATE TABLE episode_meta (
  show_id          TEXT NOT NULL,
  guid             TEXT NOT NULL,
  title            TEXT NOT NULL,
  published_at     INTEGER NOT NULL,
  duration_ms      INTEGER,              -- player-reported only; the feed's duration stays local
  hlc              INTEGER NOT NULL,
  PRIMARY KEY (show_id, guid)
);

-- Position and played state live in separate tables: each is plain row-level LWW.
CREATE TABLE episode_position (
  show_id      TEXT NOT NULL,
  guid         TEXT NOT NULL,
  position_ms  INTEGER NOT NULL,
  duration_at_save_ms  INTEGER,      -- length of the copy the position was saved on (ad insertion guard)
  play_context  TEXT,                -- UP_NEXT | GENRE:<id> | SHOW:<id> | NONE; drives autoplay
  hlc          INTEGER NOT NULL,
  PRIMARY KEY (show_id, guid)
);

CREATE TABLE episode_played (
  show_id  TEXT NOT NULL,
  guid     TEXT NOT NULL,
  played   INTEGER NOT NULL,             -- 1 = played, 0 = marked unplayed
  played_by  TEXT,                       -- FINISHED | MARKED; only FINISHED counts for rotation
  hlc      INTEGER NOT NULL,
  PRIMARY KEY (show_id, guid)
);

-- Listening-time counters. Each device writes only its own rows, so LWW is exact
-- and totals are sums across devices. Recorded from v1; no stats screen yet.
CREATE TABLE listen_stats (
  device_id    TEXT NOT NULL,
  day          TEXT NOT NULL,            -- device-local date, 'YYYY-MM-DD'
  category_id  TEXT NOT NULL,
  wall_ms      INTEGER NOT NULL,         -- real time spent listening
  content_ms   INTEGER NOT NULL,         -- audio covered (2x speed = double wall_ms)
  hlc          INTEGER NOT NULL,
  PRIMARY KEY (device_id, day, category_id)
);

-- ===== Local only: re-derived from feeds =====

CREATE TABLE show_feed (
  show_id      TEXT PRIMARY KEY REFERENCES show(id),
  title        TEXT NOT NULL,
  image_url    TEXT,                     -- channel itunes:image
  itunes_type  TEXT,                     -- episodic | serial; drives the auto defaults
  author       TEXT,                     -- channel itunes:author, for the show page
  description  TEXT                      -- channel description (or itunes:summary), for the show page
);

-- Every category the feed declares, as written. Input for the prefill and any future tags.
CREATE TABLE show_feed_category (
  show_id            TEXT NOT NULL REFERENCES show(id),
  position           INTEGER NOT NULL,   -- order in the feed
  apple_category     TEXT NOT NULL,
  apple_subcategory  TEXT NOT NULL DEFAULT '',
  PRIMARY KEY (show_id, position)
);

CREATE TABLE episode (
  show_id        TEXT NOT NULL REFERENCES show(id),
  guid           TEXT NOT NULL,
  title          TEXT NOT NULL,
  published_at   INTEGER NOT NULL,
  enclosure_url  TEXT NOT NULL,
  image_url      TEXT,                   -- item itunes:image; null = use show's
  duration_ms    INTEGER,                -- feed value; episode_meta holds the player's
  download_path  TEXT,                   -- per device, never synced
  PRIMARY KEY (show_id, guid)
);

-- ===== Local only: sync machinery =====

CREATE TABLE device (
  id           TEXT PRIMARY KEY,         -- generated on first launch
  last_hlc     INTEGER NOT NULL,         -- highest HLC issued or seen
  pull_cursor  INTEGER NOT NULL DEFAULT 0  -- highest server_seq pulled
);

CREATE TABLE outbox (
  seq      INTEGER PRIMARY KEY AUTOINCREMENT,  -- local order only
  kind     TEXT NOT NULL,              -- e.g. episode_position, listen_stats, show, category
  payload  TEXT NOT NULL,              -- JSON, including its original hlc
  hlc      INTEGER NOT NULL
);
```

**Derived values** (computed, never synced)

- **canonical(show\_id)** = `COALESCE(show_alias.canonical_id, show_id)`. Every query that groups or joins by show goes through it. Aliases always point straight at the group's root, so a single lookup is enough.
- **duration(episode)** = `episode_meta.duration_ms` (player-reported) when present, otherwise the local `episode.duration_ms`, otherwise unknown.
- **counts\_as\_listened(episode)** = finished (`played_by = FINISHED`), **or** the current position is at or past the threshold: 30% of `duration` when it's known, otherwise 10 minutes. The rule follows the current position. Skimming through to hear one story counts once you're past the line, scrubbing back below it undoes it, and marking the episode unplayed (which resets the position to 0) clears it.
- **listened\_at(episode)** = the later of `episode_played.hlc` (only if `played_by = FINISHED`) and `episode_position.hlc` (if past the threshold), and undefined when the episode doesn't count.
- **lastListened(category)** = `MAX(listened_at)` over episodes in that category's subscribed shows. **lastListened(show)** works the same way for a single show.
- **In progress** = `position_ms > 0`, and either not played or the position's `hlc` is newer than the played `hlc` (a re-listen of a played episode, which stays played and out of the queues). In addition, either the show is serial or the position's `hlc` falls within the in-progress expiry window (default 6 months, user-configurable). Expiry is derived, never written, so nothing syncs and resuming the episode from Played brings it straight back.
- **Played** (screen) = every episode with a position or played row, joined to `episode_meta`, ordered by the later of its position and played `hlc`, newest first. One entry per episode, at its most recent activity. There's no listen log.

**Listening stats** are sums of `listen_stats` across devices. They're recorded from v1, but there's no stats screen yet.

**Artwork.** Show art comes from the channel-level `itunes:image href`, with RSS `<image><url>` as a fallback. Episode art comes from the item-level `itunes:image` and is often missing, so the UI uses `COALESCE(episode.image_url, show_feed.image_url)`. Images are cached on disk by URL through the image loader (Coil 3 is KMP), not stored in the database.

## Sync

Devices sync through Supabase. Each device works from its local database and outbox, so sync can always lag or fail without losing data. The private build is local-only and has no account. Sync and sign-in are part of v1, the first public release.

| Data | Rule |
| --- | --- |
| `episode_position`, `episode_played` | Last write wins, per row. They're separate tables so that each needs only a row-level rule |
| `episode_meta` | Last write wins, per row. The duration is only ever the player's, so there's no conflicting rule |
| `listen_stats` | Last write wins, per row. Only the owning device writes a row, so totals are sums across devices |
| `category`, `apple_category_map`, `show`, `show_alias` | Last write wins, per record; deletes and unsubscribes are tombstones |
| Feed cache, downloads | Not synced; each device re-fetches its own |

**Write triggers.** During playback, the device writes `episode_position` and adds the time played since the last write to today's `listen_stats` row:

1. Every 30 seconds
2. On pause
3. On stop
4. On seek or scrub (time skipped doesn't count towards listening stats)

Devices that are only browsing never write playback state.

**Outbox.** Every change, online or not, is written locally with its HLC first, then queued in `outbox`. The push drains the outbox, sending each change **with the HLC recorded when it happened**, never the push time. Retries resend the same row, so pushes are idempotent. Changes made in aeroplane mode keep their real times and merge into the right place when the device reconnects.

**Pull before play.** Before resuming any episode, the device pulls the latest state. This stops a device with stale data from writing an old position 30 seconds later and having it win as the newest write.

**Hybrid logical clock.** A 64-bit value with wall-clock milliseconds in the high bits and a counter in the low bits, so plain numeric comparison orders it.

- Issue: `hlc = max(now_ms, device.last_hlc.ms)`; the counter increments when the ms part doesn't advance.
- Receive: `device.last_hlc = max(device.last_hlc, incoming)`, so later local writes order after anything this device has seen.
- Skew then only affects truly concurrent writes, where neither device had seen the other's write.

**Backend: Supabase** (Postgres and Auth) on the free plan. Pro costs $25/month and is the step up if the app ever has real users.

- **Push.** The outbox drains to Postgres, and the server does the merging. Last-write-wins rows use `INSERT … ON CONFLICT … DO UPDATE … WHERE excluded.hlc > existing.hlc`. Postgres applies that `WHERE` to the whole row, which is why position and played state are separate tables. A losing write updates nothing, so it never bumps `server_seq`.
- **Pull.** Every server row also carries `server_seq`, a bigserial assigned on write. Devices pull `WHERE server_seq > device.pull_cursor`. **The HLC decides which write wins; `server_seq` decides what's new.** An HLC cursor would miss writes that arrive late carrying old timestamps, such as a phone coming out of aeroplane mode.
- **Daily background sync** (once sync ships). A WorkManager job that keeps idle devices fresh and stops Supabase pausing the free project for inactivity. If the project does pause, sync just waits; nothing is lost.

**Duplicate shows.** Two devices can subscribe to the same feed through different URLs before they sync: one an old URL that later redirects, one the new URL. Each gets its own show ID. Shows are compared by `feed_url_norm`, which is the URL with:

- the scheme and host lowercased;
- default ports removed;
- `http` and `https` treated as equal;
- a trailing slash on the path removed.

The query string is **never** touched, because premium tokens live in it. When two subscribed shows share a `feed_url_norm`, the group's root is the show with the earliest HLC (ties broken by ID). Every other member gets a `show_alias` row pointing straight at that root, never at another alias. Every device computes the same root from the same rows, and aliases are re-flattened on each pull. The alias's state and settings are then read through `canonical()`, so nothing is rewritten.

**Same GUID, same show.** `show.podcast_guid` is filled from the feed's `podcast:guid`. Two subscribed shows with the same GUID are the same show and are aliased exactly as above. URL normalisation stays the fallback when either show has no GUID.

**Auth.** Social sign-in only: Google on Android, Automotive OS and desktop, and Sign in with Apple on iOS (required alongside Google). There's no email/password and no magic links, so no SMTP is needed. Phones get the native account picker through supabase-kt Compose Auth; desktop uses supabase-kt's localhost callback. Every server table has `user_id UUID NOT NULL DEFAULT auth.uid()` with the row-level security policy `user_id = auth.uid()`, so the publishable key can ship in the app.

## Search

Search runs on our own index, built weekly from Podcast Index's public catalogue dump. It's for discovery only: imported and subscribed feeds are always fetched directly. The app never talks to Podcast Index: their terms (v2.0, October 2026) forbid API credentials in client code and any public service that proxies their API, but allow downloading the weekly dump and running our own index and API from it.

**Shape.** One small VPS runs Meilisearch (community edition, MIT) on `127.0.0.1:7700` and a Ktor gateway beside it. The gateway is the only public endpoint, reached through a Cloudflare Tunnel, so the box opens no inbound ports.

**Weekly import.** A JVM CLI runs overnight once a week:

1. Download the dump from podcastindex.org/datasets and read it with sqlite-jdbc.
2. Keep a feed only if it isn't dead, isn't a duplicate of another feed, has episodes, and last answered with a 2xx. Drop junk: a popularity score of 0 together with fewer than 3 episodes, or nothing newer than 3 years.
3. Map each feed to a small document: ID, title, author, owner name, artwork, feed URL, podcast GUID, language, explicit flag, `itunesType`, episode count, newest item date, popularity score, categories (`category1..10`, deduplicated) and `dedupeKey`. Every other column is dropped.
4. Build a fresh index `podcasts_<yyyyMMdd>`, apply the settings, upload in batches and wait for every task to succeed. Swap it with `podcasts`, then delete the old index. Any failure leaves `podcasts` untouched and the run exits non-zero.

Meilisearch's indexing memory is capped, so an import can't starve the gateway.

**Ranking.** Only title, author and owner name are searchable; descriptions are too keyword-stuffed. The ranking rules are words, typo, proximity, attribute, sort and exactness, then popularity score and newest item date, both descending. `dedupeKey` (the podcast GUID, else the content hash `chash`, else `id:<id>`) collapses one show listed under several feeds. A query returns at most 100 hits.

**Endpoints.**

| Endpoint | Does |
| --- | --- |
| `GET /v1/search` | `q` (1–100 characters after trimming), optional `lang` (matches the language prefix) and `hideExplicit`, `limit` up to 20, `offset` up to 40. Out-of-range input is a 400. Returns the hits and an estimated total |
| `GET /health` | No auth. Reports the gateway up and Meilisearch reachable |

A hit carries raw facts only: Podcast Index ID, podcast GUID, title, author, artwork, feed URL, language, categories, `itunesType`, explicit flag, episode count and newest item date. The client decides labels such as "Ended".

**Auth.** Every install has a Supabase session, anonymous until the user subscribes, so the gateway accepts any valid Supabase access token, anonymous ones included. Search isn't gated on entitlement. Tokens are verified locally against Supabase's published keys (ES256 by default): expiry, issuer and audience are checked, and the keys are cached, refreshed hourly or when an unknown key ID appears. The gateway never calls Supabase per request and holds no shared JWT secret. The private build has no account, so it sends a private token instead, compared in constant time and disabled when the server doesn't set one.

**Limits.** Per user (the token's `sub`): search allows bursts of 20, refilling at 1 a second. Over the limit is a 429 with `Retry-After`. Logs record method, path, status, latency and a hash of `sub`, never query text. Meilisearch errors are a 503, never a stack trace.

**Private-build passthrough.** Search sits behind a `SearchSource` interface. An optional source calls the Podcast Index API with the key and secret held on the server (environment only) and an honest `Damask/<version>` User-Agent. It's on only when `PI_PASSTHROUGH=true`, for the private build before the first import exists, and never for public users unless Podcast Index agree in writing.

**Hide explicit.** A plain preference in Settings, off by default, not a safety feature: the app is 18+. It filters search results on the show's explicit flag and hides episodes flagged explicit in their feed.

## Architecture

All logic lives in a shared KMP module. Each platform app supplies only audio playback and the shell around the shared Compose Multiplatform UI.

&#91;embedded content: architecture · shared module + 3 platform apps\]

Each platform implements the Player API and reports position back to the shared module, which writes the play events. Once you've subscribed, episodes come straight from each show's RSS feed; our search index is used only for discovery.

**Repository layout.**

| Path | Contains |
| --- | --- |
| `shared/core`, `shared/presentation`, `shared/ui` | The app's shared code: rules and data, ViewModels, Compose screens |
| `shared/api` | The search API's `@Serializable` request and response types. Pure Kotlin on every app target plus JVM; depends only on kotlinx-serialization |
| `server/search` | The Ktor search gateway (JVM) |
| `server/importer` | The weekly dump-to-Meilisearch CLI (JVM) |
| `deploy/` | docker-compose, Dockerfiles, Meilisearch settings, the importer's schedule |

**Dependency rule.** `server/*` may depend on `shared/api` and nothing else from `shared/`; a Konsist test enforces it. CI path filters build and test the server only when `server/`, `shared/api/` or `deploy/` change.

**API versioning.** The search API is path-versioned (`/v1/...`) and additive-only. Within a version, fields are only ever added, each with a default, and never removed or renamed; clients decode with `ignoreUnknownKeys = true`. A breaking change is a new `/v2` running alongside `/v1`. Golden-JSON contract tests in `shared/api` fail the build on an accidental break.

**Thin UI rule.** Every screen's state and logic lives in a shared ViewModel in `commonMain`. That's the AndroidX KMP `ViewModel`, exposing a `StateFlow<UiState>` and taking intents. Composables only render state and forward intents.

- No queue rules, formatting or validation inside composables.
- Navigation state (which screen, which arguments) is shared. Each UI renders it its own way.
- String and image resources are reachable from Swift (for example through moko-resources), or are accepted as a later duplication cost.
- Swift consumes shared code through SKIE or KMP-NativeCoroutines.

This keeps iOS free to replace any Compose screen with SwiftUI, one screen at a time, by hosting each in the other (`ComposeUIViewController` / `UIKitView`).

**Desktop playback**

- **Decoding: FFmpeg through JavaCV.** Use an LGPL FFmpeg build, shipped as per-platform native jars (not `javacv-platform`), with PCM output through `javax.sound`. `atempo` gives pitch-corrected speed, and `silenceremove` makes skip-silence possible later. It plays every format, including Ogg, Opus and FLAC. The playback loop (frame pull, filter graph, PCM out, seek, pause, buffering) is ours to write.
- **Networking: a loopback proxy over OkHttp.** It handles redirects, premium credentials, signed-URL retry, `Range` passthrough for seeking, and streaming to disk.
- **Proxy security.** Bind to `127.0.0.1` only, on a random port, with an unguessable per-session token in every path. Allow `GET` and `HEAD` only, and only for episodes the app asked to play. Start it lazily and stop it when idle.
- **OS media controls: Nucleus Media Control**, which covers MPRIS on Linux, SMTC on Windows and MPNowPlayingInfoCenter on macOS. JMTC is the fallback if it proves immature.
- **Rejected:**
  - vlcj: GPL licence, and VLC has to be bundled per OS.
  - JavaFX Media: no pitch correction, and no rate change at all on streams of unknown length.
  - Native players: three implementations, each with different format gaps.

**Packaging.** jpackage, with a jlink-trimmed runtime and AppCDS for faster startup. GraalVM native-image is deferred: the stack is heavy on JNI (Skiko, JavaCV, Nucleus, SQLite), AWT support on macOS is incomplete, and the Graal Gradle plugin doesn't work with the Kotlin Multiplatform plugin. To keep that door open, prefer libraries that don't rely on reflection, such as kotlinx.serialization over Gson or Jackson.

## Storage

Most of the app's footprint is disposable cache that the OS can reclaim. Audio is deleted as soon as it stops being useful.

| Kind | Location (Android) | Budget |
| --- | --- | --- |
| Prefetch audio: top of Up Next and each queue, plus Media3's streaming cache | `cacheDir`: the OS may delete it, and "Clear cache" wipes it | `min(getCacheQuotaBytes(), 5% of free space, user cap)`, LRU eviction |
| Explicit downloads (user tapped download) | `getExternalFilesDir()`: app-specific, never shared Music or Downloads folders, removed on uninstall | Counts toward the user's total budget |
| Artwork | `cacheDir`, Coil disk cache | `maxSizePercent(0.02)`, clamped to 10–250 MB |

**One copy per device.** Each device fetches an episode's audio once. Streaming fills the cache, and a later download reuses those bytes rather than fetching again: on Android, Media3's `CacheDataSource` and `DownloadManager` share one cache, and the desktop proxy writes to disk as it streams. Playback always prefers the local copy.

**Dynamic ad insertion.** Ad slots sit at fixed points in the content, but the ads that fill them vary in length with every fetch, so two devices' copies can be offset from each other by up to a couple of minutes. Nothing can line them up exactly. When resuming a position saved on a copy whose length differs from the local one by more than 5 seconds (`duration_at_save_ms`), rewind 30 seconds. That way you sometimes hear a little again, but never skip anything.

**Deletion triggers**

- **Marked played:** the audio is deleted after a 24-hour grace period, so an accidental mark can be undone.
- **Leaves the prefetch window:** prefetched audio is deleted; explicit downloads are kept.
- **In-progress expiry, retention pruning, unsubscribing:** the audio is deleted.
- **App start:** files are reconciled against the database, and orphans left by crashes or killed downloads are deleted.

**Budget and visibility**

- A total storage budget appears in settings. When it's exceeded, the oldest prefetched files go first, then the oldest explicit downloads, and the user is told before any explicit download is deleted.
- A storage screen shows usage per show, with a "Clear downloads" button.
- Automotive OS uses smaller default budgets.

**Polite downloading**

- Media3 `DownloadManager` requirements: unmetered network by default, `DEVICE_STORAGE_NOT_LOW`, optionally charging.
- Feed refreshes run as WorkManager jobs and use conditional GETs (`ETag` / `If-Modified-Since`).
- Media and caches are excluded from Android Auto Backup.

## Premium feeds

Premium shows arrive as private RSS feeds: a per-subscriber URL with a token in it, as issued by Patreon, Supercast, Supporting Cast, Memberful and similar hosts. They're supported in v1. Subscriptions locked to a platform (Apple Podcasts Subscriptions, Spotify, Wondery+) expose no feed and can't be supported.

- **Add by URL.** A paste-a-URL subscribe flow alongside search. Search can't find private feeds, and the same flow covers public feeds missing from the directory.
- **The URL is a secret.** It's never sent to the search API, it's masked in the UI, and it's kept out of logs and crash reports. It syncs to Supabase, protected by row-level security, because other devices need it.
- **Basic-auth feeds.** Credentials go in the Android Keystore or iOS Keychain, never in the database.
- **Replace feed URL.** An action on the show for when a host issues a new URL. The show keeps its ID, category, settings and history. Redirects and `itunes:new-feed-url` take the same path automatically.
- **A premium feed is its own show.** There's no linking or matching between feeds. Shows and episodes can't be matched reliably across feeds: GUIDs, titles, artwork and timings all differ. If you switch to an ad-free feed, you unsubscribe from the public one yourself. Played state doesn't carry over.
- **Lapsed or revoked feeds.** A 401 or 403 marks the show "feed unavailable" with a prompt to replace the URL. Episodes already downloaded stay playable, and nothing gets deleted.
- **Signed audio links.** Some hosts' enclosure URLs expire. A download that gets a 403 triggers a feed re-fetch for fresh links before it's reported as a failure.
- **Pocket Casts import.** Its OPML export includes private feed URLs (checked on a real export with an Acast premium feed), so premium shows come across with the import.

## Compliance

As designed, the app is outside the UK Online Safety Act, and it's for over-18s only. This is a conservative reading, not legal advice; a solicitor reviews it if the app does well.

**UK Online Safety Act: stay out of scope**

- **Search a single directory only:** our index of the Podcast Index catalogue. Searching more than one website or database makes the app a regulated search service. Adding the iTunes Search API, or any other second directory, would cross that line.
- **No user-to-user features without a review first.** That means no shared playlists, comments, public profiles or anything else users publish for one another.
- The app publishes no content of its own, so the pornography duties don't apply.

**18+ only, everywhere**

The catalogue has nothing reliable for age-limiting search results, so the app is for adults. Per-country ages of majority may come later; the rules differ by country, so for now it's one rating.

- Store listings are rated 18+.
- On mobile, query the Play Age Signals API (Android) or Apple's Declared Age Range API (iOS) at sign-in and on each launch. A minor signal shows a blocking "Damask is for over-18s" screen.
- There is no in-app self-declaration: an unknown signal doesn't block anything.
- Desktop and other non-store builds have no store signal and rely on the terms of service. A minor signal on any of the account's devices blocks the account everywhere.
- Launch in the UK only at first.
- Store the age **band** only (`18+`, `13–17`, `under-13`, `unknown`), never a birthdate, with one row per device on the server:

```sql
CREATE TABLE age_signal (               -- server; RLS on user_id
  user_id     UUID NOT NULL,
  device_id   TEXT NOT NULL,
  store       TEXT NOT NULL,            -- PLAY | APPLE
  band        TEXT NOT NULL,            -- 18+ | 13-17 | under-13 | unknown
  checked_at  TIMESTAMPTZ NOT NULL,
  PRIMARY KEY (user_id, device_id)
);
```

- `unknown` counts as no information, so it never overrides a known band.
- **A block lifts only when the account holder acts:** signing a device out or removing it from the device list deletes that device's row. Rows never expire on their own.
- "Hide explicit" is a plain preference, not part of the age gate (see Search).

**Mapping signals to bands.** Check the signal's status before its bounds, because the two APIs read null differently:

- **Apple:** a `nil` lower bound means the user is below the lowest age gate, so the band is minor.
- **Google:** a `null` lower bound together with a not-shared or verification-required status means no signal, so the band is `unknown`, not minor.

Each platform's mapping gets its own tests.

## Pricing and billing

The app is free on a single device and charges a subscription for sync. Sync is the only part with an ongoing cost, so it's the part that's charged for.

| Tier | What it includes | Price (starting point) |
| --- | --- | --- |
| Free | The whole app on one device: rotation, queues, playback, downloads | £0 |
| Sync | Cross-device sync, continuity between desktop and car, backup | £1.49/month or £12.99/year |

- **Trial:** a 1-month store introductory offer. Apple's "1 month" is a calendar month, 28 to 31 days. Google allows any length.
- **No lifetime purchase.** Sync costs money every month.
- **What you keep from a £12.99 annual sale:** about £9.20 through the stores (after 20% VAT and the 15% small-developer commission), or about £9.78 through Paddle. Monthly plans go through the stores only, because Paddle's fixed fee eats a third of £1.49.
- **Fixed costs:** roughly £360–470 a year (Supabase Pro, the search VPS, the Apple Developer Program, Google Play's one-off fee). That's covered by about 50 annual subscribers. The cost per user is close to zero, because audio and artwork never pass through our servers.

**Entitlements.** RevenueCat is the source of truth for purchases. It wraps Play Billing, StoreKit and web checkout, and is free below $2,500 monthly tracked revenue, then 1%. Its webhooks write an `entitlement` row to Supabase:

```sql
CREATE TABLE entitlement (              -- server; RLS on user_id
  user_id     UUID PRIMARY KEY,
  product     TEXT NOT NULL,            -- SYNC
  source      TEXT NOT NULL,            -- PLAY | APPLE | WEB | PROMO
  expires_at  TIMESTAMPTZ NOT NULL,     -- the store's date, mirrored; never computed by us
  updated_at  TIMESTAMPTZ NOT NULL
);
```

- An active entitlement from any source unlocks sync on every device.
- **Desktop never sells anything.** It reads the entitlement when you sign in.
- `PROMO` covers free time for beta testers or goodwill credit.
- **Web checkout (later, annual plans only):** Paddle as merchant of record, connected through RevenueCat's Paddle integration. It's for desktop-only users and anyone who'd rather pay on the web. The mobile apps don't point users to it while UK steering is restricted. Revisit when the CMA's steering rules take effect.

**Protecting search.** Free users search too, so the gateway must not be open to anyone:

- Every install signs in anonymously with Supabase, silently and with no account screen. When the user subscribes, the anonymous account is upgraded to Google or Apple sign-in, keeping its data.
- The gateway accepts only valid Supabase tokens and rate-limits each user (see Search).
- Play Integrity (Android) and App Attest (iOS) prove that requests come from the genuine app.

## Licences

Every library is under a permissive licence or the LGPL; nothing is GPL. Search is built from Podcast Index's public catalogue dump.

| Component | Licence | Notes |
| --- | --- | --- |
| Nucleus Media Control | MIT | Use `dev.nucleusframework:nucleus.media-control`, the current group ID |
| FFmpeg through JavaCV | FFmpeg LGPL 2.1+; JavaCV Apache 2.0 | Depend on `org.bytedeco:ffmpeg`, never the `-gpl` artifacts |
| JMTC (fallback) | MIT | Pre-1.0, and no macOS support |
| Kotlin, Compose Multiplatform, AndroidX (Media3, WorkManager, ViewModel), SQLDelight, Ktor, OkHttp, Coil, kotlinx.serialization, xmlutil, Okio, moko-resources, SKIE | Apache 2.0 |  |
| supabase-kt, KMP-NativeCoroutines | MIT |  |
| sqlite-jdbc | Apache 2.0 (SQLite: public domain) |  |
| Meilisearch (community edition) | MIT | Server-side only; not distributed |

**FFmpeg's LGPL obligations**

- Ship it as separate dynamic libraries, as JavaCPP does by default.
- Include the LGPL text, and link to the exact FFmpeg source of the build you ship.
- Don't prevent users from replacing the libraries.
- Never use FFmpeg in the iOS App Store build, because of the long-disputed LGPL and App Store compatibility. iOS stays on AVPlayer.

**Patents.** MP3 patents have expired. FFmpeg's own AAC decoder on desktop isn't covered by a patent licence, whereas the OS decoders on Android, iOS, macOS and Windows are. In a paid product this is a little more exposed. Where the build supports it, prefer FFmpeg's wrappers around the OS's own licensed decoders: `aac_at` (AudioToolbox) on macOS and `aac_mf` (Media Foundation) on Windows. Check during the desktop spike whether bytedeco's builds include them. Linux uses FFmpeg's own decoder; that risk is accepted.

**Podcast Index.** No build ships the key: the app only ever calls our gateway, and the index is rebuilt weekly from the public dump, as the terms (v2.0) allow. Every request we make to Podcast Index (the dump download, and the private-build passthrough) carries an honest `Damask/<version>` User-Agent. Search results are never stored: everything the app keeps about a show comes from its own feed. The iTunes Search API was rejected because Apple's terms permit its content only for promoting Apple's stores.

## Decisions

Questions that came up while the spec was being written, with how each was settled.

- **"Not now" on Up Next.** Decided for v1: there is no "Not now" or snooze. Dismissing an episode means swiping to Mark played on Up Next and genre queue rows (played\_by = MARKED). The episode leaves every queue, the category keeps its place and offers its next episode, and the rotation isn't affected. It can be undone from Played. Snooze ("bring it back later") can be added later if real use shows it's wanted.
- **Definition of "finished".** Decided: the true end, or stopping with 2 minutes or less left (see Finishing and autoplay). Multi-part stories inside episodic shows aren't detected, because no feed tag marks them. A show can be switched to oldest-first by hand.
- **Unknown duration.** The player's duration replaces the feed's \`itunes:duration\` as soon as the media is prepared. Unknown durations use a 10-minute threshold (see Derived values). Dynamic ad insertion is handled by keeping one copy per device and rewinding when the copies don't match (see Storage).
- **Category list.** Decided: categories are user-created, and Apple's categories act as overridable defaults through the mapping table.
- **Search backend.** Decided (Nov 2026): our own Meilisearch index, rebuilt weekly from the Podcast Index dump, behind a Ktor gateway on one VPS (see Search). Search is for discovery only; there is no bulk lookup for OPML import, so private feed URLs never reach our server. Podcast Index's terms v2.0 forbid client-side credentials and public proxies of their API.
- **Age rating.** Decided (Nov 2026): 18+ only, everywhere, launching in the UK first (see Compliance). No in-app self-declaration screen (removed 27 Nov).
- **Sync transport.** Decided: Supabase, with social sign-in only (see Sync). Drive, Firebase and gpodder were considered and rejected.
- **iOS UI.** Decided: Compose Multiplatform first. The thin UI rule (see Architecture) lets screens move to SwiftUI one at a time later. CarPlay, the audio session and Now Playing are native Swift either way.
- **Colour and Material You.** Decided: Material You is supported as a user option, Settings › Appearance › "Use wallpaper colours", off by default (the Damask brand scheme is the default; changed 27 Nov) and hidden where the platform lacks it. Everywhere else, and for users who turn it off, the app uses a Default brand scheme `(teal #0f766e primary, indigo tertiary; no red, orange or amber in the brand, since those read as danger)` with light and dark variants. UI code reads Material 3 colour roles only (`MaterialTheme.colorScheme`), never hex values, so the scheme can be swapped at runtime. The roles and both schemes are in the [design system](https://claude.ai/artifact/EZHMHvdS5nqQch8gN5a4SK).
- **Summarised values.** Decided: any relative or rounded value ("3 weeks ago", "32 min left", a percentage, a rounded count) must show the exact value on long press (touch) or hover/focus (desktop), and expose it as the accessible label. No summary ships without it.
- **Fonts.** Decided: platform default fonts only (`FontFamily.Default`: Roboto or the OEM font on Android, San Francisco on Apple platforms, Segoe UI on Windows). No bundled typeface.
- **Tablets and window sizes.** Decided: v1 supports all three Material window size classes. Compact (phones, under 600dp): bottom navigation bar, one pane. Medium (600–839dp): navigation rail, one wider pane. Expanded (840dp and up, which covers landscape tablets, unfolded foldables and desktop): rail plus two panes. Home is In Progress and Up Next beside Now Playing, Genres is the list beside the selected queue, Played is the list beside the episode. Built with `NavigationSuiteScaffold` and `ListDetailPaneScaffold` from Material 3 adaptive. Check early that the Compose Multiplatform port covers desktop; if it doesn't, branch on window size class by hand.
- **Name.** Decided: the app is called **Damask**, after the figured linen Belfast was famous for. The developer name is **Linenopolis**. Rejected: Mint (Mint Pod, an existing iOS podcast app), Linen ("Linen: Daily Devotion & Prayer" holds `com.linen.app` on Play, and "Linen Prayer" vs "Linen Player" is one letter apart), Flax (a friend's company, Flax & Teal), Weave (very crowded). Known near-miss: a small Linux wallpaper app called Damask on Flathub, so use a distinguishing store title there. Still to do: UK IPO and USPTO trademark search (classes 9 and 41), domain, Play package name, logo.
- **Now Playing artwork size.** Decided: the artwork takes whatever space is left after the controls and the show notes peek, which always keep their spacing. It shrinks on small phones rather than squeezing anything else. Feeds give large square originals (Apple requires 1400–3000 px), not thumbnails. So download each image once, then decode and cache it downsampled on device (for example a \~256 px copy for rows and the mini-player, and \~1024 px for Now Playing), never displaying the original.

## Desktop spike

The open work before desktop playback is considered de-risked:

- [x] FFmpeg through JavaCV: stream an MP3 and an Opus file through `atempo` at 2× with pitch preserved, output through `javax.sound`, with accurate seeking and pause/resume. (Done; see [Playback](playback.md) › Desktop. The `aac_at`/`aac_mf` OS decoder question is still open.)
- [ ] Loopback proxy: OkHttp, redirects (including HTTP to HTTPS), `Range` passthrough, basic auth, token-guarded paths, streaming to disk.
- [ ] Nucleus Media Control: media keys and Now Playing working on macOS, Windows and Linux. Check maintenance and licence. (Wired up with 2.5.18, MIT, on all three; the native library loads on macOS arm64. Still to confirm by hand on each OS.)
- [ ] Packaging: jpackage with a jlink-trimmed runtime and AppCDS, with the per-platform JavaCV natives and Nucleus bundled. macOS signing and notarisation of the native libraries.
