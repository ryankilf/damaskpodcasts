package uk.co.kilfeder.damask.server.search

import uk.co.kilfeder.damask.server.meili.PodcastIndex
import kotlin.math.abs
import uk.co.kilfeder.damask.server.meili.ScoredHit

/**
 * Merges the region's results (list A, e.g. en-GB) with the rest of the language (list B) in
 * Meilisearch's own rule order, boosting A's `rankScore`.
 *
 * Each hit's sort key is its per-rule scores in [PodcastIndex.RANKING_RULES] order (words, typo,
 * proximity, `rankScore` × boost, attribute, exactness), compared rule by rule, never summed. The
 * boost multiplies one rule by a constant for all of A, so A's internal order is unchanged, and a
 * two-way merge of the two already-sorted lists is exactly the order a sort of their union would
 * give (RegionMergeTest checks that on random inputs). The boost only reorders shows that tie on
 * words, typo and proximity, and only where the region's show is within the boost of the other on
 * `rankScore`: it nudges, it doesn't dominate.
 */
internal object RegionMerge {
    /**
     * Float tolerance per rule: Meilisearch's scores are ratios of small integers (e.g. 2/3), so
     * two equal ranks can differ by rounding. Larger differences are real.
     */
    const val EPSILON = 1e-9
    private const val BOOSTED_RULE = "rankScore:desc"

    /** The sort key: one number per ranking rule, higher is better. */
    fun key(hit: ScoredHit, boost: Double): DoubleArray = PodcastIndex.RANKING_RULES.map { rule ->
        val detail = hit.details[rule]
        if (rule.endsWith(":desc")) {
            (detail?.value ?: 0.0) * (if (rule == BOOSTED_RULE) boost else 1.0)
        } else {
            detail?.score ?: 0.0
        }
    }.toDoubleArray()

    /** Negative when [a] ranks before [b]. Lexicographic, rule by rule, with [EPSILON]. */
    fun compare(a: DoubleArray, b: DoubleArray): Int {
        val rule = a.indices.firstOrNull { abs(a[it] - b.getOrElse(it) { 0.0 }) > EPSILON } ?: return 0
        return if (a[rule] > b.getOrElse(rule) { 0.0 }) -1 else 1
    }

    /**
     * Two-way merge of lists already in rank order. On a tie, A (the region) goes first. A show in
     * both lists (same `dedupeKey`) keeps only its first, so better-ranked, copy.
     */
    fun <T> merge(
        a: List<T>,
        b: List<T>,
        keyA: (T) -> DoubleArray,
        keyB: (T) -> DoubleArray,
        dedupeKey: (T) -> String,
    ): List<T> {
        val out = ArrayList<T>(a.size + b.size)
        val seen = HashSet<String>()
        var i = 0
        var j = 0
        while (i < a.size || j < b.size) {
            val takeA = j >= b.size || (i < a.size && compare(keyA(a[i]), keyB(b[j])) <= 0)
            val next = if (takeA) a[i++] else b[j++]
            if (seen.add(dedupeKey(next))) out += next
        }
        return out
    }
}
