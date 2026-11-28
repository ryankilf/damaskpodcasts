package uk.co.kilfeder.damask.server.importer

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import uk.co.kilfeder.damask.server.meili.MeiliAdmin
import uk.co.kilfeder.damask.server.meili.MeiliClient

/**
 * Recomputes `rankScore` for every document in place, using Meilisearch's (experimental)
 * edit-documents-by-function. The function below is [RankScore.score] written in Rhai, run by
 * Meilisearch over the stored inputs (`popularityScore`, `newestItemPubdate`, `oldestItemPubdate`,
 * `appleListed`, `scoredAt`).
 * RescorerTest checks that it agrees with the Kotlin version, so the two can't drift apart.
 */
class Rescorer(private val meili: MeiliClient, private val admin: MeiliAdmin = MeiliAdmin(meili)) {

    suspend fun rescore(index: String, rank: RankScore) {
        admin.enableEditDocumentsByFunction() // synchronous; a no-op when already on
        val context = buildJsonObject {
            put("halfLife", rank.halfLifeDays)
            put("floor", rank.freshnessFloor)
            put("prior", rank.popularityPrior)
            put("maxPopularity", RankScore.MAX_POPULARITY)
            put("appleBoost", rank.appleListedBoost)
            put("ageBoost", rank.feedAgeBoost)
            put("ageFullYears", rank.feedAgeFullYears)
            put("step", rank.step)
        }
        meili.awaitTask(admin.editDocumentsByFunction(index, FUNCTION, context))
    }

    companion object {
        /** RankScore.score, in Rhai. `()` is Rhai's unit (a missing field). */
        val FUNCTION = """
            let pop = doc.popularityScore;
            if pop == () { pop = 0; }
            if pop < 0 { pop = 0; }
            if pop > context.maxPopularity { pop = context.maxPopularity; }
            let fresh = 0.0;
            if doc.newestItemPubdate != () {
                let age = (doc.scoredAt - doc.newestItemPubdate).to_float() / 86400.0;
                if age < 0.0 { age = 0.0; }
                fresh = 0.5 ** (age / context.halfLife);
            }
            let years = 0.0;
            if doc.oldestItemPubdate != () {
                years = (doc.scoredAt - doc.oldestItemPubdate).to_float() / 31536000.0;
                if years < 0.0 { years = 0.0; }
            }
            let ageShare = years / context.ageFullYears;
            if ageShare > 1.0 { ageShare = 1.0; }
            let apple = 1.0;
            if doc.appleListed == true { apple = context.appleBoost; }
            let score = (pop.to_float() + context.prior) * (context.floor + (1.0 - context.floor) * fresh)
                * apple * (1.0 + context.ageBoost * ageShare);
            if context.step > 0.0 { score = (score / context.step).floor() * context.step; }
            doc.rankScore = score;
        """.trimIndent()
    }
}
