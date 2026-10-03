# MusicHub Project Summary

## 1. Project Overview

MusicHub is a standalone native Android application designed for downloading supported media, organizing audio and video libraries, and providing offline media playback. The application is distributed as a standalone APK with built-in in-app APK update capabilities.

- App Name: MusicHub
- Application ID: `com.aistudio.musichub.vwnkqp`
- Base Package: `com.example`
- Minimum SDK: 26 (Android 8.0 Oreo)
- Target SDK: 35 (Android 15)
- Languages Supported: Khmer (default) and English

## 2. Technical Stack and Dependencies

- Kotlin 2.2.10
- Jetpack Compose with Material Design 3 (M3)
- Android Gradle Plugin 9.1.1
- Room Database 2.7.0 with Kotlin Symbol Processing (KSP)
- DataStore Preferences 1.1.7
- Navigation Compose 2.8.9
- Media3 ExoPlayer & MediaSession 1.5.1
- Coil Compose 2.7.0 for image loading
- WorkManager 2.10.0 for scheduled work
- OkHttp 4.10.0 & Retrofit 2.12.0 for network communication
- Robolectric 4.16.1 & JUnit 4.13.2 for unit testing

## 3. Implemented Features

### Phase 1: Foundation
- Material 3 theme with exact light (#F5F6F8 background, #111318 primary) and dark (#0E0F12 background, #FFFFFF primary) palettes.
- Custom adaptive launcher icon with dark gradient background and stylized vector music note and download emblem.
- Full localization in Khmer (`res/values-km/strings.xml`) and English (`res/values/strings.xml`). Zero emojis throughout the codebase.
- AppContainer dependency injection pattern decoupling repositories and ViewModels.

### Phase 2: Download System
- URL validation supporting YouTube, SoundCloud, TikTok, and direct media streams.
- Metadata extractor pulling title, author, duration, and thumbnail via oEmbed and HTTP HEAD inspection.
- Audio formats (MP3 at 128 kbps, 192 kbps, 320 kbps) and video formats (MP4 at 360p, 720p, 1080p).
- Playlist URL support with item selection, select all, deselect all, and total size estimation.
- Duplicate prevention checking existing songs and queued downloads.
- Storage full detection ensuring at least 50MB available space before downloading.
- Download queue with real-time speed, progress percentage, pause/resume, cancel, and retry.
- Completed downloads automatically saved into app-specific storage and indexed into Room.
- Clipboard link detection with "Use copied link" banner.
- ACTION_SEND share intent handler in MainActivity to prefill URLs shared from external apps.

### Phase 3: Media Playback
- Media3 ExoPlayer integration with `AudioAttributes.USAGE_MEDIA` and audio focus handling.
- `PlaybackService` running as an exported `MediaSessionService` (`foregroundServiceType="mediaPlayback"`).
- Background playback with system notification controls, lock screen metadata, and Bluetooth media button handling.
- 64dp Mini Player above bottom navigation displaying 44dp thumbnail, title, artist, favorite toggle, and linear progress.
- Now Playing modal screen with dynamic canvas waveform seek bar, time labels, shuffle, repeat (off, all, one), speed control (0.5x..2.0x), sleep timer, and queue manager.
- Video mode presentation using `androidx.media3.ui.PlayerView` attached to the shared player.

### Phase 4: Playlists and Favorites
- Playlist management: create, rename, delete, add songs, remove songs with Undo snackbar.
- Custom reordering of songs within playlists.
- Automatic playlist cover art derived from song thumbnails.
- Favorites toggle persisted reactively in Room.

### Phase 5: Search
- Dual search modes: On-device local library search and Online public search.
- 300ms debounce on search queries.
- Search history chips with clear history capability.
- Online search result cards with direct one-tap download action.

### Phase 6: Settings
- Downloads: default format, default quality, Wi-Fi only toggle, storage location.
- Playback: audio focus handling, gapless playback, default playback speed.
- Appearance: theme switcher (System Default, Light, Dark) and language switcher (Khmer, English).
- Storage: calculated audio, video, and cache sizes with clear cache action.
- Updates: installed version display, check for updates, auto-check toggle, and extraction engine updater.
- About: copyright and platform terms disclaimer, open-source licenses.

### Phase 7: In-App APK Updater
- `UpdateRepository` fetches and parses `version.json` manifest.
- Numerical `versionCode` comparison against installed version.
- Enforces `forceUpdate` and `minSupportedVersionCode`.
- Background APK download with live byte count, progress bar, and cancellation.
- Authentic SHA-256 checksum calculation and verification.
- FileProvider-based system package installer handoff (`REQUEST_INSTALL_PACKAGES` permission checking and settings intent).

### Phase 8: Quality and Testing
- Unit tests covering URL validation, version comparison, minimum version logic, forced updates, SHA-256 calculation, download size estimation, and file size formatting.
- In-memory Room database tests verifying CRUD, sorting, duplicate checks, and playlist reordering.

### Phase 9: Automated GitHub Actions CI/CD
- Workflow file `.github/workflows/build.yml` configured with concurrency group.
- Pull request and push builds running lint, unit tests, and uploading `MusicHub-debug` artifact.
- Version-tag builds (`v*`) validating release secrets, decoding keystore from secret, building signed release APK, calculating SHA-256, generating `version.json`, and creating a GitHub Release.

## 4. Database Schema

- `songs`: id, title, artist, thumbnailPath, filePath, format, quality, durationMs, sourceUrl, isFavorite, createdAt, fileSizeBytes, mimeType, updatedAt, isVideo.
- `playlists`: id, name, coverPath, createdAt.
- `playlist_songs`: playlistId, songId, position (composite primary key [playlistId, songId], cascade foreign keys).
- `downloads`: id, url, title, artist, status, progress, error, createdAt, updatedAt, filePath, thumbnailPath, speedBytesPerSec, retryCount, format, quality, isVideo, targetPlaylistId.
- `search_history`: id, query (unique), timestamp.

## 5. Android Manifest and Permissions

- `android.permission.INTERNET`
- `android.permission.ACCESS_NETWORK_STATE`
- `android.permission.POST_NOTIFICATIONS`
- `android.permission.FOREGROUND_SERVICE`
- `android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK`
- `android.permission.FOREGROUND_SERVICE_DATA_SYNC`
- `android.permission.REQUEST_INSTALL_PACKAGES`
- `android.permission.WAKE_LOCK`

## 6. Build and Verification Commands

- Compile project: `gradle compileDebugSources` or `compile_applet` tool
- Run unit tests: `gradle :app:testDebugUnitTest`
- Assemble Debug APK: `gradle assembleDebug`
- Assemble Release APK: `gradle assembleRelease`

## 7. Required GitHub Secrets for Signed Releases

1. `MUSICHUB_KEYSTORE_BASE64`
2. `MUSICHUB_KEYSTORE_PASSWORD`
3. `MUSICHUB_KEY_ALIAS`
4. `MUSICHUB_KEY_PASSWORD`

Detailed instructions are documented in `docs/RELEASE.md`.

## 8. Known Limitations

- YouTube restricted videos or age-gated media that require user credentials or authentication cannot be downloaded directly without session cookies.
- Playback notification style uses standard Media3 MediaSession notification on Android 8.0 through 15; exact layout depends on the device vendor system UI.
- The default in-app update URL points to the GitHub repository release endpoint; configure `version.json` hosting on GitHub Pages or custom CDN if a custom domain is preferred.
