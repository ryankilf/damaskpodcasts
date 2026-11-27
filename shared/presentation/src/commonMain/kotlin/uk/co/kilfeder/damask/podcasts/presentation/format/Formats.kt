package uk.co.kilfeder.damask.podcasts.presentation.format

import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * A summarised value and the exact one behind it (spec › Decisions › Summarised values): the UI
 * shows [summary] and reveals [exact] on long press, hover or focus, and uses it as the
 * accessible label. No summary ships without it.
 */
data class Summarised(val summary: String, val exact: String)

/**
 * Every user-visible date, duration and progress string, formatted here so the Compose UI and a
 * future SwiftUI UI show the same text. English only for now.
 */
object Formats {
    private const val MINUTE_MS = 60_000L
    private const val SECOND_MS = 1_000L
    private const val HOUR_MINUTES = 60L
    private const val MINUTE_SECONDS = 60L
    private const val WEEK_DAYS = 7
    private const val MONTH_DAYS = 30
    private const val YEAR_DAYS = 365
    private const val PERCENT = 100
    private const val TWO_DIGITS = 2
    private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    /** "Today", "Yesterday", "3 days ago", "2 weeks ago", "5 months ago", "2 years ago". */
    fun relativeDate(epochMs: Long, nowMs: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
        val then = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone).date
        val today = Instant.fromEpochMilliseconds(nowMs).toLocalDateTime(zone).date
        val days = then.daysUntil(today)
        return when {
            days <= 0 -> "Today"
            days == 1 -> "Yesterday"
            days < WEEK_DAYS -> "$days days ago"
            days < MONTH_DAYS -> plural(days / WEEK_DAYS, "week") + " ago"
            days < YEAR_DAYS -> plural(days / MONTH_DAYS, "month") + " ago"
            else -> plural(days / YEAR_DAYS, "year") + " ago"
        }
    }

    /** "Thu 6 Nov 2026, 21:14". */
    fun exactDateTime(epochMs: Long, zone: TimeZone = TimeZone.currentSystemDefault()): String {
        val t = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone)
        return "${WEEKDAYS[t.dayOfWeek.ordinal]} ${t.day} ${MONTHS[t.month.ordinal]} ${t.year}, " +
            "${pad(t.hour.toLong())}:${pad(t.minute.toLong())}"
    }

    fun date(epochMs: Long, nowMs: Long, zone: TimeZone = TimeZone.currentSystemDefault()) =
        Summarised(relativeDate(epochMs, nowMs, zone), exactDateTime(epochMs, zone))

    /** "54 min", "1 h 12 min", "under 1 min". */
    fun duration(ms: Long): String {
        val minutes = ms / MINUTE_MS
        val hours = minutes / HOUR_MINUTES
        val rest = minutes % HOUR_MINUTES
        return when {
            minutes < 1 -> "under 1 min"
            hours == 0L -> "$minutes min"
            rest == 0L -> "$hours h"
            else -> "$hours h $rest min"
        }
    }

    /** "54:10", "1:02:03". */
    fun clock(ms: Long): String {
        val totalSeconds = ms.coerceAtLeast(0) / SECOND_MS
        val hours = totalSeconds / (HOUR_MINUTES * MINUTE_SECONDS)
        val minutes = totalSeconds / MINUTE_SECONDS % HOUR_MINUTES
        val seconds = totalSeconds % MINUTE_SECONDS
        return if (hours > 0) "$hours:${pad(minutes)}:${pad(seconds)}" else "$minutes:${pad(seconds)}"
    }

    /** The episode's length, or what's left of it once started. */
    fun length(durationMs: Long?, positionMs: Long?): Summarised? {
        val total = durationMs ?: return null
        if (positionMs == null || positionMs <= 0) return Summarised(duration(total), clock(total))
        val left = (total - positionMs).coerceAtLeast(0)
        val percent = (positionMs * PERCENT / total).coerceIn(0, PERCENT.toLong())
        return Summarised("${duration(left)} left", "${clock(left)} left of ${clock(total)} ($percent%)")
    }

    private fun plural(n: Int, unit: String) = if (n == 1) "1 $unit" else "$n ${unit}s"

    private fun pad(n: Long) = n.toString().padStart(TWO_DIGITS, '0')
}
