package uk.co.kilfeder.damask.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression

/**
 * Spec decision: platform default fonts only (`FontFamily.Default`). No bundled typefaces and no
 * generic families (Serif, Monospace, ...). Applies everywhere, including the theme.
 */
class DefaultFontsOnly(config: Config) : Rule(
    config,
    "Only FontFamily.Default is allowed; no bundled or generic font families.",
) {
    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeName ?: return
        if (name != "FontFamily" && name != "Font") return
        report(Finding(Entity.from(expression), "`$name(...)`: use the platform font (`FontFamily.Default`)."))
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        if (expression.receiverExpression.text != "FontFamily") return
        val family = expression.selectorExpression?.text ?: return
        if (family == "Default") return
        report(Finding(Entity.from(expression), "`FontFamily.$family`: use `FontFamily.Default`."))
    }
}
