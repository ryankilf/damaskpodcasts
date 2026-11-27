# Guardrails

Everything here runs as part of `./gradlew check`. A failure in any of them fails the build.

## Lint (detekt)

Config: [`config/detekt/detekt.yml`](../config/detekt/detekt.yml), on top of detekt's defaults. Custom rules: [`detekt-rules`](../detekt-rules), rule set `DamaskA11y`.

| Rule | Enforces |
|---|---|
| `CyclomaticComplexMethod` ≤ 10, `CognitiveComplexMethod` ≤ 15 | Small, readable functions (also nesting depth, method length, return count with guard clauses exempt) |
| `ContentDescriptionRequired` | `Image`/`Icon`/`AsyncImage` pass `contentDescription =` as a named argument; `null` only when written out |
| `ClickableRoleRequired` | `clickable`/`toggleable`/`selectable`/… pass a non-null `role =` |
| `MinimumTouchTarget` | No literal size under 48dp on an interactive modifier chain; Material's minimum size isn't disabled |
| `NoHardcodedDimensions` | No `.dp`/`.sp`/`Dp(…)` literals outside `ui.theme` or a `*Dimens*.kt` file (`0.dp` allowed) |
| `NoHardcodedColors` | No `Color(0x…)`/`Color.Red` outside `ui.theme`; use `MaterialTheme.colorScheme` roles |
| `DefaultFontsOnly` | Platform fonts only (`FontFamily.Default`) |
| `NoHardcodedUiStrings` | No string literals in `Text`, `contentDescription`, `onClickLabel`, …; previews and tests exempt |
| compose-rules (`Compose:` section) | ViewModels only at the top-level screen and never forwarded; hoisted state; modifier conventions |

`MinimumTouchTarget` can only see literal sizes, and literals are banned outside the theme, so touch targets also need a runtime check. Add Compose UI tests with accessibility checks when the first real screens land.

Suppress a rule only with `@Suppress("RuleName")` and a comment saying why.

## Design tokens

In `shared/ui/.../ui/theme`: `DamaskColors.kt` (copied from the design hand-off; re-copy when it changes), `Dimens.kt` (`Spacing`, `Radius`, `Sizes`), `Type.kt` (`DamaskTextStyles` mapped onto Material slots), and `Theme.kt` (brand scheme, or wallpaper colours on Android).

`ColorContrastTest` checks every foreground/background pair of the brand schemes against WCAG AA (4.5:1 for text, 3:1 for outlines), including every surface-container level.

## Architecture (Konsist)

[`architecture-tests`](../architecture-tests) checks:

- Each layer's imports (see [architecture](architecture.md)).
- ViewModels live in `presentation` and expose only `StateFlow`.
- `UiState` types are immutable data classes.
- `ui` does no formatting.

## Build checks

| Task | Fails when |
|---|---|
| `verifyNoComposeDependency` | `core` or `presentation` resolves any Compose artifact, even transitively |
| `licensee` | A shipping module pulls in anything not under Apache-2.0, MIT, BSD, ISC or CC0. When FFmpeg arrives, allow LGPL deliberately |
| `verifyNoBannedArtifacts` | GPL FFmpeg builds (including `-gpl` classifiers), `javacv-platform` or vlcj appear |
| `verifySqlDelightMigration` | A migration doesn't turn the previous schema snapshot into the current schema |
