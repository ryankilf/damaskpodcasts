package uk.co.kilfeder.damask.lint

import dev.detekt.api.Config
import dev.detekt.api.Entity
import dev.detekt.api.Finding
import dev.detekt.api.Rule
import dev.detekt.api.config
import org.jetbrains.kotlin.psi.KtCallExpression

/**
 * Image-like composables must pass `contentDescription` as a *named* argument.
 *
 * `contentDescription = null` is allowed, but only when written out explicitly so it reads as a
 * deliberate "this is decorative" decision. Positional arguments (`Image(painter, null)`) and
 * omitted arguments are reported.
 *
 * Add project wrappers (e.g. `PodcastArtwork`) to `functions` in detekt.yml.
 */
class ContentDescriptionRequired(config: Config) : Rule(
    config,
    "Images and icons must state `contentDescription =` explicitly (null only if decorative).",
) {
    private val functions: List<String> by config(
        listOf("Image", "Icon", "AsyncImage", "SubcomposeAsyncImage", "KamelImage"),
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val name = expression.calleeName ?: return
        if (name !in functions) return
        if (expression.namedArgument(CONTENT_DESCRIPTION) != null) return

        report(
            Finding(
                Entity.from(expression),
                "`$name` must pass `$CONTENT_DESCRIPTION = ...` as a named argument. " +
                    "Use `$CONTENT_DESCRIPTION = null` only for purely decorative images.",
            ),
        )
    }

    private companion object {
        const val CONTENT_DESCRIPTION = "contentDescription"
    }
}
