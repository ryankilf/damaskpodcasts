package uk.co.kilfeder.damask.lint

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/** Project accessibility and design-system rules for Compose UI. Configured under `DamaskA11y:` in detekt.yml. */
class DamaskA11yRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("DamaskA11y")

    override fun instance(): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            ::ContentDescriptionRequired,
            ::ClickableRoleRequired,
            ::MinimumTouchTarget,
            ::NoHardcodedDimensions,
            ::NoHardcodedColors,
            ::DefaultFontsOnly,
            ::NoHardcodedUiStrings,
        ),
    )
}
