package com.example.domain.models

enum class SortOrder {
    NAME,
    DATE,
    DURATION
}

enum class SortDirection {
    ASC,
    DESC
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class AppLanguage {
    KHMER,
    ENGLISH
}

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val thumbnailPath: String? = null,
    val filePath: String,
    val format: String,
    val quality: String,
    val durationMs: Long = 0L,
    val sourceUrl: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val fileSizeBytes: Long = 0L,
    val mimeType: String = "audio/mpeg",
    val updatedAt: Long = System.currentTimeMillis(),
    val isVideo: Boolean = false
)

data class Playlist(
    val id: Long,
    val name: String,
    val coverPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val songCount: Int = 0,
    val previewThumbnail: String? = null
)

data class DownloadItem(
    val id: String,
    val url: String,
    val title: String,
    val artist: String = "",
    val status: String = "QUEUED",
    val progress: Float = 0f,
    val error: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val filePath: String? = null,
    val thumbnailPath: String? = null,
    val speedBytesPerSec: Long = 0L,
    val retryCount: Int = 0,
    val format: String = "mp3",
    val quality: String = "320kbps",
    val isVideo: Boolean = false,
    val targetPlaylistId: Long? = null
)

data class MediaMetadataResult(
    val url: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val isPlaylist: Boolean = false,
    val playlistItems: List<PlaylistItemInfo> = emptyList(),
    val availableFormats: List<String> = listOf("mp3", "mp4"),
    val availableQualities: List<String> = listOf("128kbps", "192kbps", "320kbps", "360p", "720p", "1080p")
)

data class PlaylistItemInfo(
    val id: String,
    val url: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val isSelected: Boolean = true
)

data class OnlineSearchResult(
    val id: String,
    val title: String,
    val channel: String,
    val durationMs: Long,
    val thumbnailUrl: String?,
    val url: String
)

data class AppVersionManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val changelogEn: List<String> = emptyList(),
    val changelogKm: List<String> = emptyList(),
    val forceUpdate: Boolean = false,
    val minSupportedVersionCode: Int = 1
)
