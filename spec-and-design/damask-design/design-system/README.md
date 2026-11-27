# Damask — design system

A podcast player for Android first (Jetpack Compose, Material 3), then desktop and iOS. The whole app follows one rule: **unfinished episodes first, then whatever you've neglected longest.** The design exists to make that rule visible. The app is called **Damask**, after the figured linen Belfast was famous for: one thread, two finishes, a pattern you only notice when the light catches it. The developer name is Linenopolis.

## Colour: Material You, with a default

Colour is defined as **Material 3 colour roles**, named exactly as Compose's `MaterialTheme.colorScheme` names them (`primary`, `primaryContainer`, `surfaceContainer`, `onSurfaceVariant`…; kebab-case here as CSS variables). Screens use roles only, never hex values, so any scheme can be swapped in.

There are two sources for the roles:

| Scheme | When | Source |
| --- | --- | --- |
| **Default** (light, dark) | iOS, desktop, Android 11 and below, and any Android user who turns Material You off | The brand: teal `#0f766e` as primary, indigo as tertiary, cool neutrals |
| **Material You** | Android 12+ with "Use wallpaper colours" on | The wallpaper, via `dynamicLightColorScheme` / `dynamicDarkColorScheme` |

Material You is a user option, not forced: Settings › Appearance › "Use wallpaper colours", **on by default** where the platform supports it, hidden where it doesn't. Theme (System / Light / Dark) is a separate setting. The Material You themes in this system are samples (a blue and a green wallpaper) for checking that designs hold up; on a device the wallpaper supplies them.

Because hues change, meaning lives in the role, not the colour:

- `primary` means **unfinished**: progress fills, the play/resume button, the In Progress header. Never decoration, never a link colour.
- `primary-container` is the In Progress section's background.
- `tertiary` / `tertiary-container` mean **category**: genre chips and the "longest unplayed" marker, always beside the genre's name.
- `error` is for destructive actions only.
- Everything else is neutral: `surface` behind screens, `surface-container-low` rows, `surface-container` navigation bar and mini-player, `surface-container-high` sheets and menus, `outline-variant` dividers.

No warning hues in the brand: red, orange and amber read as danger, so `error` is the only red on screen and the brand stays teal. Every text role passes 4.5:1 on its intended background in all six themes (checked, not eyeballed). The lowest: `primary` on `primary-container`, 5.0:1.

## Principles

1. **Unfinished is the loudest thing on screen.** Only `primary` gets to be loud.
2. **The order is the feature.** Lists are already sorted stalest-first, so screens show order without explaining it: no sort controls in v1, no rank numbers.
3. **Artwork carries the colour; chrome stays tonal.** Show art is the only full-colour thing in a list.
4. **One tap to resume.** Every in-progress row has its saved position and a play button on the row itself.

## Content

Plain, short, dry. Sentence case everywhere. Times as the listener thinks of them: "32 min left", not "00:32:10". Dates relative up to a week ("Tuesday"), then "12 Nov".

- Do: "Resume", "Mark played", "Last played 3 weeks ago".
- Don't: exclamation marks, "Oops", streaks, emoji, "Discover".

**Every summarised value can be expanded to the exact one.** Relative dates, "32 min left", "about an hour", rounded counts and percentages are shorthand, never the only form. Long press on touch, hover or focus on desktop, shows the exact value in a plain tooltip: "Last played Thu 6 Nov 2026, 21:14", "32:04 left of 54:10 (41%)". Screen readers get the exact value as the accessible label. No summary ships without this.

## Type, space, shape

- **Type.** The platform's own font, never a bundled one: Roboto (or the phone maker's system font) on Android, San Francisco on iOS and macOS, Segoe UI on Windows, the desktop default on Linux. In Compose that means `FontFamily.Default`; on the canvas the stack is `system-ui`. `title` for episode titles (two lines, then truncate), `body-sm` in `on-surface-variant` for show name and date, `time` with tabular figures for durations and time left. `display` only on Now Playing.
- **Space.** `space-4` screen inset and row padding, `space-2` between artwork and text, `space-6` between sections. Touch targets at least 48dp.
- **Shape.** `radius-sm` on row artwork and chips, `radius-md` on cards, sheets, the mini-player and Now Playing artwork, `radius-full` on progress bars and the play button. Progress bars are 4dp tall (half of `space-2`).

## Pattern

Damask patterns are tone on tone. When the app uses one (later: Now Playing backdrop, onboarding, empty states), draw it in `surface-container-low` on `surface` so it follows Material You and never competes with content.

The one place it appears from v1: **filled play and resume buttons**. The `primary` fill carries the diaper pattern in `on-primary` at 16% opacity, about 9dp pitch on a 36–48dp button and 14dp on the 80dp Now Playing button. It's drawn as a mask, so it follows whichever scheme is active. Only filled play/pause buttons get it; nothing else that is `primary` does, so the pattern becomes the mark of "press here to listen". In Compose, draw it as a tiled `Brush` or a cached `ImageBitmap` behind the icon.

## Iconography

Material Symbols Rounded, 24dp, weight 400, in `on-surface` or `on-surface-variant`; filled only for the play/pause button. No custom icons yet. The logo is in progress; until it lands, set "Damask" in the platform font at semibold.

## Still open

The logo and app icon (in progress); the bottom-tab set (see the [screen flow board](https://claude.ai/artifact/AEBbsBe7TLKUWNa2QKms3Q)). Spec: [Podcast App — v1 Spec](https://claude.ai/artifact/3Frn7vpC4A8RsnnSNPNCsz).
