package uk.co.kilfeder.damask.podcasts.core.sync

import okio.ByteString.Companion.encodeUtf8
import okio.ByteString.Companion.toByteString
import uk.co.kilfeder.damask.podcasts.core.model.ShowId

/**
 * Show IDs are UUIDv5 (RFC 9562, SHA-1 name-based) of the feed URL at first subscription, fixed
 * for life (spec › Data model).
 *
 * The name hashed is the *normalised* URL ([FeedUrl.normalise]), so two devices subscribing via
 * `http://` and `https://` spellings of the same feed get the same ID and never need an alias.
 * Normalisation keeps the query string, so private feeds with different tokens stay distinct.
 */
object ShowIds {
    /** RFC 9562 namespace for URLs: 6ba7b811-9dad-11d1-80b4-00c04fd430c8. */
    private val URL_NAMESPACE = byteArrayOf(
        0x6b, 0xa7.toByte(), 0xb8.toByte(), 0x11, 0x9d.toByte(), 0xad.toByte(), 0x11, 0xd1.toByte(),
        0x80.toByte(), 0xb4.toByte(), 0x00, 0xc0.toByte(), 0x4f, 0xd4.toByte(), 0x30, 0xc8.toByte(),
    )
    private const val UUID_BYTES = 16
    private const val VERSION_INDEX = 6
    private const val VARIANT_INDEX = 8
    private const val VERSION_5 = 0x50
    private const val VARIANT_RFC = 0x80
    private const val LOW_NIBBLE = 0x0f
    private const val LOW_SIX_BITS = 0x3f
    private val GROUPS = listOf(0..3, 4..5, 6..7, 8..9, 10..15)

    fun fromFeedUrl(feedUrl: String): ShowId = ShowId(uuid5(FeedUrl.normalise(feedUrl)))

    internal fun uuid5(name: String): String {
        val hash = (URL_NAMESPACE.toByteString() + name.encodeUtf8()).sha1().toByteArray().copyOf(UUID_BYTES)
        hash[VERSION_INDEX] = ((hash[VERSION_INDEX].toInt() and LOW_NIBBLE) or VERSION_5).toByte()
        hash[VARIANT_INDEX] = ((hash[VARIANT_INDEX].toInt() and LOW_SIX_BITS) or VARIANT_RFC).toByte()
        return GROUPS.joinToString("-") { range -> hash.sliceArray(range).toByteString().hex() }
    }

    private operator fun okio.ByteString.plus(other: okio.ByteString): okio.ByteString =
        (toByteArray() + other.toByteArray()).toByteString()
}
