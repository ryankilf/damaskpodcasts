package uk.co.kilfeder.damask.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Interactive modifiers must declare a non-null semantic `role` as a named argument, so screen
 * readers announce "Button", "Switch", "Checkbox", etc.
 *
 * Material components (`Button`, `IconButton`, `Switch`, ...) already set a role internally; this
 * targets hand-rolled interactive elements built from modifiers.
 */
class ClickableRoleRequired(config: Config) : Rule(
    config,
    "Interactive modifiers must declare a non-null `role = Role.X`.",
) {
    private val functions: List<String> by config(
        listOf("clickable", "combinedClickable", "selectable", "toggleable", "triStateToggleable"),
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeName ?: return
        if (name !in functions) return

        val role = expression.namedArgument(ROLE)
        val message = when {
            role == null -> "`$name` must pass `$ROLE = Role.X` as a named argument."
            role.getArgumentExpression().isNullLiteral() -> "`$name` must not pass `$ROLE = null`."
            else -> return
        }
        report(Finding(Entity.from(expression), message))
    }

    private companion object {
        const val ROLE = "role"
    }
}
