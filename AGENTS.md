# Working on Damask

Notes for coding agents (and people) changing this repo. The *what* and *why* live in [docs/](docs/):
[spec](docs/spec.md), [architecture](docs/architecture.md), [decisions](docs/decisions.md),
[guardrails](docs/guardrails.md), [playback](docs/playback.md).

## Before you commit

- `./gradlew check` must pass. Commit only when it exits 0. It runs the tests, detekt with the
  project's own rules, compose-rules, Konsist layering tests and Licensee.
- After changing anything in `detekt-rules`, run `./gradlew --stop` first: the daemon caches the plugin.
- Update `docs/` in the same commit as the code: the spec for behaviour, `decisions.md` for the why.
- Schema changes need `migrations/<n>.sqm`, then `./gradlew :shared:core:generateCommonMainDamaskDatabaseSchema`.

## Layers

`core` (rules, database, feeds) ← `presentation` (ViewModels, UiState, all formatting; no Compose)
← `ui` (Compose; never imports `core`) ← the apps. The checks enforce this; don't work around them.

## Keep it DRY

Wet code is a bug waiting to happen: two copies drift, and one gets the fix. Before writing
something, look for the thing that already does it, and reuse or extend that.

- **Search first.** Before adding a composable, a mapping, a helper or a string, grep for an
  existing one: `ui/components` for UI pieces, `ui/screens/Common.kt` for shared screen states,
  `presentation/format` for formatting, `core` for rules. If something close exists, extend it
  with a parameter rather than copying it.
- **Same interaction, same component.** If two screens let you do the same thing (pick a genre,
  play an episode, show an empty state, highlight a selected row), they use one component. For
  example, the genre picker is `CategorySheet` wherever a show's genre is chosen.
- **Same data, same model.** Don't add a second `UiState` type for something that already has one
  (`CategoryOptionUi`, `EpisodeRowUi`, `Summarised`, `ArtworkFallback`).
- **The second copy is the signal.** The second time you write the same thing, pull it out. Don't
  wait for a third.
- **Constants and tokens live once.** Sizes, colours and timings in `ui/theme`; strings in
  `strings.xml`; durations and date maths in `presentation/format` or `core`.
- **Splitting for detekt's limits (11 functions per file, 120-character lines) isn't
  duplication.** Split by responsibility into a new file; never copy to get under a limit.

## Keep it SOLID-ish

- One job per class. ViewModels shape state and forward intents; stores write; `Rotation` orders;
  engines only play.
- Depend on the interface where a platform differs (`AudioEngine`, `DriverFactory`), not on the
  implementation.
- Composables take plain state and callbacks. Only a screen's top composable holds a ViewModel.

## House style

- Copy is plain, friendly English. Never name Pocket Casts (or any other app) in the product.
- No hard-coded dp, colours or user-visible strings outside `ui/theme` and resources; the lints fail them.
- Every interactive element has a role and a label, and a 48dp touch target.
