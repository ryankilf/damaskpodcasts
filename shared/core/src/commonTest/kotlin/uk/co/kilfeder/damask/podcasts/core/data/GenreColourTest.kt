package uk.co.kilfeder.damask.podcasts.core.data

import uk.co.kilfeder.damask.podcasts.core.NOW_MS
import uk.co.kilfeder.damask.podcasts.core.db.DamaskDatabase
import uk.co.kilfeder.damask.podcasts.core.model.CategoryId
import uk.co.kilfeder.damask.podcasts.core.model.GenreColour
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GenreColourTest {
    private val driver = inMemoryDriver()
    private val db = DamaskDatabase(driver)
    private val settings = GenreSettings(db, DeviceClock(db) { NOW_MS })

    @AfterTest
    fun close() = driver.close()

    private fun genre(id: String, withShow: Boolean = true) {
        db.categoryQueries.upsert(id, id, is_system = 0, deleted = 0, hlc = 1)
        if (withShow) {
            db.showQueries.upsert("s-$id", "https://$id.example/f", "$id.example/f", id, null, null, 10, 1, hlc = 1)
        }
    }

    private fun colourOf(id: String) = LibraryRepository(db).load().category(CategoryId(id))?.colour

    @Test
    fun genresInUseGetTheLeastUsedColourInNameOrder() {
        genre("news")
        genre("comedy")
        genre("empty", withShow = false)
        settings.assignMissingColours()
        assertEquals(GenreColour.RED, colourOf("comedy"))
        assertEquals(GenreColour.ORANGE, colourOf("news"))
        assertNull(colourOf("empty"), "no shows yet, so no colour used up")
        assertNull(colourOf("uncategorised"), "Uncategorised is always grey")
    }

    @Test
    fun coloursOnlyRepeatPastSixteen() {
        (1..17).forEach { genre("g" + it.toString().padStart(2, '0')) }
        settings.assignMissingColours()
        val colours = (1..17).map { colourOf("g" + it.toString().padStart(2, '0')) }
        assertEquals(16, colours.take(16).toSet().size)
        assertEquals(GenreColour.RED, colours.last())
    }

    @Test
    fun aPickedColourIsKeptAndCountsAsUsed() {
        genre("news")
        settings.setColour(CategoryId("news"), GenreColour.RED)
        settings.assignMissingColours()
        assertEquals(GenreColour.RED, colourOf("news"))
        assertEquals(GenreColour.ORANGE, settings.nextColour())
    }
}
