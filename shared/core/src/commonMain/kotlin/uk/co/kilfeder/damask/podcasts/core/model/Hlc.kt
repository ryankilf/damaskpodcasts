package uk.co.kilfeder.damask.podcasts.core.model

import kotlin.jvm.JvmInline

/**
 * Hybrid logical clock timestamp (spec › Sync). Wall-clock milliseconds in the high bits and a
 * counter in the low [COUNTER_BITS], so plain numeric comparison orders it. Stored as `INTEGER`.
 */
@JvmInline
value class Hlc(val value: Long) : Comparable<Hlc> {
    val wallMs: Long get() = value ushr COUNTER_BITS
    val counter: Int get() = (value and COUNTER_MASK).toInt()

    override fun compareTo(other: Hlc): Int = value.compareTo(other.value)

    override fun toString(): String = "Hlc($wallMs:$counter)"

    companion object {
        const val COUNTER_BITS = 16
        const val MAX_COUNTER = (1 shl COUNTER_BITS) - 1
        private const val COUNTER_MASK = MAX_COUNTER.toLong()

        val ZERO = Hlc(0)

        fun of(wallMs: Long, counter: Int = 0): Hlc {
            require(wallMs >= 0) { "wallMs must be non-negative: $wallMs" }
            require(counter in 0..MAX_COUNTER) { "counter out of range: $counter" }
            return Hlc((wallMs shl COUNTER_BITS) or counter.toLong())
        }
    }
}

/** The later of two optional timestamps. */
fun laterOf(a: Hlc?, b: Hlc?): Hlc? = when {
    a == null -> b
    b == null -> a
    else -> if (a >= b) a else b
}
