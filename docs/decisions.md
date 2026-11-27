# Decisions

Places where the code interprets the spec, fills a gap, or deviates from it. Each entry says what was decided, why, and where it lives. Newest first within each section.

## Search and age rating

Now specified in [spec › Search](spec.md#search) and [spec › Compliance](spec.md#compliance) (revised 27 Nov 2026). Recorded here: what the code will do where the spec leaves room.

- **The spec moved to `docs/spec.md`** (from `spec-and-design/spec.md`), so the docs and the spec live together. The design hand-off's `spec/v1-spec.md` is an older snapshot.
- **No `/v1/lookup` endpoint** (dropped from the task brief, 27 Nov). Import already fetches every feed directly (about 3 s for 78 feeds) and gets categories, artwork, `itunesType` and moves from the feeds. Lookup would have sent private feed URLs to our server.
- **The feed URL normaliser stays in `core`** (`core.sync.FeedUrl`). The brief moved it into `shared/api` so the server could use it for lookup; without lookup the server never normalises URLs, and the index carries no `urlNorm` fields.
- **Search results prefill from `category1`** when you subscribe from search: it's the main category. If it isn't an Apple top-level category, the prefill falls back to the feed's own `itunes:category` once the feed is fetched.
- **Anonymous Supabase users are accepted** (as specified). Each has its own stable `sub`, kept when the account is later upgraded, so per-user limits work. Scripted creation of fresh anonymous users is held back by Supabase's anonymous sign-in limits and a Cloudflare per-IP rule; see `deploy/README.md` when it exists.

## Screens

- **Empty categories are hidden on Genres.** All 19 Apple categories are seeded at import, and most users fill only some of them. `GenresViewModel`.
- **Presentation formats English strings** ("3 days ago", "32 min left"), so Compose and a future SwiftUI app show identical text. Translation would move these to shared resources. `format/Formats`.
- **"6 Nov 2026" is a Friday.** The design hand-off's exact-value example says "Thu 6 Nov 2026, 21:14"; the code formats the real weekday.
- **Icons are Material Icons Rounded** (`material-icons-extended`, the last published version, 1.7.3), the closest Compose equivalent of the design's Material Symbols Rounded.
- **Apostrophes in UI strings are typographic (’).** Compose Multiplatform resources don't unescape Android-style `\'`, which showed as a literal backslash.
- **The Damask theme is the default everywhere.** "Use wallpaper colours" is off until the user turns it on (27 Nov, user request; spec › Decisions updated).
- **Choosing a category opens a bottom sheet** with a search box and the full list (current category marked), instead of a dropdown. Search ignores case and treats "&" and "and" as the same; the matching lives in presentation (`matching`) so a SwiftUI sheet behaves identically.
- **Every show on the import review is ticked by default**, broken feeds included; unticked shows aren't subscribed, and the button counts only ticked ones. Tapping anywhere on a row toggles it (the category button keeps its own tap), and an "All shows" checkbox ticks everything, or unticks everything when all are ticked; it shows a dash when mixed.
- **Import shows a grid of every show** filling with artwork as each feed is read (failed feeds become dimmed outlines), above the counter. Tiles fit the space between 32 and 64dp; a library too big for 32dp shows as many as fit. Each tile is labelled "name, waiting/read/couldn’t be read" (or "Show 12 of 78" when unnamed) but never announces itself; the counter stays the live progress.
- **Import shows determinate progress**: "Reading feeds: 34 of 78", then "Saving shows: 12 of 78". Reading runs confined to the database thread (network calls still overlap), like feed refresh.
- **The app always opens on Home.** With no subscriptions, Home, Genres and Played show empty states, and Home points to Search and to Settings › Import from OPML. There is no first-run import screen (27 Nov, at the user's request).
- **No over-18 self-declaration screen** (removed 27 Nov at the user's request; spec › Compliance updated). The 18+ store rating and the v1 minor-signal block remain.
- **Up Next and genre queues load 30 rows at a time**, with "Show more".
- **Swipe actions:** swiping an Up Next or genre-queue row marks it played; on Played, swiping a played episode marks it unplayed. Both are also accessibility actions, because a swipe alone isn't usable with a screen reader.
- **A 12dp gap** between row artwork and text comes from the EpisodeRow artboard but isn't in `tokens.json`; it's `Spacing.Space3`.

## Rotation and listening

- **Re-listening a played episode** puts it in Home › In Progress but not in genre queues, following the spec's "stays played and out of the queues". `Rotation.genreQueue`.
- **Expired in-progress episodes** (episodic, untouched for 183 days, unfinished) go back into queues as ordinary unplayed episodes and resume at their saved position. The spec doesn't say whether they reappear. `Rotation.candidates`.
- **Marking played after passing 30 %** still counts towards the rotation, because the position rule applies independently of `played_by`. `ListeningRules.countsAsListened`.
- **Ties:** never-listened categories sort by name; never-listened shows sort by ID (effectively arbitrary). Sorting shows by subscription date would be nicer once it's stored. `Rotation.genres`, `Rotation.showRounds`.
- **Show-page autoplay on a newest-first show** plays the next *older* episode. `Autoplay.nextInShow`.
- **Genre-queue autoplay** can pick a half-listened episode next, because the queue leads with in-progress episodes. `Autoplay.next`.
- **"6 months"** is 183 days. `ListeningSettings`.

## Data

- **Foreign keys aren't enforced** (SQLite's default). With sync, a show can arrive before its category; the Uncategorised fallback covers that. `REFERENCES` clauses are documentation only.
- **Seeded rows use fixed IDs and HLC 0** (`uncategorised`, `apple:<name>`), so devices don't create duplicates and user edits always win. [data.md](data.md).
- **The HLC counter is 16 bits.** When it overflows within one millisecond, the clock borrows the next millisecond. `HlcClock`.
- **The outbox isn't written yet**, because the private build has no sync. Enqueue in the same transaction as each write when sync lands. `UserStateStore`, `FeedStore`.

## Feeds and import

- **Pocket Casts exports private feed URLs** (answers the spec's open question). A real export included an `access.acast.com` premium feed, so premium shows shouldn't need re-adding after import. Confirmed on one export with one premium host.
- **Show IDs hash the normalised feed URL**, not the URL as typed. The http and https spellings of one feed get the same ID and never need an alias; private feeds with different tokens stay distinct, because the query string is kept. `ShowIds`.
- **xmlutil instead of RSS-Parser** (which the spec's licence table lists). It gives direct control over nested `itunes:category`, namespace quirks and HTML entities. Also Apache-2.0. `feed/Xml.kt`.
- **All 19 Apple top-level categories are seeded at first import**, as the spec says. The Genres screen should probably hide empty categories.
- **Undated episodes** are stored with `published_at = 0`, so they sort oldest and don't move on every refresh. `FeedStore.applyFeed`.
- **Feeds that fail during import** can still be confirmed; the next refresh retries them. `OpmlImporter`.
- **The GUID falls back to the enclosure URL** when an item has no `<guid>`. `FeedParser`.
- **The OPML title falls back to `""`, never the URL**, because private feed URLs are secrets. `Opml`.
