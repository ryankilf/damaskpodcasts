package uk.co.kilfeder.damask.podcasts.playback

import android.app.PendingIntent
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import uk.co.kilfeder.damask.podcasts.MainActivity
import uk.co.kilfeder.damask.podcasts.R

private const val SKIP_BACK_MS = 10_000L
private const val SKIP_FORWARD_MS = 30_000L

/**
 * Background playback: ExoPlayer in a media session, so playback carries on with the app closed and
 * the notification, lock screen, headphones and Bluetooth can control it. The system media
 * controls show back 10 s / play-pause / forward 30 s. Speech audio
 * attributes; pauses on another app's audio or when headphones are unplugged. Speed changes keep
 * the pitch. The app's [Media3Engine] drives it through a MediaController.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val attributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
            .build()
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(AudioCache.dataSourceFactory(this)))
            .setAudioAttributes(attributes, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .setSeekBackIncrementMs(SKIP_BACK_MS)
            .setSeekForwardIncrementMs(SKIP_FORWARD_MS)
            .build()
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, SkipsForNextAndPrevious(player))
            .setSessionActivity(openApp)
            .setMediaButtonPreferences(skipButtons())
            .build()
    }

    /** Back 10 s and forward 30 s either side of play/pause in the system media controls. */
    private fun skipButtons(): List<CommandButton> = listOf(
        CommandButton.Builder(CommandButton.ICON_SKIP_BACK_10)
            .setPlayerCommand(Player.COMMAND_SEEK_BACK)
            .setDisplayName(getString(R.string.playback_skip_back))
            .setSlots(CommandButton.SLOT_BACK)
            .build(),
        CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD_30)
            .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
            .setDisplayName(getString(R.string.playback_skip_forward))
            .setSlots(CommandButton.SLOT_FORWARD)
            .build(),
    )

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Swiping the app away stops the service unless something is playing. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = session?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.run {
            player.release()
            release()
        }
        session = null
        super.onDestroy()
    }
}

/**
 * Headphones, cars and Bluetooth send "next" and "previous", which mean nothing for one episode:
 * they skip forward 30 s and back 10 s instead, like the buttons.
 */
@OptIn(UnstableApi::class)
private class SkipsForNextAndPrevious(player: Player) : ForwardingPlayer(player) {
    override fun seekToNext() = seekForward()

    override fun seekToNextMediaItem() = seekForward()

    override fun seekToPrevious() = seekBack()

    override fun seekToPreviousMediaItem() = seekBack()

    override fun getAvailableCommands(): Player.Commands = super.getAvailableCommands().buildUpon()
        .addAll(Player.COMMAND_SEEK_TO_NEXT, Player.COMMAND_SEEK_TO_PREVIOUS)
        .build()

    override fun isCommandAvailable(command: Int): Boolean =
        command == Player.COMMAND_SEEK_TO_NEXT || command == Player.COMMAND_SEEK_TO_PREVIOUS ||
            super.isCommandAvailable(command)
}
