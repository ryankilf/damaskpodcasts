package uk.co.kilfeder.damask.lint

import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

/** Project rules against crashes and unsafe patterns. Configured under `DamaskSafety:` in detekt.yml. */
class DamaskSafetyRuleSetProvider : RuleSetProvider {
    override val ruleSetId: RuleSetId = RuleSetId("DamaskSafety")

    override fun instance(): RuleSet = RuleSet(ruleSetId, listOf(::UnsafeIntentLaunch))
}
