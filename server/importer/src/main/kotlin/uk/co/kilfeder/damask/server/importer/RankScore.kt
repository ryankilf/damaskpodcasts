package uk.co.kilfeder.damask.server.importer

import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * The combined score the index ranks by (`rankScore:desc`), computed at import. Smooth, with no
 * cliffs: a show's weight decays continuously as its newest episode ages.
 *
 *     age_days     = max(0, (scoredAt - newestItemPubdate) / 86400)      (missing date: very old)
 *     freshness    = 0.5 ^ (age_days / halfLifeDays)
 *     feed_years   = max(0, (scoredAt - oldestItemPubdate) / (365 * 86400)) (missing date: 0)
 *     rankScore    = (popularity + popularityPrior)
 *                  * (freshnessFloor + (1 - freshnessFloor) * freshness)
 *                  * (appleListedBoost if the show has an Apple Podcasts ID, else 1)
 *                  * (1 + feedAgeBoost * min(1, feed_years / feedAgeFullYears))
 *
 * then rounded down to a multiple of [step] when that is set. The constants come from the
 * environment (see [fromEnv] and docs/server.md › Ranking) and can be re-applied to the live index
 * without downloading the dump (`importer rescore`).
 */
data class RankScore(
    /** Days for freshness to halve. Lower: recency matters more, sooner. */
    val halfLifeDays: Double = DEFAULT_HALF_LIFE_DAYS,
    /** Share of its weight a show keeps however old it is. Higher: finished series rank better. */
    val freshnessFloor: Double = DEFAULT_FRESHNESS_FLOOR,
    /** Added to popularity so popularity 0 isn't flattened to 0. Higher: popularity matters less. */
    val popularityPrior: Double = DEFAULT_POPULARITY_PRIOR,
    /**
     * Multiplier for shows listed in Apple Podcasts (the dump's `itunesId`). Re-uploads and spam
     * feeds are rarely listed. Higher: listed shows win more often. 1: off.
     */
    val appleListedBoost: Double = DEFAULT_APPLE_LISTED_BOOST,
    /** The most a long-running feed gains: 0.1 is up to 10%. Higher: older feeds win more. 0: off. */
    val feedAgeBoost: Double = DEFAULT_FEED_AGE_BOOST,
    /** Feed age, in years since its oldest episode, at which [feedAgeBoost] is reached in full. */
    val feedAgeFullYears: Double = DEFAULT_FEED_AGE_FULL_YEARS,
    /**
     * Rounds the score down to a multiple of this (0 = off). Nearly equal scores then tie, so the
     * title rules after `rankScore` (attribute, exactness) decide between them. Higher: title
     * matches matter more against small differences in popularity and recency. Leave it at 0: bands
     * make cliffs (see docs/decisions.md › Server, RANK_STEP).
     */
    val step: Double = DEFAULT_STEP,
) {
    init {
        require(halfLifeDays > 0) { "RANK_HALF_LIFE_DAYS must be positive" }
        require(freshnessFloor in 0.0..1.0) { "RANK_FRESHNESS_FLOOR must be between 0 and 1" }
        require(popularityPrior >= 0) { "RANK_POPULARITY_PRIOR must not be negative" }
        require(appleListedBoost > 0) { "RANK_APPLE_LISTED_BOOST must be positive" }
        require(feedAgeBoost >= 0) { "RANK_FEED_AGE_BOOST must not be negative" }
        require(feedAgeFullYears > 0) { "RANK_FEED_AGE_FULL_YEARS must be positive" }
        require(step >= 0) { "RANK_STEP must not be negative" }
    }

    fun score(
        popularity: Int,
        newestItemPubdate: Long?,
        scoredAt: Long,
        appleListed: Boolean = false,
        oldestItemPubdate: Long? = null,
    ): Double {
        val ageDays = newestItemPubdate?.let { max(0.0, (scoredAt - it) / SECONDS_PER_DAY) }
        val freshness = ageDays?.let { HALF.pow(it / halfLifeDays) } ?: 0.0
        val feedYears = oldestItemPubdate?.let { max(0.0, (scoredAt - it) / SECONDS_PER_YEAR) } ?: 0.0
        val pop = popularity.coerceIn(0, MAX_POPULARITY)
        val raw = (pop + popularityPrior) *
            (freshnessFloor + (1 - freshnessFloor) * freshness) *
            (if (appleListed) appleListedBoost else 1.0) *
            (1 + feedAgeBoost * min(1.0, feedYears / feedAgeFullYears))
        return if (step > 0) floor(raw / step) * step else raw
    }

    companion object {
        const val DEFAULT_HALF_LIFE_DAYS = 365.0
        const val DEFAULT_FRESHNESS_FLOOR = 0.4
        const val DEFAULT_POPULARITY_PRIOR = 1.0
        const val DEFAULT_APPLE_LISTED_BOOST = 1.5
        const val DEFAULT_FEED_AGE_BOOST = 0.1
        const val DEFAULT_FEED_AGE_FULL_YEARS = 5.0
        const val DEFAULT_STEP = 0.0

        /** The dump's scores run 0-9; one row of 4.77 M has 29, which would otherwise dominate. */
        const val MAX_POPULARITY = 9
        private const val SECONDS_PER_DAY = 86_400.0
        const val SECONDS_PER_YEAR = 365 * SECONDS_PER_DAY
        private const val HALF = 0.5

        fun fromEnv(env: Map<String, String>): RankScore {
            fun d(name: String, default: Double) = env[name]?.toDoubleOrNull() ?: default
            return RankScore(
                halfLifeDays = d("RANK_HALF_LIFE_DAYS", DEFAULT_HALF_LIFE_DAYS),
                freshnessFloor = d("RANK_FRESHNESS_FLOOR", DEFAULT_FRESHNESS_FLOOR),
                popularityPrior = d("RANK_POPULARITY_PRIOR", DEFAULT_POPULARITY_PRIOR),
                appleListedBoost = d("RANK_APPLE_LISTED_BOOST", DEFAULT_APPLE_LISTED_BOOST),
                feedAgeBoost = d("RANK_FEED_AGE_BOOST", DEFAULT_FEED_AGE_BOOST),
                feedAgeFullYears = d("RANK_FEED_AGE_FULL_YEARS", DEFAULT_FEED_AGE_FULL_YEARS),
                step = d("RANK_STEP", DEFAULT_STEP),
            )
        }
    }
}
