package uk.co.kilfeder.damask.podcasts.presentation.format

/**
 * One cache key for one image, however many addresses it's published at. Podcast hosts give each
 * episode its own link to the same picture, but name the file after a hash of the image itself:
 * Captivate `/<episode>/297483f2…a185e.jpg`, Megaphone `/podcasts/<episode>/image/ac3fdd5b….jpg`.
 * When the file's name is such a hash (24 or more hex digits), the key is the host and that file
 * name (and the query, which can ask for a different size), so every episode shares one download.
 * Any other address is its own key (null: key by the address).
 */
object ArtworkKey {
    private val URL = Regex("^https?://([^/?#]+)[^?#]*/([^/?#]+)(\\?[^#]*)?")
    private val HASH_NAME = Regex("^[0-9a-fA-F]{24,}(\\.[A-Za-z0-9]+)?$")

    fun of(url: String): String? {
        val match = URL.find(url) ?: return null
        val (host, file, query) = match.destructured
        return if (HASH_NAME.matches(file)) "artwork:${host.lowercase()}/$file$query" else null
    }
}
