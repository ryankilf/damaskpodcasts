package uk.co.kilfeder.damask.podcasts.playback

import dev.nucleusframework.media.control.MediaControlEvent
import dev.nucleusframework.media.control.MediaControlService
import dev.nucleusframework.media.control.MediaMetadata
import dev.nucleusframework.media.control.MediaPlaybackState
import dev.nucleusframework.media.control.MediaPlaybackStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import uk.co.kilfeder.damask.podcasts.presentation.playback.NowPlaying
import uk.co.kilfeder.damask.podcasts.presentation.playback.PlaybackController

private const val SKIP_BACK_MS = 10_000L
private const val SKIP_FORWARD_MS = 30_000L

/**
 * The OS's own media controls on desktop (spec › Desktop playback): media keys and Now Playing in
 * Control Center on macOS, the media flyout on Windows, MPRIS on Linux, through Nucleus Media
 * Control. What's playing is published from [PlaybackController.state]; key presses come back
 * as intents. "Next" and "previous" skip forward 30 s and back 10 s, as on Android.
 */
internal object SystemMediaControls {
    fun start(playback: PlaybackController, scope: CoroutineScope) {
        if (!MediaControlService.isAvailable()) return
        MediaControlService.configure(displayName = "Damask")
        MediaControlService.attach { event -> handle(event, playback) }
        scope.launch {
            playback.state
                .map { now -> now?.let { Track(it.title, it.showTitle, it.artworkUrl, it.durationMs) } }
                .distinctUntilChanged()
                .collect { track -> publish(track) }
        }
        scope.launch { playback.state.collect(::publishState) }
    }

    private data class Track(val title: String, val show: String, val artworkUrl: String?, val durationMs: Long?)

    private fun publish(track: Track?) {
        if (track == null) {
            MediaControlService.setPlaybackState(MediaPlaybackState(MediaPlaybackStatus.STOPPED))
            return
        }
        val metadata = MediaMetadata(
            title = track.title,
            artist = track.show,
            coverUrl = track.artworkUrl,
            duration = track.durationMs,
        )
        MediaControlService.setMetadata(metadata)
    }

    private fun publishState(now: NowPlaying?) {
        if (now == null) return
        val status = if (now.isPlaying) MediaPlaybackStatus.PLAYING else MediaPlaybackStatus.PAUSED
        MediaControlService.setPlaybackState(MediaPlaybackState(status, now.positionMs))
    }

    private fun handle(event: MediaControlEvent, playback: PlaybackController) {
        val playing = playback.state.value?.isPlaying == true
        when (event) {
            MediaControlEvent.Play -> if (!playing) playback.togglePlayPause()
            MediaControlEvent.Pause, MediaControlEvent.Stop -> if (playing) playback.togglePlayPause()
            MediaControlEvent.Toggle -> playback.togglePlayPause()
            MediaControlEvent.Next -> playback.skipBy(SKIP_FORWARD_MS)
            MediaControlEvent.Previous -> playback.skipBy(-SKIP_BACK_MS)
            is MediaControlEvent.SeekBy -> playback.skipBy(event.offsetMs)
            is MediaControlEvent.SetPosition -> playback.seekTo(event.positionMs)
            else -> Unit
        }
    }
}
