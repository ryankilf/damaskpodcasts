package uk.co.kilfeder.damask.podcasts.core.feed

/**
 * RSS dates are meant to be RFC 822 (`Wed, 02 Oct 2002 13:00:00 GMT`), but feeds also send
 * missing day names, single-digit days, two-digit years, named US zones and ISO 8601. Returns
 * epoch milliseconds, or null if the date can't be read.
 */
internal object FeedDates {
    private val RFC_822 = Regex(
        """^(?:[A-Za-z]+,?\s*)?(\d{1,2})\s+([A-Za-z]+)\.?\s+(\d{2,4})\s+(\d{1,2}):(\d{2})(?::(\d{2}))?\s*(.*)$""",
    )
    private val ISO_8601 = Regex(
        """^(\d{4})-(\d{2})-(\d{2})(?:[T ](\d{2}):(\d{2})(?::(\d{2})(?:\.\d+)?)?)?\s*(Z|[+-]\d{2}:?\d{2})?$""",
    )
    private val MONTHS = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    private val NAMED_ZONES = mapOf(
        "GMT" to 0, "UT" to 0, "UTC" to 0, "Z" to 0,
        "EST" to -5, "EDT" to -4, "CST" to -6, "CDT" to -5,
        "MST" to -7, "MDT" to -6, "PST" to -8, "PDT" to -7,
    )
    private val NUMERIC_ZONE = Regex("""^([+-])(\d{2}):?(\d{2})$""")
    private const val TWO_DIGIT_YEAR_PIVOT = 50
    private const val YEAR_1900 = 1900
    private const val YEAR_2000 = 2000
    private const val MONTH_ABBREVIATION = 3

    // Regex group indices. Time and zone groups are at the same positions in both patterns.
    private const val RFC_DAY = 1
    private const val RFC_MONTH = 2
    private const val RFC_YEAR = 3
    private const val ISO_YEAR = 1
    private const val ISO_MONTH = 2
    private const val ISO_DAY = 3
    private const val HOUR = 4
    private const val MINUTE = 5
    private const val SECOND = 6
    private const val ZONE = 7
    private const val MINUTE_MS = 60_000L
    private const val HOUR_MINUTES = 60

    fun parse(raw: String): Long? {
        val text = raw.trim()
        return parseRfc822(text) ?: parseIso8601(text)
    }

    private fun parseRfc822(text: String): Long? {
        val g = RFC_822.matchEntire(text)?.groupValues ?: return null
        val month = MONTHS.indexOf(g[RFC_MONTH].take(MONTH_ABBREVIATION).lowercase()) + 1
        if (month == 0) return null
        val zone = zoneOffsetMinutes(g[ZONE]) ?: return null
        val yearText = g[RFC_YEAR]
        return DateTimeFields(
            year = fullYear(yearText.toInt(), yearText.length),
            month = month,
            day = g[RFC_DAY].toInt(),
            hour = g[HOUR].toInt(),
            minute = g[MINUTE].toInt(),
            second = g[SECOND].toIntOrNull() ?: 0,
        ).toEpochMs(zone)
    }

    private fun parseIso8601(text: String): Long? {
        val g = ISO_8601.matchEntire(text)?.groupValues ?: return null
        val zone = zoneOffsetMinutes(g[ZONE]) ?: return null
        return DateTimeFields(
            year = g[ISO_YEAR].toInt(),
            month = g[ISO_MONTH].toInt(),
            day = g[ISO_DAY].toInt(),
            hour = g[HOUR].toIntOrNull() ?: 0,
            minute = g[MINUTE].toIntOrNull() ?: 0,
            second = g[SECOND].toIntOrNull() ?: 0,
        ).toEpochMs(zone)
    }

    private fun fullYear(year: Int, digits: Int): Int = when {
        digits > 2 -> year
        year < TWO_DIGIT_YEAR_PIVOT -> YEAR_2000 + year
        else -> YEAR_1900 + year
    }

    /** Minutes east of UTC. A missing zone is read as UTC; an unknown one rejects the date. */
    private fun zoneOffsetMinutes(raw: String): Int? {
        val zone = raw.trim().uppercase()
        val named = NAMED_ZONES[zone]
        return when {
            zone.isEmpty() -> 0
            named != null -> named * HOUR_MINUTES
            else -> numericZoneMinutes(zone)
        }
    }

    /** `+hhmm`, `-hhmm`, `+hh:mm`. */
    private fun numericZoneMinutes(zone: String): Int? {
        val (sign, hours, minutes) = NUMERIC_ZONE.matchEntire(zone)?.destructured ?: return null
        val total = hours.toInt() * HOUR_MINUTES + minutes.toInt()
        return if (sign == "-") -total else total
    }

    private class DateTimeFields(
        val year: Int,
        val month: Int,
        val day: Int,
        val hour: Int,
        val minute: Int,
        val second: Int,
    ) {
        fun toEpochMs(zoneOffsetMinutes: Int): Long? {
            if (!isValid()) return null
            val minutes = (daysFromCivil() * MINUTES_PER_DAY) + hour * HOUR_MINUTES + minute - zoneOffsetMinutes
            return minutes * MINUTE_MS + second * SECOND_MS
        }

        @Suppress("MagicNumber") // calendar bounds
        private fun isValid() = month in 1..12 && day in 1..31 && hour in 0..23 && minute in 0..59 && second in 0..60

        /** Days since 1970-01-01 (Howard Hinnant's days_from_civil). */
        @Suppress("MagicNumber") // the algorithm's constants
        private fun daysFromCivil(): Long {
            val y = if (month <= 2) year - 1 else year
            val era = (if (y >= 0) y else y - 399) / 400
            val yoe = y - era * 400
            val mp = (month + 9) % 12
            val doy = (153 * mp + 2) / 5 + day - 1
            val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
            return era * 146_097L + doe - 719_468L
        }

        private companion object {
            const val MINUTES_PER_DAY = 1_440L
            const val SECOND_MS = 1_000L
        }
    }
}
