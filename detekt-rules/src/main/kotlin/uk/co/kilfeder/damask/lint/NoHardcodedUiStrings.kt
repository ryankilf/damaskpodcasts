package uk.co.kilfeder.damask.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtStringTemplateExpression

/**
 * User-visible text must come from string resources, so it can be translated and later reached
 * from Swift. Reports string literals (including templates with literal text, e.g. `"Played $n"`)
 * passed as:
 * - the first positional argument, or `text =`, of [textFunctions] (`Text`, ...);
 * - any argument named in [accessibilityArguments] (`contentDescription`, `onClickLabel`, ...).
 *
 * Pure interpolations like `"$title"` are allowed. `@Preview` functions are skipped.
 */
class NoHardcodedUiStrings(config: Config) : Rule(
    config,
    "User-visible and accessibility strings must come from string resources.",
) {
    private val textFunctions: List<String> by config(listOf("Text", "BasicText"))
    private val accessibilityArguments: List<String> by config(
        listOf("contentDescription", "onClickLabel", "onLongClickLabel", "stateDescription"),
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeName ?: return
        if (expression.isInsidePreview()) return

        expression.valueArguments.forEachIndexed { index, argument ->
            val argName = argument.getArgumentName()?.asName?.asString()
            val isText = name in textFunctions && (argName == "text" || (argName == null && index == 0))
            val isA11y = argName in accessibilityArguments
            if ((isText || isA11y) && argument.getArgumentExpression().hasLiteralText()) {
                report(
                    Finding(
                        Entity.from(argument),
                        "Hard-coded string in `$name(${argName ?: "text"} = ...)`. Use a string resource.",
                    ),
                )
            }
        }
    }

    private fun KtExpression?.hasLiteralText(): Boolean {
        val template = this as? KtStringTemplateExpression ?: return false
        return template.entries.any { it is KtLiteralStringTemplateEntry && it.text.isNotBlank() }
    }
}
