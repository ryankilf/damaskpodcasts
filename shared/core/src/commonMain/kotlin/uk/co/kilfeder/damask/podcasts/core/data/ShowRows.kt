package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.model.Hlc
import uk.co.kilfeder.damask.podcasts.core.model.ShowId
import uk.co.kilfeder.damask.podcasts.core.db.Show as ShowRow

/** A show's synced row, or null if there isn't one. */
internal fun DamaskDatabase.showRow(id: ShowId): ShowRow? = showQueries.selectById(id.value).executeAsOneOrNull()

/** Writes a show's synced row with a new stamp; an older stamp than the stored one is ignored. */
internal fun DamaskDatabase.writeShow(row: ShowRow, hlc: Hlc) = showQueries.upsert(
    id = row.id,
    feed_url = row.feed_url,
    feed_url_norm = row.feed_url_norm,
    category_id = row.category_id,
    episode_order = row.episode_order,
    keep_mode = row.keep_mode,
    keep_count = row.keep_count,
    subscribed = row.subscribed,
    hlc = hlc.value,
)
