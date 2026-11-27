package uk.co.kilfeder.damask.podcasts.core.rotation

/**
 * Takes one item from each source in turn, repeating rounds until a full round adds nothing.
 * Lazy, so callers page with `take(n)`; each iteration of the result starts afresh.
 */
internal fun <T> roundRobin(sources: List<Sequence<T>>): Sequence<T> = sequence {
    var active = sources.map { it.iterator() }
    while (active.isNotEmpty()) {
        active = active.filter { it.hasNext() }
        active.forEach { yield(it.next()) }
    }
}
