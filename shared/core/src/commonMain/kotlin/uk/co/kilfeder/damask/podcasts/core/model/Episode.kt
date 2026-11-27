package uk.co.kilfeder.damask.podcasts.core.model

/** Why an episode is played. Only [FINISHED] counts towards the rotation. */
enum class PlayedBy { FINISHED, MARKED }

/** `episode_played`: row-level last-write-wins. `played = false` means "marked unplayed". */
data class PlayedState(
    val played: Boolean,
    val playedBy: PlayedBy?,
    val hlc: Hlc,
)

/** `episode_position`: row-level last-write-wins. */
data class PositionState(
    val positionMs: Long,
    val hlc: Hlc,
    /** Length of the copy the position was saved on (dynamic ad insertion guard). */
    val durationAtSaveMs: Long? = null,
    val context: PlayContext = PlayContext.None,
)

/** Where playback started; decides what autoplay plays next (spec › Finishing and autoplay). */
sealed interface PlayContext {
    data object UpNext : PlayContext

    data class Genre(val categoryId: CategoryId) : PlayContext

    data class Show(val showId: ShowId) : PlayContext

    /** Search, Played, a shared link: playback stops when the episode finishes. */
    data object None : PlayContext

    /** The `play_context` column encoding: `UP_NEXT | GENRE:<id> | SHOW:<id> | NONE`. */
    fun encode(): String = when (this) {
        UpNext -> UP_NEXT
        is Genre -> "$GENRE_PREFIX${categoryId.value}"
        is Show -> "$SHOW_PREFIX${showId.value}"
        None -> NONE
    }

    companion object {
        private const val UP_NEXT = "UP_NEXT"
        private const val NONE = "NONE"
        private const val GENRE_PREFIX = "GENRE:"
        private const val SHOW_PREFIX = "SHOW:"

        /** Unknown or null values decode to [None], so a bad row never autoplays something odd. */
        fun decode(raw: String?): PlayContext = when {
            raw == UP_NEXT -> UpNext
            raw == null -> None
            raw.startsWith(GENRE_PREFIX) -> Genre(CategoryId(raw.removePrefix(GENRE_PREFIX)))
            raw.startsWith(SHOW_PREFIX) -> Show(ShowId(raw.removePrefix(SHOW_PREFIX)))
            else -> None
        }
    }
}

/**
 * One episode as the rules see it: feed data joined with synced user state. An episode that has
 * left its feed but has user state (from `episode_meta`) is still represented, for Played.
 */
data class Episode(
    val key: EpisodeKey,
    val publishedAt: Long,
    /** The feed's `itunes:duration`; stays local. */
    val feedDurationMs: Long? = null,
    /** Reported by the player once media is prepared; synced via `episode_meta`. */
    val playerDurationMs: Long? = null,
    val position: PositionState? = null,
    val played: PlayedState? = null,
    val isDownloaded: Boolean = false,
) {
    /** duration(episode): the player's value when known, otherwise the feed's, otherwise unknown. */
    val durationMs: Long? get() = playerDurationMs ?: feedDurationMs

    val isPlayed: Boolean get() = played?.played == true
}
