package uk.co.kilfeder.damask.podcasts.core

import uk.co.kilfeder.damask.podcasts.core.model.Category
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.Hlc
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext
import uk.co.kilfeder.damask.podcasts.core.model.PlayedBy
import uk.co.kilfeder.damask.podcasts.core.model.PlayedState
import uk.co.kilfeder.damask.podcasts.core.model.PositionState
import uk.co.kilfeder.damask.podcasts.core.model.Show
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.model.ShowType
import uk.co.kilfeder.damask.podcasts.core.model.DAY_MS
import uk.co.kilfeder.damask.podcasts.core.model.MINUTE_MS

/** Fixed "now" for every test: 2026-11-27T12:00:00Z. */
const val NOW_MS: Long = 1_795_780_800_000
const val HOUR_MS: Long = 60 * MINUTE_MS

fun minutesAgo(minutes: Long): Hlc = Hlc.of(NOW_MS - minutes * MINUTE_MS)
fun hoursAgo(hours: Long): Hlc = Hlc.of(NOW_MS - hours * HOUR_MS)
fun daysAgo(days: Long): Hlc = Hlc.of(NOW_MS - days * DAY_MS)

fun category(id: String, isSystem: Boolean = false) = Category(CategoryId(id), name = id, isSystem = isSystem)

fun show(id: String, category: String, type: ShowType = ShowType.EPISODIC) =
    Show(ShowId(id), CategoryId(category), type)

/** An hour-long episode of [show], published [publishedDaysAgo] days before [NOW_MS]. */
fun episode(show: String, guid: String, publishedDaysAgo: Long = 1, durationMs: Long? = HOUR_MS) = Episode(
    key = EpisodeKey(ShowId(show), guid),
    publishedAt = NOW_MS - publishedDaysAgo * DAY_MS,
    feedDurationMs = durationMs,
)

fun Episode.at(positionMs: Long, hlc: Hlc, context: PlayContext = PlayContext.None) =
    copy(position = PositionState(positionMs, hlc, context = context))

/** Listened past the 30% threshold of an hour-long episode at [hlc]. */
fun Episode.listened(hlc: Hlc) = at(positionMs = 30 * MINUTE_MS, hlc = hlc)

fun Episode.finished(hlc: Hlc) = copy(played = PlayedState(played = true, playedBy = PlayedBy.FINISHED, hlc = hlc))

fun Episode.markedPlayed(hlc: Hlc) = copy(played = PlayedState(played = true, playedBy = PlayedBy.MARKED, hlc = hlc))

val List<Episode>.guids: List<String> get() = map { it.key.guid }
val Sequence<Episode>.guids: List<String> get() = map { it.key.guid }.toList()
