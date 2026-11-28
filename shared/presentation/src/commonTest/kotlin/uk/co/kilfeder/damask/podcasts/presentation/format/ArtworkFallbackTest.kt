package uk.co.kilfeder.damask.podcasts.presentation.format

import uk.co.kilfeder.damask.podcasts.presentation.genres.GenreHue
import kotlin.test.Test
import kotlin.test.assertEquals

class ArtworkFallbackTest {
    private fun initials(title: String) = ArtworkFallback.of(title, GenreHue.GREY).initials

    @Test
    fun initialsComeFromTheFirstTwoSignificantWords() {
        assertEquals("RH", initials("The Rest Is History"))
        assertEquals("LI", initials("Ledger & Ink"))
        assertEquals("ED", initials("Empires of Dust"))
        assertEquals("HL", initials("Half-Life FM"))
        assertEquals("Q", initials("QAA"))
        assertEquals("T", initials("The"))
        assertEquals("", initials(""))
    }

    @Test
    fun theTileTakesTheGenresColour() {
        assertEquals(GenreHue.BROWN, ArtworkFallback.of("Ledger & Ink", GenreHue.BROWN).hue)
    }
}
