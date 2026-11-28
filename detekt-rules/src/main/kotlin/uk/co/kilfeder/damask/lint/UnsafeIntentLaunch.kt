package uk.co.kilfeder.damask.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtImportDirective
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType

/**
 * Handing something to another app (a link, an email address, a share) crashes the app when the
 * device has nothing that can handle it: no mail client, no browser in a work profile. Every
 * launch goes through `rememberSafeIntent()` (shared/ui), which shows a snackbar instead.
 *
 * Reports calls to the launching functions ([launchFunctions]) and any use of [handlers]
 * (`LocalUriHandler`). The safe wrapper itself suppresses this rule, with a comment saying why.
 */
class UnsafeIntentLaunch(config: Config) : Rule(
    config,
    "Open other apps through rememberSafeIntent(), which can't crash when no app can handle it.",
) {
    private val launchFunctions: List<String> by config(
        listOf(
            "startActivity",
            "startActivities",
            "startActivityForResult",
            "startActivityIfNeeded",
            "startIntentSender",
            "openUri",
        ),
    )
    private val handlers: List<String> by config(listOf("LocalUriHandler"))

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeName ?: return
        if (name !in launchFunctions) return
        val message = "`$name(...)` can crash with no app to handle it: use rememberSafeIntent()."
        report(Finding(Entity.from(expression), message))
    }

    override fun visitReferenceExpression(expression: KtReferenceExpression) {
        super.visitReferenceExpression(expression)
        if (expression !is KtNameReferenceExpression) return
        if (expression.getReferencedName() !in handlers) return
        if (expression.getParentOfType<KtImportDirective>(strict = true) != null) return
        val message = "`${expression.getReferencedName()}` opens other apps unguarded: use rememberSafeIntent()."
        report(Finding(Entity.from(expression), message))
    }
}
