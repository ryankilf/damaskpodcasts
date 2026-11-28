package uk.co.kilfeder.damask.lint

import org.jetbrains.kotlin.psi.KtExpression
import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFile

/**
 * `.dp` / `.sp` / `.em` literals and `Dp(...)` / `TextUnit(...)` constructors are only allowed in
 * design-token files (the `ui.theme` package, or a `*Dimens*.kt` file). Everywhere else, use a named
 * token such as `Spacing.Inset`, so the same gap isn't written as 15, 16 and 17dp across screens.
 */
class NoHardcodedDimensions(config: Config) : Rule(
    config,
    "Dimensions must come from design tokens (Dimens.kt / ui.theme), not inline literals.",
) {
    private val units: List<String> by config(listOf("dp", "sp", "em"))
    private val constructors: List<String> by config(listOf("Dp", "TextUnit", "DpSize", "DpOffset"))
    private val allowedValues: List<String> by config(listOf("0"))
    private val allowedPackageSuffixes: List<String> by config(listOf(".theme"))
    private val allowedFileNamePattern: String by config(""".*Dimens.*\.kt""")

    override fun visitKtFile(file: KtFile) {
        if (!file.isTokenFile(allowedPackageSuffixes, Regex(allowedFileNamePattern))) super.visitKtFile(file)
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        val unit = expression.selectorExpression?.text ?: return
        if (unit !in units) return
        val literal = expression.receiverExpression as? KtConstantExpression ?: return
        if (literal.text.normalisedNumber() in allowedValues) return
        report(
            finding(expression),
        )
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeName ?: return
        if (name !in constructors) return
        if (expression.valueArguments.none { it.getArgumentExpression() is KtConstantExpression }) return
        report(
            finding(expression),
        )
    }

    private fun String.normalisedNumber(): String =
        numericLiteral()?.toBigDecimal()?.stripTrailingZeros()?.toPlainString() ?: this

    private fun finding(expression: KtExpression) =
        Finding(
            Entity.from(expression),
            "Inline dimension `${expression.text}`. Use a named token from Dimens.kt / ui.theme instead.",
        )
}
