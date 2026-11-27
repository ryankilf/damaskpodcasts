package uk.co.kilfeder.damask.podcasts.playback

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import uk.co.kilfeder.damask.podcasts.R
import uk.co.kilfeder.damask.podcasts.presentation.playback.AudioEngine
import uk.co.kilfeder.damask.podcasts.presentation.playback.EngineEvent
import uk.co.kilfeder.damask.podcasts.presentation.playback.MediaRequest

/**
 * The shared [AudioEngine] on Android: a MediaController connected to [PlaybackService]. Commands
 * issued before the connection is ready are queued. Events come from the session, so a pause
 * from the notification or headphones reaches the shared controller just like one from the app.
 *
 * "Playing" means playWhenReady (the listener's intent), not "producing sound": buffering
 * mid-episode isn't a stop, so it doesn't trigger saves or the finish rule.
 */
class Media3Engine(private val context: Context) : AudioEngine {
    override val available = true

    private var controller: MediaController? = null
    private val pending = mutableListOf<(MediaController) -> Unit>()
    private var emit: (EngineEvent) -> Unit = {}
    private var prepared = false

    override val positionMs: Long get() = controller?.currentPosition ?: 0

    override val durationMs: Long?
        get() = controller?.duration?.takeIf { it != C.TIME_UNSET && it > 0 }

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                val connected = future.get()
                connected.addListener(Events())
                controller = connected
                pending.forEach { it(connected) }
                pending.clear()
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    override fun setListener(listener: (EngineEvent) -> Unit) {
        emit = listener
    }

    override fun load(request: MediaRequest, playWhenReady: Boolean) {
        prepared = false
        val metadata = MediaMetadata.Builder()
            .setTitle(request.title)
            .setArtist(request.showTitle)
            .setArtworkUri(request.artworkUrl?.let(AudioCache::uriOf))
            .build()
        val item = MediaItem.Builder()
            .setMediaId(request.mediaId)
            .setUri(AudioCache.uriOf(request.uri))
            .setMediaMetadata(metadata)
            .build()
        withController {
            it.setMediaItem(item, request.startMs)
            it.setPlaybackSpeed(request.speed)
            it.prepare()
            it.playWhenReady = playWhenReady
        }
    }

    override fun play() = withController { it.play() }

    override fun pause() = withController { it.pause() }

    override fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs) }

    override fun setSpeed(speed: Float) = withController { it.setPlaybackSpeed(speed) }

    override fun clear() = withController {
        it.stop()
        it.clearMediaItems()
    }

    private fun withController(block: (MediaController) -> Unit) {
        controller?.let(block) ?: pending.add(block)
    }

    private inner class Events : Player.Listener {
        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) =
            emit(EngineEvent.PlayingChanged(playWhenReady))

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> emit(EngineEvent.BufferingChanged(true))
                Player.STATE_READY -> {
                    emit(EngineEvent.BufferingChanged(false))
                    if (!prepared) {
                        prepared = true
                        emit(EngineEvent.Prepared(durationMs))
                    }
                }
                Player.STATE_ENDED -> emit(EngineEvent.Ended)
                else -> Unit
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            if (reason == Player.DISCONTINUITY_REASON_SEEK && prepared) {
                emit(EngineEvent.Seeked(oldPosition.positionMs, newPosition.positionMs))
            }
        }

        override fun onPlayerError(error: PlaybackException) = emit(EngineEvent.Failed(message(error)))
    }

    private fun message(error: PlaybackException): String = context.getString(
        when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            -> R.string.playback_error_network
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            -> R.string.playback_error_missing
            PlaybackException.ERROR_CODE_IO_CLEARTEXT_NOT_PERMITTED -> R.string.playback_error_insecure
            else -> R.string.playback_error_other
        },
    )
}
