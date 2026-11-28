# Guardrails

Everything here runs as part of `./gradlew check`. A failure in any of them fails the build.

## Lint (detekt)

Config: [`config/detekt/detekt.yml`](../config/detekt/detekt.yml), on top of detekt's defaults. Custom rules: [`detekt-rules`](../detekt-rules), rule sets `DamaskA11y` and `DamaskSafety`. After changing a rule, run `./gradlew --stop` once: the Gradle daemon caches detekt's plugin classes.

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
| `UnsafeIntentLaunch` (rule set `DamaskSafety`) | Other apps are only opened through `rememberSafeIntent()`: no `startActivity…`, `openUri` or `LocalUriHandler` elsewhere. With no app to handle it (no mail client, no browser), the user gets a snackbar instead of a crash |
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

## Seeing the real app

`ScreenshotTour` (`shared/ui` jvmTest) drives the real `App` offscreen at phone size with a throwaway database. It taps through the age gate, imports an OPML file over the network and writes PNGs of each screen. It's opt-in, so `check` never runs it:

```sh
./gradlew :shared:ui:jvmTest --tests '*ScreenshotTour*' \
    -Pdamask.screenshots=llm-scrapbook/screens -Pdamask.opml=llm-scrapbook/podcasts_opml.xml
```

## Build checks

| Task | Fails when |
|---|---|
| `verifyNoComposeDependency` | `core` or `presentation` resolves any Compose artifact, even transitively |
| `licensee` | A shipping module pulls in anything not under Apache-2.0, MIT, BSD, ISC or CC0. When FFmpeg arrives, allow LGPL deliberately |
| `verifyNoBannedArtifacts` | GPL FFmpeg builds (including `-gpl` classifiers), `javacv-platform` or vlcj appear |
| `verifySqlDelightMigration` | A migration doesn't turn the previous schema snapshot into the current schema |

CI (`.github/workflows/`):

| Workflow | Runs when | Does |
|---|---|---|
| `app.yml` | Anything outside docs | `./gradlew check` on Linux (iOS targets skipped) |
| `security.yml` | Every push and pull request | Secret scan over the full history; allow-list with reasons in `.gitleaks.toml` |

Also runnable locally from the repo root: `sh .github/scripts/secret-scan.sh`, the same secret scan as CI, over the working tree.

Dependabot (`.github/dependabot.yml`) proposes weekly updates for Gradle and GitHub Actions.
