package uk.co.kilfeder.damask.server.search

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

class RegionMergeTest {
    private data class Item(val id: String, val dedupe: String, val key: DoubleArray)

    private val byKey = Comparator<Item> { a, b -> RegionMerge.compare(a.key, b.key) }

    /** Keys from a few coarse values per rule, so ties (and near-ties) are common, as in real data. */
    private fun randomItems(random: Random, prefix: String, n: Int, boost: Double): List<Item> = List(n) { i ->
        val rules = DoubleArray(RULES) { rule ->
            val base = random.nextInt(0, 4) / 3.0 + (if (random.nextInt(5) == 0) 1e-12 else 0.0)
            if (rule == RANK_RULE) base * boost else base
        }
        Item("$prefix$i", "k${random.nextInt(0, n)}", rules)
    }.sortedWith(byKey)

    @Test
    fun mergeEqualsSortingTheUnion() {
        val random = Random(20261127)
        repeat(2_000) {
            val a = randomItems(random, "a", random.nextInt(0, 30), boost = 1.25)
            val b = randomItems(random, "b", random.nextInt(0, 30), boost = 1.0)
            val merged = RegionMerge.merge(a, b, { it.key }, { it.key }, dedupeKey = { it.id })
            // Stable sort of A then B: on a tie, A (the region) comes first, as the merge does.
            val sorted = (a + b).sortedWith(byKey)
            assertEquals(sorted.map { it.id }, merged.map { it.id })
        }
    }

    @Test
    fun deduplicationKeepsTheBetterCopy() {
        val random = Random(7)
        repeat(2_000) {
            val a = randomItems(random, "a", random.nextInt(0, 20), boost = 1.25)
            val b = randomItems(random, "b", random.nextInt(0, 20), boost = 1.0)
            val merged = RegionMerge.merge(a, b, { it.key }, { it.key }, dedupeKey = { it.dedupe })
            val expected = (a + b).sortedWith(byKey).distinctBy { it.dedupe }
            assertEquals(expected.map { it.id }, merged.map { it.id })
        }
    }

    @Test
    fun rulesAreComparedInOrderNotSummed() {
        // Better on an earlier rule wins, however much better the other is on later ones.
        val earlier = doubleArrayOf(1.0, 0.5, 0.0)
        val later = doubleArrayOf(0.9, 1.0, 100.0)
        assertEquals(-1, RegionMerge.compare(earlier, later))
        assertEquals(0, RegionMerge.compare(doubleArrayOf(2.0 / 3), doubleArrayOf(0.6666666666667)), "within epsilon")
    }

    private companion object {
        const val RULES = 6
        const val RANK_RULE = 3
    }
}
