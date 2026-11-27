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
| `FfmpegEngine` + `FfmpegDecoder` + `PcmSink` | `desktopApp/playback` | Desktop: FFmpeg through JavaCV, PCM out through `javax.sound`. |

iOS has no engine yet (`NoAudioEngine`), so its controls show disabled.

## What happens

- **Play.** If the episode is already loaded, it resumes. Otherwise the current episode is saved (it stays In Progress) and the new one loads at its resume point: the saved position if it's in progress, rewound 30 seconds when the copy's length changed (dynamic ad insertion, `ResumePosition`), else the start. The context comes from where it was started; resuming from In Progress uses the context saved with it.
- **Write triggers** (spec › Sync): the position is saved every 30 seconds while playing, and on pause, stop and seek. Each save adds the time listened since the last one to today's `listen_stats` row for the show's genre, as wall time and audio time. Time skipped by seeking never counts. The position, the time and any finish are written in one transaction.
- **Finishing.** Pausing or stopping with 2 minutes or less left finishes the episode. Reaching the true end does too, and then autoplay picks the next episode for the context from a freshly read rotation (the cached one may predate the finish), or stops.
- **Mark played on the playing episode** stops it without saving a position over the mark.
- **The player's duration** replaces the feed's once the media is prepared.

## In the app

- **Tapping an episode row plays it and opens its screen.** The row decides the context: Up Next rows play as Up Next, In Progress rows keep the context saved with them, genre queue rows play as that genre, Played rows play with no autoplay after. (`AppViewModel.onPlayEpisode`, `PlaySource`.)
- **The mini-player** sits above the tabs on every screen while something is loaded: artwork, title, show, progress, play/pause. Tapping it opens the episode. It hides on that episode's own screen.
- **The episode screen** has working controls once the episode is the one loaded: play/pause (the damask play button), back 10 s, forward 30 s, speed (steps through 0.8×, 1×, 1.2×, 1.5×, 1.8×, 2×), and the position bar (tap or drag; screen readers can set it). Play on an episode that isn't loaded starts it with its saved context. Errors show under the controls.

## Android

- **Background playback.** `PlaybackService` runs ExoPlayer in a media session, so it keeps playing with the app closed, and the notification, lock screen, headphones and Bluetooth control it. Swiping the app away stops the service only when nothing is playing.
- **Audio.** Speech content type, audio focus handled (pauses for calls and other apps), pauses when headphones are unplugged, pitch-preserving speed, back 10 s / forward 30 s.
- **"Playing" means playWhenReady**, the listener's intent, not "making sound". Buffering mid-episode isn't a stop, so it doesn't save or trigger the finish rule.
- **Streaming cache.** Audio streams through one `SimpleCache` in `cacheDir/audio`, at most the smaller of the OS's cache quota and 5% of free space, least recently used evicted first. Listening again or seeking back doesn't download again.
- **Plain `http://` enclosures are asked for over HTTPS.** Android blocks cleartext traffic and most hosts serve both. An episode only offered over HTTP fails with a plain-English message; allowing cleartext for audio is an open question.
- **System media controls** (the player in Quick Settings, the lock screen) show back 10 s, play/pause and forward 30 s, with the artwork and a seek bar. Headphones, cars and Bluetooth "next" and "previous" skip forward 30 s and back 10 s (`SkipsForNextAndPrevious`), since there's no next track within an episode.
- Errors reach the UI in plain English (`androidApp` string resources).

## Desktop

- **FFmpeg through JavaCV** (spec › Desktop playback). `FfmpegDecoder` opens the episode (URL or file), decodes to 44.1 kHz stereo 16-bit PCM, and runs it through FFmpeg's `atempo` filter when the speed isn't 1×, so speech keeps its pitch. It plays every format FFmpeg reads (MP3, AAC, Opus, Ogg, FLAC).
- **One playback thread** owns the decoder and the sound line. The UI's calls are queued to it; opening a stream happens there, so the UI never waits on the network. Writing to the sound line blocks while its buffer is full, which paces decoding to real time. The position reported is the decoded position less what's still in the output buffer (scaled by speed).
- **Networking** is FFmpeg's own for now: HTTPS, redirects and reconnects work, and plain `http://` works too (desktop has no cleartext block). The spec's loopback proxy over OkHttp (download-to-disk while streaming, premium credentials, signed-URL retry) is still to come.
- **Natives:** JavaCV is used without its transitive dependencies (it would pull in OpenCV, Tesseract, JavaFX and more). Only JavaCPP and FFmpeg's LGPL build are shipped, for the OS building the app (`nativePlatform` in `desktopApp/build.gradle.kts`). There's no Windows arm64 build of FFmpeg from bytedeco; Windows on Arm would run the x64 app under emulation.
- **Tests:** `FfmpegEngineTest` decodes two 30-second test tones (MP3, Opus) to check lengths, that 2× halves the audio, seeking, and the engine's events. `RealStreamSmokeTest` is opt-in (`-Pdamask.audio=<URL>`) and streams a real episode at 1.5× from 10 minutes in.
- **Still to do:** OS media keys and Now Playing (Nucleus Media Control), the loopback proxy, packaging with the natives, and FFmpeg's LGPL obligations in the shipped app (licence text, a link to the exact source).
