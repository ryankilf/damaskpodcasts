package uk.co.kilfeder.damask.lint

import org.jetbrains.kotlin.psi.KtExpression
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile

/**
 * Spec decision: UI code reads Material 3 colour roles only (`MaterialTheme.colorScheme.x`), never
 * hex values, so the brand scheme and Material You can be swapped at runtime.
 *
 * Reports `Color(...)` constructors and `Color.Red`-style constants outside the `ui.theme` package.
 * `Color.Transparent` and `Color.Unspecified` aren't colours and are allowed.
 */
class NoHardcodedColors(config: Config) : Rule(
    config,
    "Use MaterialTheme.colorScheme roles, never literal colours, outside ui.theme.",
) {
    private val allowedConstants: List<String> by config(listOf("Transparent", "Unspecified"))
    private val allowedPackageSuffixes: List<String> by config(listOf(".theme"))
    private val allowedFileNamePattern: String by config("""^$""")

    override fun visitKtFile(file: KtFile) {
        if (!file.isTokenFile(allowedPackageSuffixes, Regex(allowedFileNamePattern))) super.visitKtFile(file)
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        if (expression.calleeName != COLOR) return
        report(
            finding(expression),
        )
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        if (expression.receiverExpression.text != COLOR) return
        val selector = expression.selectorExpression ?: return
        // Color.hsv(...), Color.hsl(...) factories, or capitalised constants like Color.Red.
        val isFactory = selector is KtCallExpression
        val isConstant = !isFactory && selector.text.first().isUpperCase() && selector.text !in allowedConstants
        if (isFactory || isConstant) {
            report(
                finding(expression),
            )
        }
    }

    private companion object {
        const val COLOR = "Color"
    }

    private fun finding(expression: KtExpression) =
        Finding(Entity.from(expression), "Literal colour `${expression.text}`. Use a `MaterialTheme.colorScheme` role.")
}
