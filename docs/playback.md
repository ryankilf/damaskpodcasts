# Playback

How an episode gets from a tap to your ears, and how listening is recorded. The rules themselves (what counts as finished, what autoplays) are in [the spec](spec.md) › Finishing and autoplay and [Rotation](rotation.md).

## Layers

| Piece | Where | Does |
|---|---|---|
| `AudioEngine` | `presentation/playback` | The interface each platform implements: load, play, pause, seek, speed, clear, and events. It only plays. |
| `PlaybackController` | `presentation/playback` | What the UI calls (`play`, `togglePlayPause`, `seekTo`, `skipBy`, `setSpeed`, `onMarkedPlayed`) and the `NowPlaying` state it shows. One per process, on `AppGraph.playback`. |
| `PlaybackCore` | `presentation/playback` | The rules: resume, save, finish, autoplay. Handles engine events whoever caused them (the app, the notification, headphones, the car). |
| `ListeningSession` | `presentation/playback` | The episode being played: saves its position and adds listening time on the write triggers. |
| `PlaybackPlan` | `presentation/playback` | Works out what to load: the audio's address (a download wins over the feed URL), the start position, the context, the category. |
| `Media3Engine` + `PlaybackService` | `androidApp/playback` | Android: ExoPlayer in a `MediaSessionService`, driven through a `MediaController`. |

Desktop and iOS have no engine yet (`NoAudioEngine`), so their controls show disabled. Desktop's is the FFmpeg/JavaCV spike in the spec.

## What happens

- **Play.** If the episode is already loaded, it resumes. Otherwise the current episode is saved (it stays In Progress) and the new one loads at its resume point: the saved position if it's in progress, rewound 30 seconds when the copy's length changed (dynamic ad insertion, `ResumePosition`), else the start. The context comes from where it was started; resuming from In Progress uses the context saved with it.
- **Write triggers** (spec › Sync): the position is saved every 30 seconds while playing, and on pause, stop and seek. Each save adds the time listened since the last one to today's `listen_stats` row for the show's genre, as wall time and audio time. Time skipped by seeking never counts. The position, the time and any finish are written in one transaction.
- **Finishing.** Pausing or stopping with 2 minutes or less left finishes the episode. Reaching the true end does too, and then autoplay picks the next episode for the context from a freshly read rotation (the cached one may predate the finish), or stops.
- **Mark played on the playing episode** stops it without saving a position over the mark.
- **The player's duration** replaces the feed's once the media is prepared.

## Android

- **Background playback.** `PlaybackService` runs ExoPlayer in a media session, so it keeps playing with the app closed, and the notification, lock screen, headphones and Bluetooth control it. Swiping the app away stops the service only when nothing is playing.
- **Audio.** Speech content type, audio focus handled (pauses for calls and other apps), pauses when headphones are unplugged, pitch-preserving speed, back 10 s / forward 30 s.
- **"Playing" means playWhenReady**, the listener's intent, not "making sound". Buffering mid-episode isn't a stop, so it doesn't save or trigger the finish rule.
- **Streaming cache.** Audio streams through one `SimpleCache` in `cacheDir/audio`, at most the smaller of the OS's cache quota and 5% of free space, least recently used evicted first. Listening again or seeking back doesn't download again.
- **Plain `http://` enclosures are asked for over HTTPS.** Android blocks cleartext traffic and most hosts serve both. An episode only offered over HTTP fails with a plain-English message; allowing cleartext for audio is an open question.
- Errors reach the UI in plain English (`androidApp` string resources).
