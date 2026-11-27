# Decisions

Places where the code interprets the spec, fills a gap, or deviates from it. Each entry says what was decided, why, and where it lives. Newest first within each section.

## Search and age rating (27 Nov 2026; revised spec to follow)

These replace parts of spec › Compliance and the search design. The revised spec will supersede this section.

- **Search runs over our own hosted copy of Podcast Index's published dataset**, not their live API. This follows clause 3 of the Podcast Index terms. Hosting it is ours to solve: Supabase's free plan caps the database at 500 MB, so it will need a filtered subset or a bigger plan.
- **The app is rated 18+ everywhere.** The dataset has nothing usable for age-limiting search results. Per-country ages of majority may come later; the rules differ by country, so it's one rating for now. The band-based age logic (most restrictive band wins, explicit filter for `unknown` and minors) is superseded, pending the revised spec.
- **No separate live-API path for the private build.** It's a staging post, not worth its own search implementation. Search waits for the hosted dataset.
- **No partial gating** (for example, turning Search off for under-18 bands). A solicitor will review this if the app goes well.
- **Unchanged:** searching a single directory (the Online Safety Act scope argument), and the library explicit filter, which works from the feeds' `itunes:explicit`.

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
