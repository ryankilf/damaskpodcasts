package uk.co.kilfeder.damask.podcasts.presentation.episodes

import uk.co.kilfeder.damask.podcasts.core.model.PlayedBy
import uk.co.kilfeder.damask.podcasts.presentation.app.AppGraph

/** Mark played by hand: stops it if it's playing, and it leaves every queue (undo from Played). */
internal suspend fun AppGraph.markPlayed(id: EpisodeId) {
    playback.onMarkedPlayed(id)
    onDb { userState.markPlayed(id.toKey(), PlayedBy.MARKED) }
}

/** Mark unplayed: also resets the position, per the spec, so it starts again. */
internal suspend fun AppGraph.markUnplayed(id: EpisodeId) {
    onDb { userState.markUnplayed(id.toKey()) }
}
