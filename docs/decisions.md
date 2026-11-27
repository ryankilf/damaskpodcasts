# Decisions

Places where the code interprets the spec, fills a gap, or deviates from it. Each entry says what was decided, why, and where it lives. Newest first within each section.

## Search and age rating

Now specified in [spec › Search](spec.md#search) and [spec › Compliance](spec.md#compliance) (revised 27 Nov 2026). Recorded here: what the code will do where the spec leaves room.

- **The spec moved to `docs/spec.md`** (from `spec-and-design/spec.md`), so the docs and the spec live together. The design hand-off's `spec/v1-spec.md` is an older snapshot.
- **No `/v1/lookup` endpoint** (dropped from the task brief, 27 Nov). Import already fetches every feed directly (about 3 s for 78 feeds) and gets categories, artwork, `itunesType` and moves from the feeds. Lookup would have sent private feed URLs to our server.
- **The feed URL normaliser stays in `core`** (`core.sync.FeedUrl`). The brief moved it into `shared/api` so the server could use it for lookup; without lookup the server never normalises URLs, and the index carries no `urlNorm` fields.
- **Search results prefill from `category1`** when you subscribe from search: it's the main category. If it isn't an Apple top-level category, the prefill falls back to the feed's own `itunes:category` once the feed is fetched.
- **Anonymous Supabase users are accepted** (as specified). Each has its own stable `sub`, kept when the account is later upgraded, so per-user limits work. Scripted creation of fresh anonymous users is held back by Supabase's own anonymous sign-in limits (and optionally a CAPTCHA); there's no per-IP limit on the gateway (see Server below).

## Server

The search backend (`shared/api`, `server/*`, `deploy/`; see [server.md](server.md)). User decisions from 27 Nov first, then judgement calls made while building it.

**Decided by the user (27 Nov)**

- **Ingress is Caddy, not a Cloudflare Tunnel or proxy.** Cloudflare is DNS only (grey cloud) and TLS terminates on the VPS: Caddy gets Let's Encrypt certificates (ZeroSSL fallback) and reverse-proxies to the gateway, which publishes no host port. Firewall: 80 and 443 open, SSH key-only. `deploy/docker-compose.yml`, `deploy/Caddyfile`.
- **Per-user rate limits only; no per-IP limit.** Shared IPs would knock out legitimate users. If anonymous accounts are abused, the fallback is an IP limit for anonymous tokens and the user limit for signed-in ones. `deploy/README.md` › Abuse.
- **`/health` is `{status, db, cert}`**, replacing the first `{status, meilisearch}` draft (never released). `db` means Meilisearch is reachable and `podcasts` exists; `cert` is `ok`/`warning`/`expired` from a TLS handshake to `PUBLIC_HOSTNAME`, or `unknown` without one, cached an hour. 503 only when `down`. `shared/api` `HealthResponse`, `server/search` `Health.kt`.
- **Monitoring is an external uptime service** (none named): watch `/health`, alert on non-200, a keyword alert on `warning`, and the service's own certificate check. `deploy/README.md` › Monitoring.

**Judgement calls**

- **A server-only `server/meili` module** holds the Meilisearch client, the document shape and the index settings, so the importer and gateway can't drift apart. The rule "server may use only `shared/api` from `shared/`" still holds: `server/meili` isn't in `shared/`.
- **Read and write Meilisearch clients are split.** The gateway only ever holds `MeiliClient` with a key restricted to `search` on `podcasts`; `MeiliAdmin` and the master key stay in the importer.
- **The search key's value is derived, not printed.** Meilisearch computes a key as `HMAC-SHA256(master key, uid)` (verified against v1.58.0 and asserted in `MeiliClientTest`), so `.env` holds a fixed UID, the importer ensures that key exists on every run, and `deploy/search-key.sh` computes the value.
- **The `lang` filter uses a `languagePrefix` field** (`en-GB` becomes `en`), not a prefix match on `language`. Meilisearch filters match whole values, so the brief's filterable `language` becomes `languagePrefix`; `language` is kept as written, for display.
- **"Not a duplicate" is `duplicateOf` `''`, NULL or 0.** The first real import kept 0 rows. In the real dump (21 Nov 2026), 4,763,436 of 4,768,189 rows store "none" as the text `''`, and duplicates carry the other feed's integer ID; the fixture had used NULL. The fixture now mostly uses `''`, like the dump. NULL `popularityScore` counts as 0, and NULL `newestItemPubdate` as old, in the junk rule. `DumpReader`.
- **`popularityScore` runs 0–9 in the real dump** (one row has 29), not 0–5 as the brief said. The junk rule only tests for 0, so nothing changes; ranking uses it as given.
- **The dump URL was verified** (`podcastindex.org/datasets` loads `/api/datasets`): `https://public.podcastindex.org/podcastindex_feeds.db.tgz`, about 1.8 GB, rebuilt weekly from Saturday 22:00 UTC. The page says generic User-Agents get a 403, so the importer sends `Damask/<version> (search index importer)`. The timer runs Sundays at 03:30 UTC.
- **The Supabase JWKS path was verified** from Supabase's signing-keys docs: `$SUPABASE_URL/auth/v1/.well-known/jwks.json`, with P-256 (ES256) keys and a 10-minute edge cache. The gateway refreshes hourly or on an unknown `kid`, at most every 30 s, and allows 30 s of clock leeway.
- **`SUPABASE_URL` is optional** so the local stack can run with only the private token. The gateway refuses to start with neither configured.
- **Rate limiting is a custom token bucket** inside Ktor's RateLimit plugin. Ktor's built-in limiter refills the whole quota per period rather than 1 a second. The bucket reports when it'll be full again, which is when Ktor evicts it, so memory tracks active users only.
- **The health `db` check is an empty search**, because the gateway's search-only key isn't allowed to read index metadata.
- **The certificate check doesn't verify trust**, only expiry: it has to report an expired or broken certificate, not fail on it. An unreachable host counts as `expired`.
- **Passthrough paging:** the Podcast Index API has no offset, so `PodcastIndexSource` asks for `offset + limit` and drops the front. Language and explicit filtering happen server-side; categories are ordered by category ID.
- **Images are built from Gradle's `installDist` output**, not a Gradle build inside Docker: configuring this Gradle project needs the Android SDK. `deploy/build.sh` does both steps.
- **The JRE base image is `eclipse-temurin:21.0.13_9-jre-noble`**, not the newer `21.0.13.1_2`, which isn't published for arm64 yet. Containers run as UID 10001.
- **Caddy's access log is off**: it would record query strings, so search text. The gateway's own log has no query.
- **The local stack's dev-only values are committed** (`deploy/local.env`) and allow-listed in the secret scan, so `docker compose up` works out of the box. `deploy/.env` is gitignored.

**Observed, not changed**

- **Ranking can prefer a mirror feed for the deduplicated slot.** Meilisearch's `attribute` rule (where the matched word sits in the title) comes before popularity. So for "hardcore", "Hardcore History (mirror)" beats "Dan Carlin's Hardcore History", and `distinctAttribute` keeps only the better-ranked of the two. Putting `popularityScore:desc` before `attribute` would change it, but the ranking rules were Decided in the brief, so this needs a decision.

## Branding

- **The app icon is logo D2, "Woven play on damask"** (27 Nov), generated for every platform by `branding/icon/generate.py` (see [branding.md](branding.md)). The PNGs are drawn directly with Pillow, supersampled, rather than rendered from SVG, because the only local SVG renderer handled the design's clip paths poorly. Android's monochrome layer keeps the diaper at 16% so themed icons stay tone-on-tone, as in the design's "Themed" variant.
- **The desktop app's package name is "Damask"** (was the reverse-domain ID), so menus, the Dock and installers show the app's name.

## Screens

- **Empty categories are hidden on Genres.** All 19 Apple categories are seeded at import, and most users fill only some of them. `GenresViewModel`.
- **Presentation formats English strings** ("3 days ago", "32 min left"), so Compose and a future SwiftUI app show identical text. Translation would move these to shared resources. `format/Formats`.
- **"6 Nov 2026" is a Friday.** The design hand-off's exact-value example says "Thu 6 Nov 2026, 21:14"; the code formats the real weekday.
- **Icons are Material Icons Rounded** (`material-icons-extended`, the last published version, 1.7.3), the closest Compose equivalent of the design's Material Symbols Rounded.
- **Apostrophes in UI strings are typographic (’).** Compose Multiplatform resources don't unescape Android-style `\'`, which showed as a literal backslash.
- **When an import finishes, the app goes to Home** (closing Import and Settings), where Up Next now has the new shows, and a snackbar says "Added 78 shows". There is no "done" screen; the import resets for next time. The navigation and the one-off message live in `AppViewModel`.
- **The Damask theme is the default everywhere.** "Use wallpaper colours" is off until the user turns it on (27 Nov, user request; spec › Decisions updated).
- **Choosing a category opens a bottom sheet** with a search box and the full list (current category marked), instead of a dropdown. The sheet is always full height, so filtering never resizes it (Material sheets otherwise shrink to their content). Search ignores case and treats "&" and "and" as the same; the matching lives in presentation (`matching`) so a SwiftUI sheet behaves identically.
- **Plain-English copy throughout**: no jargon ("OPML" appears once, in brackets, as the label some apps use for their export) and no app names; on screen, categories are called genres; network and feed errors read like "This show has gone" or "Took too long", never HTTP codes.
- **Every show on the import review is ticked by default**, broken feeds included; unticked shows aren't subscribed, and the button counts only ticked ones. Tapping anywhere on a row toggles it (the category button keeps its own tap), and an "All shows" checkbox ticks everything, or unticks everything when all are ticked; it shows a dash when mixed.
- **Artwork fades in everywhere** (Coil crossfade on the shared image loader). Memory-cache hits appear instantly, as Coil doesn't crossfade them.
- **Ambient light around artwork** (`AmbientGlow`, from the NowPlayingPane artboard) draws the artwork again behind itself, at its own footprint and shape, blurred 40dp at 90% opacity, so it glows out evenly into a 40dp margin, following the cover's shape and colours on every platform without a palette library. (The artboard's radial-gradient CSS looked too round in practice.) Used on the import cover card now; meant for Now Playing too.
- **Import shows the latest show's cover**, a companion to the progress bar (while reading feeds, and while saving): an elevated card with the cover only (no caption, whose varying line count moved the cover between shows; the name is in the screen-reader label). A newer show slides in from the end edge and pushes the current one towards the start, at most once every 2.5 seconds, start to start (the slide always gets to finish first); covers in between are skipped, and each image is downloaded before it slides in. The slide uses the OS's standard duration where there is one (Android `config_mediumAnimTime`, with the user's animation scale applied), else Compose's 300ms. The card is labelled "Latest: …" but never announces itself; the counter stays the live progress. This replaced an earlier filling grid of every show.
- **Import shows determinate progress**: "Reading feeds: 34 of 78", then "Saving shows: 12 of 78". Reading runs confined to the database thread (network calls still overlap), like feed refresh.
- **The app always opens on Home.** With no subscriptions, Home, Genres and Played show empty states, and Home's empty state has "Search for podcasts" (opens the Search tab) and "Import from OPML" (opens Import; also in Settings). There is no first-run import screen (27 Nov, at the user's request).
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

- **`http://` requests go over HTTPS first** (feeds and artwork), like an interceptor: stored URLs are never rewritten, so sync and show IDs are unaffected, and the upgrade alone never counts as the feed moving. If HTTPS can't connect or returns an error, the original `http://` request is retried. Android blocks plain HTTP by default and we haven't opted in, so there that retry fails with "Not available over a secure connection". Whether to allow plain HTTP (older shows' audio often needs it) is decided when playback arrives. `feed/HttpsUpgrade.kt`.
- **Network failures are described readably** ("Couldn't connect", "Timed out", …) rather than by exception name, and never include the URL.

- **Pocket Casts exports private feed URLs** (answers the spec's open question). A real export included an `access.acast.com` premium feed, so premium shows shouldn't need re-adding after import. Confirmed on one export with one premium host.
- **Show IDs hash the normalised feed URL**, not the URL as typed. The http and https spellings of one feed get the same ID and never need an alias; private feeds with different tokens stay distinct, because the query string is kept. `ShowIds`.
- **xmlutil instead of RSS-Parser** (which the spec's licence table lists). It gives direct control over nested `itunes:category`, namespace quirks and HTML entities. Also Apache-2.0. `feed/Xml.kt`.
- **All 19 Apple top-level categories are seeded at first import**, as the spec says. The Genres screen should probably hide empty categories.
- **Undated episodes** are stored with `published_at = 0`, so they sort oldest and don't move on every refresh. `FeedStore.applyFeed`.
- **Feeds that fail during import** can still be confirmed; the next refresh retries them. `OpmlImporter`.
- **The GUID falls back to the enclosure URL** when an item has no `<guid>`. `FeedParser`.
- **The OPML title falls back to `""`, never the URL**, because private feed URLs are secrets. `Opml`.
