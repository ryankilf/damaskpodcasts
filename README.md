Damask: a podcast player that rotates genres. Kotlin Multiplatform, targeting Android, iOS and desktop (JVM).

* [/iosApp](./iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose Multiplatform,
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.

* [/shared](./shared) is split into three layers. Dependencies only point downwards:
  - [core](./shared/core): feeds, DB, sync, rotation logic. No UI dependencies at all.
  - [presentation](./shared/presentation): ViewModels, `UiState`, intents, navigation state.
    **No Compose dependency.** A future SwiftUI layer consumes this module exactly as Compose does.
  - [ui](./shared/ui): Compose Multiplatform screens. Depends on `presentation`, never on `core`.
    For v1 it also builds the `Shared` iOS framework, which exports `presentation`.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and options:

- Android app: `./gradlew :androidApp:assembleDebug`
- Desktop app:
  - Hot reload: `./gradlew :desktopApp:hotRun --auto`
  - Standard run: `./gradlew :desktopApp:run`
- iOS app: open the [/iosApp](./iosApp) directory in Xcode and run it from there.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Everything (tests, lint, architecture rules): `./gradlew check`
- Per module, e.g. `./gradlew :shared:presentation:jvmTest`, `:shared:ui:testAndroidHostTest`, `:shared:core:iosSimulatorArm64Test`

### How it works

See [docs/](./docs/README.md): architecture, the rotation rules, the database, feeds, the lint and architecture guardrails, and every place the code interprets the spec. The product spec and design hand-off are in [spec-and-design/](./spec-and-design).

---|---|---|
| Cyclomatic complexity ≤ 10, cognitive complexity ≤ 15 (plus nesting depth, method length) | [config/detekt/detekt.yml](./config/detekt/detekt.yml) | detekt (`./gradlew detekt`) |
| `Image`/`Icon`/`AsyncImage` must pass `contentDescription =` as a named argument (`null` allowed, but only when written out) | `ContentDescriptionRequired` | [:detekt-rules](./detekt-rules) |
| `clickable`/`toggleable`/`selectable`/... must pass a non-null `role =` | `ClickableRoleRequired` | [:detekt-rules](./detekt-rules) |
| No literal size below 48dp on an interactive modifier chain; no disabling `LocalMinimumInteractiveComponentSize` | `MinimumTouchTarget` | [:detekt-rules](./detekt-rules) |
| No inline `.dp`/`.sp`/`Dp(...)` outside `ui.theme` or a `*Dimens*.kt` file (`0.dp` allowed). Use `Spacing`, `Radius`, `Sizes`, `DamaskTextStyles` | `NoHardcodedDimensions` | [:detekt-rules](./detekt-rules) |
| No `Color(0x…)`/`Color.Red` outside `ui.theme`: read `MaterialTheme.colorScheme` roles | `NoHardcodedColors` | [:detekt-rules](./detekt-rules) |
| Platform fonts only: no `FontFamily(...)`, `Font(...)` or `FontFamily.Serif` etc. | `DefaultFontsOnly` | [:detekt-rules](./detekt-rules) |
| No string literals in `Text(...)`, `contentDescription`, `onClickLabel`, ... (previews and tests exempt) | `NoHardcodedUiStrings` | [:detekt-rules](./detekt-rules) |
| Every brand colour-scheme pair meets WCAG AA (4.5:1 for text, 3:1 for outlines) | [ColorContrastTest](./shared/ui/src/commonTest/kotlin/uk/co/kilfeder/damask/podcasts/ui/theme/ColorContrastTest.kt) | unit test |
| Compose hygiene: ViewModels only at the top-level screen, never forwarded; hoisted state; modifiers | `Compose:` section | [compose-rules](https://mrmans0n.github.io/compose-rules/) |
| `core`/`presentation` never resolve Compose, even transitively | `verifyNoComposeDependency` | root [build.gradle.kts](./build.gradle.kts) |
| Only permissive licences ship (Apache-2.0, MIT, BSD, ISC, CC0); GPL FFmpeg builds, `javacv-platform` and vlcj are banned by name | `licensee`, `verifyNoBannedArtifacts` | [Licensee](https://github.com/cashapp/licensee), root [build.gradle.kts](./build.gradle.kts) |
| `ui` does no date/number formatting: it arrives preformatted in `UiState` | [LayerBoundaryTest](./architecture-tests/src/test/kotlin/uk/co/kilfeder/damask/architecture/LayerBoundaryTest.kt) | Konsist |
| Layer imports (presentation has no Compose/Android/UIKit; ui never imports core); ViewModels live in presentation and expose state only as `StateFlow`; `UiState` is an immutable data class | [LayerBoundaryTest](./architecture-tests/src/test/kotlin/uk/co/kilfeder/damask/architecture/LayerBoundaryTest.kt) | Konsist ([:architecture-tests](./architecture-tests)) |

Design tokens live in [ui/theme](./shared/ui/src/commonMain/kotlin/uk/co/kilfeder/damask/podcasts/ui/theme): `DamaskColors.kt` is copied from `spec-and-design/damask-design/compose/` (re-copy when the design system changes), and `Dimens.kt`/`Type.kt` mirror `tokens.json`.

Add your own `Image`/`Icon` wrappers (e.g. `PodcastArtwork`) to `ContentDescriptionRequired.functions` in `detekt.yml`.
To suppress a rule in a specific place, annotate it with `@Suppress("RuleName")` and add a comment explaining why.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…