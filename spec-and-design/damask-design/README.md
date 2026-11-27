# Damask: design hand-off

Snapshot of the design work for Damask (developer: Linenopolis), exported 27 Nov 2026. Drop this folder into the repo (for example `docs/design/`) so Claude Code can read it. The live versions are still on claude.ai; this is a copy, so re-export after significant design changes.

## What's here, and which wins

1. `spec/v1-spec.md` is the source of truth for behaviour. If a screen disagrees with the spec, the spec is right. Its **Decisions** section records settled choices: name, Material You, fonts, exact values on long press, window size classes, artwork sizing, no "Not now".
2. `design-system/` is the source of truth for look.
   - `README.md` covers the rules: colour roles and what they mean (`primary` = unfinished, `tertiary` = category), the copy voice, type, spacing, shape, and the damask pattern on filled play buttons.
   - `tokens.json` holds every value: 26 Material 3 colour roles across six themes (Default light/dark, plus four Material You samples), type styles, spacing, radii.
3. `compose/DamaskColors.kt` contains the Default light and dark `ColorScheme`s, generated from `tokens.json`. Use them when dynamic colour is off or unavailable; otherwise use `dynamicLightColorScheme` / `dynamicDarkColorScheme`. The Material You sample themes in the tokens are for checking designs only; don't ship them.
4. `canvas/` holds the screen mockups as HTML: phone screens, tablet/desktop two-pane layouts, theme variants, shared components and logo directions. Treat them as reference for layout and hierarchy, not code to port. Sample data (show names, episodes, dates) is invented.

## Reading the canvas files

- Each `*.dc.html` is one artboard. `canvas.json` gives each one's title and size. The orange `review1` note in it is the 27 Nov spec review that has been applied.
- Styling comes only from `theme.css` (CSS variables named after the Material 3 roles, e.g. `--primary`, `--surface-container-low`) plus inline layout. A colour in the markup maps 1:1 to a `MaterialTheme.colorScheme` role.
- `<dc-import name="X">` means "this screen uses the component in `X.dc.html`". The components map to composables: `EpisodeRow`, `MiniPlayer`, `NavBar` / `NavRail` (one `NavigationSuiteScaffold`), `NowPlayingPane`, `GenreListPane`, `GenreQueuePane`.
- Icons are Material Symbols Rounded, named by ligature (`play_arrow`, `history`).
- `title="…"` attributes hold the exact value behind every summarised one ("3 weeks ago" → "Thu 6 Nov 2026, 21:14"). In the app that's the long-press tooltip and the accessible label.

## Screen flow (from the flow-map whiteboard)

- **First run:** Import OPML (from Pocket Casts) → Confirm categories (one row per show, prefilled from Apple's categories) → Home.
- **Tabs:** Home (In Progress, then Up Next: one episode per category, stalest category first) · Genres (stalest first) → Genre queue (in progress, then shows stalest first), plus Manage categories from the Genres menu · Played (every play, newest first; re-listen, undo played) · Search (Podcast Index results) → Confirm category (same step as import).
- **Now Playing** is reached from any episode and held until played. Starting another episode leaves the current one in In Progress.
- **Open questions:** whether the four bottom tabs are right (assumed yes, with the mini-player above them), and where the per-show newest/oldest setting lives (assumed a Show page).

## Not designed yet

Played (History) screen, Import, Search, Subscribe, Settings › Appearance, the long-press example, swipe-to-mark-played, the final logo. Placeholders for some are listed in `canvas.json` without files.

## Live links

- Spec: https://claude.ai/artifact/3Frn7vpC4A8RsnnSNPNCsz
- Design system: https://claude.ai/artifact/EZHMHvdS5nqQch8gN5a4SK
- Screens canvas: https://claude.ai/artifact/HHGAxSc3KS3MG9c1xkhc11
- Flow map: https://claude.ai/artifact/AEBbsBe7TLKUWNa2QKms3Q
