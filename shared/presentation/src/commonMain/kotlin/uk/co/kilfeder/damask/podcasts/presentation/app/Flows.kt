package uk.co.kilfeder.damask.podcasts.presentation.app

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/** How long a flow keeps running after its last collector goes (a rotation, say), in ms. */
internal const val STOP_TIMEOUT_MS = 5_000L

/** A ViewModel's UI state: shared while the screen is collecting, kept briefly across rotations. */
internal fun <T> Flow<T>.stateInUi(scope: CoroutineScope, initial: T): StateFlow<T> =
    stateIn(scope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), initial)

/** How many rows a long list (Up Next, a genre's queue) shows at a time. */
internal const val PAGE_SIZE = 30

/** The first [size] items, and whether there are more after them. */
internal fun <T> Sequence<T>.page(size: Int): Pair<List<T>, Boolean> {
    val items = take(size + 1).toList()
    return items.take(size) to (items.size > size)
}
