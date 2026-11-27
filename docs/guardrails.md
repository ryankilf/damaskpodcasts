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

## Server

The search backend (`shared/api`, `server/*`, `deploy/`; see [server.md](server.md)) has its own rules on top of everything above. detekt and its complexity limits apply to the server modules too.

| Check | Enforces | Where |
|---|---|---|
| `allWarningsAsErrors` | Kotlin warnings fail the build in `shared/api` and every `server/*` module | Their `build.gradle.kts` |
| `verifyApiDependencies` | `shared/api` resolves nothing but the Kotlin stdlib and kotlinx-serialization | Root `build.gradle.kts` |
| Golden-JSON contract tests | Every `/v1` type encodes and decodes exactly as recorded; older payloads still decode; unknown fields are ignored. A failure means a breaking change: add a field with a default, or start `/v2` | `shared/api` `ContractTest` |
| Konsist `ServerBoundaryTest` | `server/*` imports nothing from the app except `shared/api`, and depends on no other `shared/` module; `shared/api` imports only `kotlin.*` and `kotlinx.serialization.*`; its `@Serializable` classes are data classes of `val`s; server main code never prints to stdout/stderr; only `MeiliSource` and `PodcastIndexSource` reach a search backend (routes go through `SearchSource`); Ktor's `CallLogging` is never installed (it logs query strings) | `architecture-tests` |
| Access-log test | A search's log output contains no query text, no query string and no raw `sub`; each request is logged once | `server/search` `AccessLogTest` |
| Error tests | 4xx/5xx bodies are `ApiError` JSON with no stack trace, exception text or class names | `server/search` `ErrorTest` |
| Auth tests | ES256 tokens: valid, expired, wrong issuer, wrong audience, missing `sub`, forged signature, unknown `kid` (refresh), rate-limited refresh, hourly refresh, anonymous accepted; private token accepted only when set, compared in constant time | `server/search` `AuthTest` |
| Meilisearch integration (Testcontainers) | Typo tolerance, `dedupeKey` collapse, `lang` and `hideExplicit` filters, 503 when Meilisearch is down, the search-only key can't write, the key's value is `HMAC(master, uid)` | `server/search`, `server/meili` |
| `licensee` + `verifyNoBannedArtifacts` | Same allow-list and bans as the apps, on `server:search` and `server:importer` | Root `build.gradle.kts` |

CI (`.github/workflows/`):

| Workflow | Runs when | Does |
|---|---|---|
| `server.yml` | `server/**`, `shared/api/**`, `deploy/**`, `architecture-tests/**` or shared build config change | The Gradle checks above; image pins; hadolint; compose validation; shellcheck; a `docker compose up` smoke test against the fixture index |
| `app.yml` | Anything outside `server/`, `deploy/` and docs | `./gradlew check` minus the server modules, on Linux (iOS targets skipped) |
| `security.yml` | Every push and pull request | Secret scan over the full history; allow-list with reasons in `.gitleaks.toml` |

Deploy lint, also runnable locally from the repo root:

- `sh .github/scripts/check-image-pins.sh`: every image has a specific version tag (never `latest`), and the Meilisearch tag in `deploy/docker-compose.yml` matches `meilisearch` in `gradle/libs.versions.toml`, which the Testcontainers tests use.
- `sh .github/scripts/hadolint.sh`: Dockerfile lint (warnings fail).
- `sh .github/scripts/secret-scan.sh`: the same secret scan as CI, over the working tree.

Dependabot (`.github/dependabot.yml`) proposes weekly updates for Gradle, Dockerfile base images, compose images and GitHub Actions.
