package uk.co.kilfeder.damask.podcasts.presentation.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class ArtworkKeyTest {
    @Test
    fun captivatesPerEpisodeLinksShareOneKey() {
        val file = "297483f22572490c274d52191f4a185e.jpg"
        val a = ArtworkKey.of("https://artwork.captivate.fm/1f4abfb9-beda-47fe-b62f-1b9ed206d2a7/$file")
        val b = ArtworkKey.of("https://artwork.captivate.fm/789d3dc5-7468-4f20-b2f0-640f4b329d3c/$file")
        assertEquals(a, b)
    }

    @Test
    fun megaphonesShareOneKeyAndKeepTheirSize() {
        val query = "?ixlib=rails-4.3.1&max-w=3000&max-h=3000&fit=crop&auto=format,compress"
        val path = "/image/ac3fdd5bdca25371ee5908b26930a7bf.jpg"
        val a = ArtworkKey.of("https://megaphone-images.spotifycdn.com/podcasts/41d16504-ca34$path$query")
        val b = ArtworkKey.of("https://megaphone-images.spotifycdn.com/podcasts/d82cf4f8-c4b9$path$query")
        assertEquals(a, b)
        val smaller = ArtworkKey.of("https://megaphone-images.spotifycdn.com/podcasts/41d16504-ca34$path?max-w=300")
        assertNotEquals(a, smaller)
    }

    @Test
    fun differentPicturesKeepDifferentKeys() {
        assertNotEquals(
            ArtworkKey.of("https://artwork.captivate.fm/x/297483f22572490c274d52191f4a185e.jpg"),
            ArtworkKey.of("https://artwork.captivate.fm/x/aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa.jpg"),
        )
    }

    @Test
    fun ordinaryNamesAreKeyedByTheirAddress() {
        assertNull(ArtworkKey.of("https://example.com/shows/history/cover.jpg"))
        assertNull(ArtworkKey.of("https://example.com/episode-12-art.png"))
    }
}
