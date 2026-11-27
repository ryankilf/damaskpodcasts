package uk.co.kilfeder.damask.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.container.KoScope
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import kotlin.test.Test
import kotlin.test.assertTrue as kAssertTrue

/**
 * Rules the compiler can't express on its own. The Gradle module graph already stops
 * :shared:presentation from *linking* Compose; these keep the intent explicit and catch leaks via
 * transitive or `api` dependencies.
 *
 * Goal: a future SwiftUI layer can do everything a Compose screen can, because both may only talk
 * to :shared:presentation.
 */
class LayerBoundaryTest {

    @Test
    fun `core has no UI, lifecycle or upper-layer imports`() =
        scope(CORE).files.assertFalse(testName = "core imports") { file ->
            file.hasImport { it.name.startsWithAny(CORE_FORBIDDEN) }
        }

    @Test
    fun `presentation never imports Compose, Android or the ui layer`() =
        scope(PRESENTATION).files.assertFalse(testName = "presentation imports") { file ->
            file.hasImport { it.name.startsWithAny(PRESENTATION_FORBIDDEN) }
        }

    @Test
    fun `ui never touches core directly`() =
        scope(UI).files.assertFalse(testName = "ui imports core") { file ->
            file.hasImport { it.name.startsWith(CORE_PKG) }
        }

    @Test
    fun `ui does no formatting (thin UI rule)`() =
        // Dates, durations, counts and percentages are formatted in presentation and arrive in
        // UiState as ready-to-show strings (plus the exact value for long press / a11y labels),
        // so a SwiftUI screen renders exactly the same text.
        scope(UI).files
            .filterNot { it.sourceSetName.endsWith("Test") }
            .assertFalse(testName = "ui formatting") { file ->
                file.hasImport { it.name.startsWithAny(FORMATTING_APIS) } || FORMAT_CALL.containsMatchIn(file.text)
            }

    @Test
    fun `apps only depend on the ui layer`() =
        (scope(ANDROID_APP) + scope(DESKTOP_APP)).files.assertFalse(testName = "app imports") { file ->
            file.hasImport { it.name.startsWithAny(listOf(CORE_PKG)) }
        }

    @Test
    fun `ViewModels live in presentation`() =
        Konsist.scopeFromProject().classes()
            .filter { it.name.endsWith("ViewModel") }
            .assertTrue(testName = "ViewModel location") { it.residesInModule(PRESENTATION) }

    @Test
    fun `ViewModels expose state only as StateFlow`() =
        scope(PRESENTATION).classes()
            .filter { it.name.endsWith("ViewModel") }
            .flatMap { it.properties() }
            .filter { it.hasPublicOrDefaultModifier }
            .assertTrue(testName = "ViewModel public state") {
                it.type?.name?.startsWith("StateFlow") == true
            }

    @Test
    fun `UiState types are immutable data classes`() =
        scope(PRESENTATION).classes()
            .filter { it.name.endsWith("UiState") }
            .assertTrue(testName = "UiState immutability") { cls ->
                cls.hasDataModifier && cls.properties().none { it.isVar }
            }

    private fun scope(module: String): KoScope = Konsist.scopeFromModule(module).also {
        // Guard against a renamed module silently turning every rule into a vacuous pass.
        kAssertTrue(it.files.isNotEmpty(), "No sources found for module '$module'")
    }

    private fun String.startsWithAny(prefixes: List<String>) = prefixes.any { startsWith(it) }

    private companion object {
        const val CORE = "shared/core"
        const val PRESENTATION = "shared/presentation"
        const val UI = "shared/ui"
        const val ANDROID_APP = "androidApp"
        const val DESKTOP_APP = "desktopApp"

        const val CORE_PKG = "uk.co.kilfeder.damask.podcasts.core"
        const val PRESENTATION_PKG = "uk.co.kilfeder.damask.podcasts.presentation"
        const val UI_PKG = "uk.co.kilfeder.damask.podcasts.ui"

        val FORMATTING_APIS = listOf(
            "java.text",
            "java.time.format",
            "kotlinx.datetime.format",
            "android.text.format",
            "android.icu.text",
            "platform.Foundation.NSDateFormatter",
            "platform.Foundation.NSNumberFormatter",
            "platform.Foundation.NSDateComponentsFormatter",
            "platform.Foundation.NSRelativeDateTimeFormatter",
        )
        val FORMAT_CALL = Regex("""\.format\(|\bformat\(\s*"|DateUtils\.""")

        val COMPOSE = listOf("androidx.compose", "org.jetbrains.compose")

        // core's platform actuals may use android.os / platform.UIKit.UIDevice etc., but no UI toolkit.
        val CORE_FORBIDDEN = COMPOSE + listOf(
            "androidx.lifecycle",
            "android.view",
            "android.widget",
            "androidx.activity",
            PRESENTATION_PKG,
            UI_PKG,
        )

        // presentation must be consumable unchanged by SwiftUI: no Compose, no Android, no UIKit.
        val PRESENTATION_FORBIDDEN = COMPOSE + listOf(
            "android.",
            "androidx.lifecycle.viewmodel.compose",
            "androidx.lifecycle.compose",
            "androidx.activity",
            "platform.UIKit",
            UI_PKG,
        )
    }
}
