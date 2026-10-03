# MusicHub Technical Architecture Documentation

## 1. System Overview

MusicHub is a native Android application built with Kotlin and Jetpack Compose for downloading supported media, organizing audio and video libraries, and providing offline media playback.

The system is engineered according to Clean Architecture and MVVM patterns to achieve separation of concerns, testability, and responsiveness.

## 2. Layer Architecture

```
                       +---------------------------------------+
                       |             UI Layer                  |
                       | (Jetpack Compose, M3, Navigation)     |
                       +-------------------+-------------------+
                                           |
                                           v
                       +---------------------------------------+
                       |           ViewModel Layer             |
                       | (StateFlow, viewModelScope, Coroutines|
                       +-------------------+-------------------+
                                           |
                                           v
                       +---------------------------------------+
                       |            Domain Layer               |
                       | (Domain Models, Business Rules)       |
                       +-------------------+-------------------+
                                           |
                                           v
                       +---------------------------------------+
                       |             Data Layer                |
                       |  - MediaRepository (Room Database)    |
                       |  - DownloadRepository (Work & Engine) |
                       |  - SettingsRepository (DataStore)     |
                       |  - UpdateRepository (HTTPS Manifest)  |
                       |  - OnlineSearchRepository (Public API)|
                       +---------------------------------------+
```

## 3. Package Structure

```
com.example/
  MusicHubApp.kt              # Application subclass and DI initializer
  MainActivity.kt             # Main entry point and Share intent receiver
  di/
    AppContainer.kt           # Centralized dependency container
  domain/
    models/
      Models.kt               # Song, Playlist, DownloadItem, Manifest models
  data/
    db/
      AppDatabase.kt          # Room Database definition (v1)
      entity/
        SongEntity.kt         # Songs table
        PlaylistEntity.kt     # Playlists table
        PlaylistSongCrossRef.kt # Composite primary key join table
        DownloadEntity.kt     # Download queue and status table
        SearchHistoryEntity.kt# Search history table
      dao/
        SongDao.kt            # CRUD and queries for songs
        PlaylistDao.kt        # CRUD and ordering queries for playlists
        DownloadDao.kt        # Active downloads and queue queries
        SearchHistoryDao.kt   # Recent queries
    repository/
      MediaRepository.kt      # Repository for songs, playlists, favorites
      SettingsRepository.kt   # Preferences DataStore repository
      OnlineSearchRepository.kt # Supported online search repository
    download/
      UrlValidator.kt         # URL parsing and host filtering
      MediaExtractor.kt       # Metadata extraction and size estimation
      DownloadRepository.kt   # Queue orchestration and stream downloading
    update/
      UpdateRepository.kt     # In-app update checker and checksum validator
  player/
    PlaybackManager.kt        # ExoPlayer manager and UI state publisher
  service/
    PlaybackService.kt        # Media3 MediaSessionService for background audio
    DownloadService.kt        # Foreground Service for downloads
  ui/
    theme/
      Color.kt                # Strict light and dark palettes
      Type.kt                 # Material 3 typography with Khmer line heights
      Theme.kt                # MusicHubTheme setup
    navigation/
      MusicHubNavHost.kt      # Bottom bar, Mini player, NavHost orchestration
    components/
      MiniPlayer.kt           # 64dp mini player component
      NowPlayingSheet.kt      # Full screen player and video presentation
      WaveformSeekBar.kt      # Custom canvas waveform seek bar
      AddToPlaylistDialog.kt  # Playlist selection dialog
      UpdateDialog.kt         # In-app update dialog
    home/
      HomeScreen.kt           # Home dashboard
      HomeViewModel.kt
    download/
      DownloadScreen.kt       # Full screen download modal
      DownloadViewModel.kt
    library/
      LibraryScreen.kt        # 5-tab library with sorting and multi-select
      LibraryViewModel.kt
    playlist/
      PlaylistDetailScreen.kt # Reordering and swipe removal with undo
      PlaylistDetailViewModel.kt
    search/
      SearchScreen.kt         # Local and online search with 300ms debounce
      SearchViewModel.kt
    settings/
      SettingsScreen.kt       # Preferences, storage, and update management
      SettingsViewModel.kt
    update/
      UpdateViewModel.kt
```

## 4. Database Schema (Room)

- **`songs`**:
  - `id` (String PK)
  - `title`, `artist`, `thumbnailPath`, `filePath`
  - `format`, `quality`, `durationMs`, `sourceUrl`
  - `isFavorite`, `isVideo`, `fileSizeBytes`, `mimeType`
  - `createdAt`, `updatedAt`
  - Indices on `title`, `artist`, `isFavorite`, `createdAt`, `sourceUrl`.

- **`playlists`**:
  - `id` (Long PK autogenerate)
  - `name`, `coverPath`, `createdAt`

- **`playlist_songs`**:
  - `playlistId` (Long FK -> playlists.id CASCADE)
  - `songId` (String FK -> songs.id CASCADE)
  - `position` (Int)
  - Composite PK: `[playlistId, songId]`
  - Indices on `playlistId`, `songId`.

- **`downloads`**:
  - `id` (String PK)
  - `url`, `title`, `artist`, `status`
  - `progress`, `speedBytesPerSec`, `retryCount`, `error`
  - `filePath`, `thumbnailPath`, `format`, `quality`, `isVideo`, `targetPlaylistId`
  - Indices on `status`, `createdAt`.

- **`search_history`**:
  - `id` (Long PK autogenerate)
  - `query` (String unique index), `timestamp`

## 5. Media3 Playback Engine

- Uses `androidx.media3.exoplayer.ExoPlayer` configured with `AudioAttributes.USAGE_MEDIA` and `handleAudioBecomingNoisy(true)`.
- `PlaybackService` extends `MediaSessionService` providing system media notifications, lock screen controls, and Bluetooth headset button integration.
- `PlaybackManager` manages queue transitions, waveform profile generation off the main thread, speed switching (0.5x..2.0x), and sleep timer countdown.
- Video mode renders via `androidx.media3.ui.PlayerView` attached to the shared player instance. Video rendering is disabled when the app is backgrounded or in audio mode to preserve battery.

## 6. Download Orchestration Engine

- `UrlValidator` verifies HTTP/HTTPS URLs against supported hosts.
- `MediaExtractor` retrieves metadata (oEmbed for YouTube/SoundCloud/TikTok, direct HEAD requests for raw files) on background dispatchers.
- `DownloadRepository` guarantees duplicate prevention by querying both the `songs` and `downloads` tables.
- Storage capacity is verified before downloading (minimum 50MB required).
- Streaming writes calculate live transfer rates and percentage progress without blocking the main thread.
- On completion, files are saved in scoped app storage (`files/music/` or `files/video/`) and automatically indexed into Room.

## 7. Automated In-App Update Engine

- Retrieves `version.json` via HTTPS.
- Compares `versionCode` numerically against `BuildConfig.VERSION_CODE`.
- Enforces `minSupportedVersionCode` and `forceUpdate` restrictions.
- Computes SHA-256 hash using streaming `MessageDigest` and verifies against the manifest.
- Handoff to Android PackageInstaller uses `FileProvider` (`${applicationId}.fileprovider`).
- Handles `REQUEST_INSTALL_PACKAGES` permission checking on Android 8.0+.
