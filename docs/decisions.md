# Decisions

Places where the code interprets the spec, fills a gap, or deviates from it. Each entry says what was decided, why, and where it lives. Newest first within each section.

## Search and age rating

Now specified in [spec › Search](spec.md#search) and [spec › Compliance](spec.md#compliance) (revised 27 Nov 2026). Recorded here: what the code will do where the spec leaves room.

- **The spec moved to `docs/spec.md`** (from `spec-and-design/spec.md`), so the docs and the spec live together. The design hand-off's `spec/v1-spec.md` is an older snapshot.
- **No `/v1/lookup` endpoint** (dropped from the task brief, 27 Nov). Import already fetches every feed directly (about 3 s for 78 feeds) and gets categories, artwork, `itunesType` and moves from the feeds. Lookup would have sent private feed URLs to our server.
- **The feed URL normaliser stays in `core`** (`core.sync.FeedUrl`). The brief moved it into `shared/api` so the server could use it for lookup; without lookup the server never normalises URLs, and the index carries no `urlNorm` fields.
- **Search results prefill from `category1`** when you subscribe from search: it's the main category. If it isn't an Apple top-level category, the prefill falls back to the feed's own `itunes:category` once the feed is fetched.
- **Anonymous Supabase users are accepted** (as specified). Each has its own stable `sub`, kept when the account is later upgraded, so per-user limits work. Scripted creation of fresh anonymous users is held back by Supabase's anonymous sign-in limits and a Cloudflare per-IP rule; see `deploy/README.md` when it exists.

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

## Genre row artwork

- **An offset stack of up to three covers, not a 2×2 mosaic.** Genres are list rows, where a mosaic tile would be about 28dp and podcast art becomes noise. The front cover stays recognisable and the covers behind it say "more than one". `ui/components/CoverStack.kt`.
- **The covers are the shows next up in the genre's queue** (`Rotation.coverShows`), so the picture previews what you'll hear. It's cheap because it reads the lazy queue only until it has three shows. The user's fallback, if it hadn't been cheap, was a random pick seeded by the date, so it wouldn't move all the time.
- **Artwork gets a faint `onSurface` hairline (16% alpha), everywhere `Artwork` is used.** Lots of covers are mostly white, or mostly black, and melted into the page in light or dark mode. Tinting toward the text colour means the edge shows only where a cover is close to the page colour. An opaque `outlineVariant` line was tried first, but it framed every coloured cover. In a stack, the covers behind are shaded 18% per step, so they recede and a white cover behind still shows. This replaced a surface-coloured ring between covers, which made white covers worse.

## Episode search (deferred)

- **No episode search for now; much later.** The search index holds shows only (the Podcast Index dump has no episodes). Searching episodes everywhere means fetching about 2.8 million feeds regularly, and weekly isn't fresh enough for news shows. That's too much crawling and storage for this setup. Cheaper options for when it's picked up: full-text search over the episodes of shows you follow, on the device (it means keeping episode text longer than the 10 kept episodes); filtering a show's back catalogue on its page; or a server index limited to popular, active shows.

## Now Playing and show notes

- **Show notes are stored per episode in the feed cache (`episode.notes_html`, schema 4)** and read one episode at a time (`EpisodeNotes`), never in the library snapshot, which loads every episode. They come from `content:encoded`, else `description`, else `itunes:summary`, capped at 100,000 characters (some feeds paste whole transcripts).
- **Migration 3 → 4 clears the feeds' ETag and Last-Modified**, so the next refresh downloads every feed in full and fills in notes for episodes already cached. Otherwise unchanged feeds answer 304 and those episodes would never get notes.
- **Notes are parsed in presentation, not rendered as HTML** (`ShowNotes` → `NotesBlock`/`NotesSpan`). Compose Multiplatform 1.12 has no common `fromHtml`, and a small structured model works the same on every platform, SwiftUI included. It keeps paragraphs, list items, headings (as bold), bold, italic and links, turns bare URLs into links, and drops images, embeds, scripts and styles. It's a lenient tokenizer, because feed markup is often broken, and nothing in it is executed.
- **Only http, https and mailto links open**, checked in presentation and again in the UI. `javascript:`, `intent:` and `file:` links show as plain text.
- **Tapping an episode opens an ordinary screen laid out like Now Playing**, with the top bar, a back arrow and the tabs. Its show notes are a second pushed screen, so back works the same as everywhere else. The design's full-screen player with a collapse arrow only makes sense with the mini-player and playback behind it, and without them it felt broken. Whether tapping an episode should also start playing it, and how the mini-player and expanded player fit around it, is to be decided with playback (the user leans towards playing straight away).
- ~~Tapping an episode plays it straight away and opens its screen.~~ Superseded 28 Nov, once every row had its own play button: tapping opens, play buttons play (see below). The mini-player above the tabs carries what's playing while you browse.
- **Where a platform has no engine** (desktop until the FFmpeg spike, iOS), play, skip and speed show but are disabled (`AudioEngine.available`).
- **On Android, "playing" is playWhenReady**, so buffering isn't a stop (no save, no finish rule). Transient pauses for calls are handled by Media3 and aren't saves either.
- **Audio over `http://` is upgraded to HTTPS with no fallback yet**; allowing cleartext for audio is an open question.
- **Links appear twice on purpose:** inline in the text, and listed under Links as full-size rows with their host, as in the NowPlayingNotes artboard. Inline links are small touch targets; the list gives each one 48dp and tells apart links with the same text ("Instagram", "Instagram").
- **The play button carries the damask diaper** (`damaskFill`), as the design system specifies for filled play and pause buttons only.

## Home

- **In progress is a panel, as in the design's Main artboard:** a primary-container card with the count beside the heading. It shows the three most recently played; "See all N" opens the full list (`Destination.InProgress`). Section headings are sentence case ("In progress", "Up next"), as in the design.

## Window sizes

- **Built by hand from the window width, not with `NavigationSuiteScaffold` / `ListDetailPaneScaffold`.** The spec allowed this if the Compose Multiplatform port was in doubt. Hand-built panes keep navigation in the shared `NavState`, with no second back stack inside a scaffold.
- **The right-hand pane on Home and Played is the playing episode, until you open another.** An opened episode shows there instead; back returns the pane to what's playing.
- **Wide episode screens put the show notes beside the player** (the user's call), from 720dp.

## Design pass, 28 Nov (design canvas v1 screens)

- **Five tabs:** Home, Genres, Shows, Played, Search (the user's call on the canvas: "Extra clicks be damned"). Shows is every subscribed show A–Z; on wide layouts it sits beside the selected show's page.
- **Show page: a plain first version** (artwork, name, genre, every episode in the show's own order, playing with the show's context) until the canvas has one. The design agent has been asked for it, with per-show settings and the feed-unavailable state.
- **Phone headings are left-aligned**, as on every canvas screen: "Home" (not "Damask") with the cog, + on Shows, a back arrow on inner screens. A genre's queue draws its own heading (back, name, "Last played … · N shows").
- **Empty states use the canvas's shared EmptyState:** a damask tile with an icon, a title, one line, and at most one filled button. Home with no shows (Search, then Import), Home with nothing new (Check now), an empty genre (with when it was last played), Played.
- **Settings in groups:** Appearance (wallpaper colours), Library (Import subscriptions), Search (Hide explicit). Account (sync, subscription) and About (licences, version) come when those exist.
- **Shows without artwork get their initials on a colour** (the user's call), from a fixed palette of muted earthy colours (`ArtworkFallbackColors`, all passing 4.5:1 for white). The colour comes from a stable FNV-1a hash of the show's permanent ID, so it's the same everywhere and on every device; the import list uses the ID the show will have. The tile sits behind the artwork, so it also shows while loading and when an image fails. Initials come from the first two significant words ("Empires of Dust" ED, "The Rest Is History" RH).
- **"Last played" reads mid-sentence:** "Last played yesterday", not "Yesterday" (`Formats.lastPlayed`).
- **Screens fade through, not slide** (the user's call: "a fade or something would be enough"): a quick fade out then in, keyed on which screen is shown, so data refreshes within a screen don't animate. Slides would fight the back gesture's own animation and suggest a direction tabs don't have. See spec › Screens › Motion.
- **Discover, not Search** (canvas, 28 Nov): the fifth tab is Discover, with the compass icon, so it can't be confused with "Search your shows" on Shows. The + on Shows went: Discover covers adding.
- **The show page** follows the canvas's Show artboard. Its order chip writes `show.episode_order`, so one setting controls the list and what plays next. Changing genre writes `show.category_id`, from the same genre sheet Import uses (`CategorySheet`). Both are synced writes with a fresh HLC (`ShowSettings`). How often a show publishes comes from the median gap between its latest ten episodes (daily up to 1.5 days, weekly up to 9, every two weeks up to 18, monthly up to 40, otherwise nothing is said), and the usual day only when at least 60% of them share it.
- **Unsubscribe has Undo** (the canvas left it open): Unsubscribe in the show's menu acts at once, leaves the page, and offers Undo in a snackbar. The show row stays as a tombstone (`subscribed = 0`), so Played keeps it and Undo is just a write back. It's reversible and quick, so it needs no confirmation dialog.
- **Show author and description are stored** (schema 5, `show_feed.author` and `show_feed.description`). The migration forgets the HTTP validators, so the next refresh fills them in for every show.
- **The "plays next" highlight runs full width** (the user's call on the canvas), so its row lines up with the others.
- **ViewModels read one snapshot** (found in the DRY sweep): `Rotation` carries the `Library` it orders, and ViewModels collect `graph.rotation` alone. Combining `library` with `rotation` could briefly map rows from a new snapshot while ordering them by the previous one.
- **The genre sheet lists genres one way everywhere:** A–Z with Uncategorised last (`Library.categoryOptions()`), where the show page had used storage order.
- **Opening and playing are separate** (the user's call, 28 Nov): tapping an episode opens its screen and doesn't play; every play button (rows, the show page's Resume / Play, the player, the mini-player) plays or pauses in place and never navigates. The show page's button reads "Pause" while its episode plays. Play on an opened episode uses the context it was opened from.
- **The opened episode is highlighted in its list** (wide layouts, where the list stays beside it), in secondary-container like a selected genre or show, but full width like the show page's "plays next" row, so artwork and titles stay aligned. `RowPlayer` carries which episode is open to every list.
- **A genre's page works like a show's** (the user's call, 28 Nov): covers of its next shows, a Resume / Play button that plays through the genre ("all my news in a row"), and an order: Variety (the rotation), Newest first, Oldest first. It's stored on `category.episode_order` (schema 6), synced, and applies to the genre's queue, the episodes it offers Home's Up Next, and autoplay within it, so the list always matches what plays next.
- **Oldest first keeps every episode** (the user's point: oldest first needs the old ones). Retention normally keeps an episodic show's latest 10; a show set to oldest first, or in a genre set to oldest first, keeps everything its feed has. Switching to oldest first clears the feeds' HTTP validators and refreshes them at once, so the server sends the whole feed (a 304 would bring nothing back). The BelTel went from 10 episodes to 901. Limits: we only get what the feed lists; hosts that cap their feed at the latest N can't go further back without feed paging (Podcasting 2.0 / RFC 5005), not supported yet. Switching back trims to 10 on the next refresh.
- **HTTP request log in debug builds** (the user's ask): one line when each request starts (number, method, URL, wall-clock time), one when its response ends (number, status, URL, bytes, ms), `✗` when it fails. A shared Ktor plugin, so it covers every platform and every client. Off in release builds: whole URLs include private feeds' tokens. Its first catch: both Gospel in Life feeds drop the connection about 10 s in on Android (`ClosedByteChannelException`, retries fail the same way) while the same URL fetches in 0.4 s from desktop and curl; not caused by the log (still fails with it off). To look into.
- **Highlights on a tinted panel darken the panel, not swap tone** (found with wallpaper colours): a highlighted row inside In progress (primary-container) was secondary-container, which some schemes make paler than the panel, so the highlighted row looked like the one *not* selected. Rows on the page still use secondary-container (the design); on a panel they use the panel's colour with its content colour at 12% over it, which reads as selected in every scheme. Genre pages mark "plays next" too (the user's call: you should see what Resume will resume), now as that darker shade inside the panel. The brand scheme itself didn't need changing.
- **The In progress panel is a light wash** (the user's call, 28 Nov): primary-container at 40% over the page (`panelColor()`) instead of solid primary-container, so it marks the section without shouting. A highlighted row inside it (plays next, or opened) is the same colour at full strength (the user's call: one colour, the unhighlighted rows mostly see-through), so it's always the stronger one in every scheme. Rows inside the panel pass the panel colour as their background, on Home and on genre pages.
- **The play-button pattern is a tone worked out from the button's own colours** (the user's call): `damaskTone(fill, onFill, strength)` nudges the fill 9% towards its content colour (4% under words, Resume / Play), instead of drawing the content colour at 16% over it. The two tones stay close, so text and icons read cleanly, and because it's derived rather than a palette it suits any theme, wallpaper colours included.
- **No episode counts on the show page** (the user's call, 28 Nov): a count would be either what we keep (10 for most shows) or what the feed lists (capped by many hosts), and neither is the show's real number. The BelTel showed "10 episodes" until a full fetch found 901. The facts line is "Weekly · Last played today"; long press adds the usual publishing day and the exact last-played time.
- **Big heading → small sticky heading** on show and genre pages (the user's ask): a small bar stays put with back and the page's actions; when the big title's bottom edge scrolls under it, the page fades in small: its cover (a genre's front cover), its name, and a round play button standing in for the big Resume / Play that has scrolled away (`CollapsingTopBar`, `BarArtwork`, `BarPlayButton`). The bar keeps the page's colour throughout and has no dividing line (the user's calls: a colour change looked wrong, and the hairline was too heavy). The genre page's back arrow moved from its scrolling header into that bar. The small name is hidden from screen readers, which already have the big one as the heading.

## Design pass, 28 Nov evening (design agent's "Damask design changes — 28 Nov, afternoon")

- **Genres have colours** (design hand-off › GenrePalette): 16 fixed hues plus grey for Uncategorised, stored on `category.colour` as the hue's name (schema 7, synced). They don't follow Material You. Tones per hue for light and dark come from the canvas's `tokens.json` (`GenreTones`): stripe, container (tone 90 / 30) and on-container (tone 10 / 90, AA in both themes, tested). A genre gets the least-used colour once it has shows (at start-up, after import, after moving a show), so the colours of genres in use only repeat past 16; Apple's 19 seeded genres don't use colours up while they're empty.
- **Where genre colour shows:** the genre chip on rows and the player (container + on-container), a 4dp stripe at the start of genre-queue rows and a 4×22dp bar beside the genre's name on its page, and a 45% tint on Genres list rows (the open one on tablets in the full container colour with a 2dp ring). "Longest unplayed" is gone: the list is already stalest first.
- **Artwork placeholders take the genre's colour** (the user's call): the initials tile is the show's genre's container tone with on-container initials, replacing the hash-picked palette, so a show's tile matches its chips and stripes.
