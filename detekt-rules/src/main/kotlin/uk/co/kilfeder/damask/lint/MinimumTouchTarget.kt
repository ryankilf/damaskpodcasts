package uk.co.kilfeder.damask.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Static check for touch targets smaller than [minSizeDp] (48dp per Material / Android guidance;
 * iOS HIG asks for 44pt, so 48 covers both).
 *
 * Reports:
 * - a modifier chain containing an interactive modifier (`clickable`, `toggleable`, ...) that also
 *   constrains its size with a literal below the minimum, e.g. `Modifier.size(24.dp).clickable {}`.
 * - disabling Material's minimum-size enforcement via `LocalMinimumInteractiveComponentSize provides`
 *   (`Dp.Unspecified` or a small literal) or `LocalMinimumInteractiveComponentEnforcement provides false`.
 *
 * Non-literal sizes (tokens, variables) can't be evaluated statically; runtime accessibility checks
 * in UI tests cover those.
 */
class MinimumTouchTarget(config: Config) : Rule(
    config,
    "Interactive elements must be at least 48dp in each dimension.",
) {
    private val minSizeDp: Int by config(DEFAULT_MIN_DP)

    private val interactiveFunctions: List<String> by config(INTERACTIVE_MODIFIERS)

    private val sizeFunctions: List<String> by config(
        listOf("size", "requiredSize", "width", "requiredWidth", "height", "requiredHeight"),
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeName ?: return
        if (name !in interactiveFunctions) return

        expression.chainRoot().chainCalls()
            .filter { it.calleeName in sizeFunctions }
            .forEach { sizeCall ->
                val tooSmall = sizeCall.valueArguments
                    .mapNotNull { it.getArgumentExpression().dpLiteralValue() }
                    .filter { it < minSizeDp }
                if (tooSmall.isNotEmpty()) {
                    report(
                        Finding(
                            Entity.from(sizeCall),
                            "`${sizeCall.calleeName}(${tooSmall.joinToString { it.asDp() }})` on a `$name` " +
                                "element is below the ${minSizeDp}dp minimum touch target. Keep the visual " +
                                "small but pad the touch area (e.g. `Modifier.minimumInteractiveComponentSize()`).",
                        ),
                    )
                }
            }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression) {
        super.visitBinaryExpression(expression)
        if (expression.operationReference.text != "provides") return
        val local = expression.left?.text?.substringAfterLast('.') ?: return
        val value = expression.right

        val disables = when (local) {
            "LocalMinimumInteractiveComponentSize" ->
                value?.text?.endsWith("Unspecified") == true ||
                    (value.dpLiteralValue()?.let { it < minSizeDp } ?: false)
            "LocalMinimumInteractiveComponentEnforcement" -> value?.text == "false"
            else -> false
        }
        if (disables) {
            report(
                Finding(
                    Entity.from(expression),
                    "Don't disable or shrink Material's ${minSizeDp}dp minimum interactive size.",
                ),
            )
        }
    }

    private fun Double.asDp(): String = "${toBigDecimal().stripTrailingZeros().toPlainString()}.dp"

    private companion object {
        const val DEFAULT_MIN_DP = 48
    }
}
