package uk.co.kilfeder.damask.podcasts.ui.components

import coil3.intercept.Interceptor
import coil3.request.ImageResult
import uk.co.kilfeder.damask.podcasts.presentation.format.ArtworkKey

/**
 * Keys every image by [ArtworkKey]: the same picture at many per-episode addresses is
 * downloaded, cached and held in memory once. It sets the disk and memory cache keys before the
 * request runs, which is also what the image loader merges concurrent requests on.
 */
internal object ArtworkKeyInterceptor : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val key = (request.data as? String)?.let(ArtworkKey::of)
        if (key == null || request.diskCacheKey != null) return chain.proceed()
        val keyed = request.newBuilder().diskCacheKey(key).memoryCacheKey(key).build()
        return chain.withRequest(keyed).proceed()
    }
}
