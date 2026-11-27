package uk.co.kilfeder.damask.podcasts.core.sync

import uk.co.kilfeder.damask.podcasts.core.model.Hlc
import uk.co.kilfeder.damask.podcasts.core.model.ShowId

/** The fields needed to detect duplicate subscriptions. */
data class ShowIdentity(
    val id: ShowId,
    val feedUrlNorm: String,
    /** The show row's HLC; the earliest in a duplicate group becomes the root. */
    val hlc: Hlc,
    val subscribed: Boolean = true,
)

/**
 * Duplicate-show resolution (spec › Sync › Duplicate shows). Every device computes the same root
 * from the same rows, so the result is deterministic: root = earliest HLC, ties broken by ID.
 * Aliases always point straight at the root, never at another alias.
 */
object ShowAliases {

    /** `alias_id -> canonical_id` for every subscribed show that duplicates another's feed. */
    fun detect(shows: List<ShowIdentity>): Map<ShowId, ShowId> = shows
        .filter { it.subscribed }
        .groupBy { it.feedUrlNorm }
        .values
        .filter { it.size > 1 }
        .flatMap { group ->
            val root = group.minWith(compareBy<ShowIdentity> { it.hlc }.thenBy { it.id.value })
            group.filter { it.id != root.id }.map { it.id to root.id }
        }
        .toMap()

    /**
     * Re-flattens alias rows after a pull so each points straight at its root. Chains (a -> b -> c)
     * become a -> c; cycles, which shouldn't occur, resolve to the smallest ID in the cycle.
     */
    fun flatten(aliases: Map<ShowId, ShowId>): Map<ShowId, ShowId> =
        aliases.keys.associateWith { rootOf(it, aliases) }.filter { (alias, root) -> alias != root }

    /** canonical(show_id) = COALESCE(show_alias.canonical_id, show_id), given flattened aliases. */
    fun canonical(id: ShowId, flattened: Map<ShowId, ShowId>): ShowId = flattened[id] ?: id

    private fun rootOf(start: ShowId, aliases: Map<ShowId, ShowId>): ShowId {
        val seen = linkedSetOf(start)
        var current = start
        while (true) {
            val next = aliases[current] ?: return current
            if (!seen.add(next)) return seen.dropWhile { it != next }.minBy { it.value }
            current = next
        }
    }
}
