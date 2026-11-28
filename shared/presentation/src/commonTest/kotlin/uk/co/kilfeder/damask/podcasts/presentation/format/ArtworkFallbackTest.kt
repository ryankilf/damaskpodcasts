package uk.co.kilfeder.damask.podcasts.presentation.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ArtworkFallbackTest {
    @Test
    fun initialsComeFromTheFirstTwoSignificantWords() {
        assertEquals("RH", ArtworkFallback.of("a", "The Rest Is History").initials)
        assertEquals("LI", ArtworkFallback.of("a", "Ledger & Ink").initials)
        assertEquals("ED", ArtworkFallback.of("a", "Empires of Dust").initials)
        assertEquals("HL", ArtworkFallback.of("a", "Half-Life FM").initials)
        assertEquals("Q", ArtworkFallback.of("a", "QAA").initials)
        assertEquals("T", ArtworkFallback.of("a", "The").initials)
        assertEquals("", ArtworkFallback.of("a", "").initials)
    }

    @Test
    fun theColourSeedDependsOnlyOnTheStableId() {
        val id = "1b4e28ba-2fa1-11d2-883f-0016d3cca427"
        // A fixed value: the same on every platform and every run, whatever the title.
        assertEquals(ArtworkFallback.of(id, "Old name").seed, ArtworkFallback.of(id, "New name").seed)
        assertEquals(EXPECTED_SEED, ArtworkFallback.of(id, "").seed)
        assertNotEquals(ArtworkFallback.of(id, "").seed, ArtworkFallback.of("another-id", "").seed)
        assertTrue(ArtworkFallback.of(id, "").seed >= 0)
    }

    private companion object {
        val EXPECTED_SEED = run {
            var hash = -0x7ee3623b
            "1b4e28ba-2fa1-11d2-883f-0016d3cca427".forEach {
                hash = hash xor it.code
                hash *= 16777619
            }
            hash and Int.MAX_VALUE
        }
    }
}
