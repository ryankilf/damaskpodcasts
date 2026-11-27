package uk.co.kilfeder.damask.podcasts.playback

import android.content.Context
import android.net.Uri
import android.os.storage.StorageManager
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

/** Spec › Storage: at most 5% of free space for streamed audio (and never more than the OS allows). */
private const val FREE_SPACE_SHARE = 0.05

/** When the OS can't say how much cache it allows. */
private const val FALLBACK_CACHE_BYTES = 500L * 1024 * 1024

/** Sent to audio hosts, which count listeners by app. */
private const val USER_AGENT = "Damask/1.0 (Android)"

/**
 * Streamed audio goes through one on-disk cache in `cacheDir` (the OS may clear it), so listening
 * again or seeking back doesn't download again. Plain `http://` enclosures are asked for over
 * HTTPS: Android blocks cleartext traffic, and most hosts serve both.
 */
@OptIn(UnstableApi::class)
internal object AudioCache {
    @Volatile
    private var cache: SimpleCache? = null

    fun dataSourceFactory(context: Context): DataSource.Factory {
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(USER_AGENT)
            .setAllowCrossProtocolRedirects(true)
        val upgraded = ResolvingDataSource.Factory(DefaultDataSource.Factory(context, http)) { spec ->
            if (spec.uri.scheme == "http") spec.withUri(spec.uri.buildUpon().scheme("https").build()) else spec
        }
        return CacheDataSource.Factory()
            .setCache(cache(context))
            .setUpstreamDataSourceFactory(upgraded)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }

    /** A local file path or a URL, as a [Uri] for Media3. */
    fun uriOf(location: String): Uri =
        if (location.startsWith("/")) Uri.fromFile(File(location)) else Uri.parse(location)

    private fun cache(context: Context): SimpleCache = cache ?: synchronized(this) {
        cache ?: SimpleCache(
            File(context.cacheDir, "audio"),
            LeastRecentlyUsedCacheEvictor(maxBytes(context)),
            StandaloneDatabaseProvider(context),
        ).also { cache = it }
    }

    private fun maxBytes(context: Context): Long = runCatching {
        val storage = context.getSystemService(StorageManager::class.java)
        val quota = storage.getCacheQuotaBytes(storage.getUuidForPath(context.cacheDir))
        minOf(quota, (context.cacheDir.usableSpace * FREE_SPACE_SHARE).toLong())
    }.getOrDefault(FALLBACK_CACHE_BYTES)
}
