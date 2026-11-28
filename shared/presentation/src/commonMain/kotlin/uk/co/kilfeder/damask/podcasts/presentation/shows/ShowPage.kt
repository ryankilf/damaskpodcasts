package uk.co.kilfeder.damask.podcasts.presentation.shows

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import uk.co.kilfeder.damask.podcasts.core.model.Episode
import uk.co.kilfeder.damask.podcasts.presentation.episodes.ShowNotes
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkFallback
import uk.co.kilfeder.damask.podcasts.presentation.format.Formats
import uk.co.kilfeder.damask.podcasts.presentation.format.Summarised
import uk.co.kilfeder.damask.podcasts.core.model.DAY_MS
import kotlin.time.Instant

/** How often a show publishes, from the gaps between its recent episodes. */
enum class Cadence { DAILY, WEEKLY, FORTNIGHTLY, MONTHLY }

/** The show itself (design hand-off › Show): everything above the episode list. */
data class ShowPageUi(
    val showId: String,
    /** Blank until the feed has been read. */
    val title: String,
    /** Blank when the feed doesn't say. */
    val author: String,
    val artworkUrl: String?,
    val fallback: ArtworkFallback,
    val subscribed: Boolean,
    val genreId: String,
    val genre: String?,
    /** null when the show publishes too irregularly (or too rarely) to say. */
    val cadence: Cadence?,
    /** "Thursdays", when most recent episodes came out on the same day. */
    val usualDay: String?,
    /** null = never played. */
    val lastPlayed: Summarised?,
    /** Plain text, paragraphs separated by blank lines. */
    val description: String,
    val feedUrl: String,
)


internal object ShowFacts {
    private const val SAMPLE = 10
    private const val DAILY_MAX_DAYS = 1.5
    private const val WEEKLY_MAX_DAYS = 9.0
    private const val FORTNIGHTLY_MAX_DAYS = 18.0
    private const val MONTHLY_MAX_DAYS = 40.0
    private const val MIN_GAPS = 3
    private const val DAY_SHARE = 0.6

    /** Dated episodes, newest first (undated ones are stored as 0). */
    private fun dated(episodes: List<Episode>) = episodes.map { it.publishedAt }.filter { it > 0 }.sortedDescending()

    /** From the median gap between the latest [SAMPLE] episodes. */
    fun cadence(episodes: List<Episode>): Cadence? {
        val gaps = dated(episodes).take(SAMPLE).zipWithNext { a, b -> (a - b).toDouble() / DAY_MS }.sorted()
        if (gaps.size < MIN_GAPS) return null
        val median = gaps[gaps.size / 2]
        return when {
            median <= DAILY_MAX_DAYS -> Cadence.DAILY
            median <= WEEKLY_MAX_DAYS -> Cadence.WEEKLY
            median <= FORTNIGHTLY_MAX_DAYS -> Cadence.FORTNIGHTLY
            median <= MONTHLY_MAX_DAYS -> Cadence.MONTHLY
            else -> null
        }
    }

    /** The weekday most recent episodes came out on, when it's most of them. */
    fun usualDay(episodes: List<Episode>, zone: TimeZone = TimeZone.currentSystemDefault()): String? {
        val days = dated(episodes).take(SAMPLE)
            .map { Instant.fromEpochMilliseconds(it).toLocalDateTime(zone).dayOfWeek }
        if (days.size < MIN_GAPS) return null
        val (day, count) = days.groupingBy { it }.eachCount().maxBy { it.value }
        return if (count >= days.size * DAY_SHARE) Formats.weekdays(day) else null
    }

    fun plainText(html: String): String =
        ShowNotes.parse(html).joinToString("\n\n") { block -> block.spans.joinToString("") { it.text }.trim() }
}
