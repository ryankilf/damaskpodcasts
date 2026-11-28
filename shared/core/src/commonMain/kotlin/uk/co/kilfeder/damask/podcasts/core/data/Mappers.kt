package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.core.model.EpisodeKey
import uk.co.kilfeder.damask.podcasts.core.model.Hlc
import uk.co.kilfeder.damask.podcasts.core.model.PlayContext
import uk.co.kilfeder.damask.podcasts.core.model.PlayedState
import uk.co.kilfeder.damask.podcasts.core.model.PositionState
import uk.co.kilfeder.damask.podcasts.core.model.Show
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.model.ShowType
import uk.co.kilfeder.damask.podcasts.core.db.Show as ShowRow

internal fun ShowRow.toModel(type: ShowType) = Show(
    id = ShowId(id),
    categoryId = CategoryId(category_id),
    type = type,
    episodeOrderOverride = Columns.episodeOrder(episode_order),
    keepModeOverride = Columns.keepMode(keep_mode),
    keepCount = keep_count.toInt(),
    subscribed = Columns.bool(subscribed),
    feedUrl = feed_url,
)

/**
 * Row mapper for the episode-with-state queries in Library.sq. Passed to SQLDelight's generated
 * `mapper` overloads, which call it with every column positionally, hence the long parameter list.
 */
@Suppress("LongParameterList")
internal fun episodeRow(
    showId: String,
    guid: String,
    publishedAt: Long,
    feedDurationMs: Long?,
    playerDurationMs: Long?,
    downloaded: Boolean,
    positionMs: Long?,
    durationAtSaveMs: Long?,
    playContext: String?,
    positionHlc: Long?,
    played: Long?,
    playedBy: String?,
    playedHlc: Long?,
    title: String,
    imageUrl: String?,
): Episode = Episode(
    key = EpisodeKey(ShowId(showId), guid),
    publishedAt = publishedAt,
    feedDurationMs = feedDurationMs,
    playerDurationMs = playerDurationMs,
    isDownloaded = downloaded,
    title = title,
    imageUrl = imageUrl,
    position = positionMs?.let { ms ->
        PositionState(
            positionMs = ms,
            hlc = Hlc(requireNotNull(positionHlc)),
            durationAtSaveMs = durationAtSaveMs,
            context = PlayContext.decode(playContext),
        )
    },
    played = played?.let { flag ->
        PlayedState(
            played = Columns.bool(flag),
            playedBy = Columns.playedBy(playedBy),
            hlc = Hlc(requireNotNull(playedHlc)),
        )
    },
)
