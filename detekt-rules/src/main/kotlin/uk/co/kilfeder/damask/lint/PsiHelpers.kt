package uk.co.kilfeder.damask.lint

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.kotlin.psi.psiUtil.parents

internal val KtCallExpression.calleeName: String?
    get() = calleeExpression?.text

internal fun KtCallExpression.namedArgument(name: String): KtValueArgument? =
    valueArguments.firstOrNull { it.getArgumentName()?.asName?.asString() == name }

internal fun KtExpression?.isNullLiteral(): Boolean = this?.text == "null"

/** Modifier functions that make something tappable; the defaults for the a11y rules' config. */
internal val INTERACTIVE_MODIFIERS =
    listOf("clickable", "combinedClickable", "selectable", "toggleable", "triStateToggleable")

/** A Kotlin number literal's value (`24`, `24f`, `1_000`, `24.0`), or null if it isn't one. */
internal fun String.numericLiteral(): Double? = removeSuffix("f").removeSuffix("F").replace("_", "").toDoubleOrNull()

/**
 * Returns the numeric value of a `24.dp` / `24f.dp` / `24.0.dp` literal, or null for anything else
 * (variables, theme tokens, arithmetic) which can't be checked statically.
 */
internal fun KtExpression?.dpLiteralValue(): Double? {
    val qualified = this as? KtDotQualifiedExpression ?: return null
    if (qualified.selectorExpression?.text != "dp") return null
    val constant = qualified.receiverExpression as? KtConstantExpression ?: return null
    return constant.text.numericLiteral()
}

/** The outermost expression of a `Modifier.a().b().c()` chain containing [this]. */
internal fun KtCallExpression.chainRoot(): KtExpression {
    var current: KtExpression = this
    while (true) {
        val parent = current.parent as? KtQualifiedExpression ?: return current
        current = parent
    }
}

/** All calls that form the links of a qualified chain, e.g. `Modifier.size(1.dp).clickable {}`. */
internal fun KtExpression.chainCalls(): List<KtCallExpression> = when (this) {
    is KtQualifiedExpression ->
        receiverExpression.chainCalls() + listOfNotNull(selectorExpression as? KtCallExpression)
    is KtCallExpression -> listOf(this)
    else -> emptyList()
}

/** Package suffix / file-name pattern identifying design-token files (theme, Dimens.kt, ...). */
internal fun KtFile.isTokenFile(allowedPackageSuffixes: List<String>, allowedFileNamePattern: Regex): Boolean {
    val pkg = packageFqName.asString()
    return allowedPackageSuffixes.any { pkg.endsWith(it) } || allowedFileNamePattern.matches(name)
}

/** True if this element sits inside a function annotated with `@Preview` (sample data is fine there). */
internal fun PsiElement.isInsidePreview(): Boolean =
    parents.filterIsInstance<KtNamedFunction>().any { fn ->
        fn.annotationEntries.any { it.shortName?.asString()?.endsWith("Preview") == true }
    }
