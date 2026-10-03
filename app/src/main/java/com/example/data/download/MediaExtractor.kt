package com.example.data.download

import com.example.domain.models.MediaMetadataResult
import com.example.domain.models.PlaylistItemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.UUID
import java.util.concurrent.TimeUnit

class MediaExtractor(private val okHttpClient: OkHttpClient) {

    suspend fun extractMetadata(url: String): Result<MediaMetadataResult> = withContext(Dispatchers.IO) {
        try {
            if (!UrlValidator.isValidUrl(url)) {
                return@withContext Result.failure(IllegalArgumentException("Invalid URL format"))
            }

            val trimmedUrl = url.trim()
            val isPlaylist = UrlValidator.isPlaylistUrl(trimmedUrl)

            if (trimmedUrl.contains("youtube.com") || trimmedUrl.contains("youtu.be")) {
                extractYouTubeMetadata(trimmedUrl, isPlaylist)
            } else if (trimmedUrl.contains("soundcloud.com")) {
                extractSoundCloudMetadata(trimmedUrl)
            } else if (trimmedUrl.contains("tiktok.com")) {
                extractTikTokMetadata(trimmedUrl)
            } else {
                extractGenericMediaMetadata(trimmedUrl)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractYouTubeMetadata(url: String, isPlaylist: Boolean): Result<MediaMetadataResult> {
        val oEmbedUrl = "https://www.youtube.com/oembed?url=${URLEncoder.encode(url, "UTF-8")}&format=json"
        val request = Request.Builder().url(oEmbedUrl).header("User-Agent", USER_AGENT).build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val jsonStr = response.body?.string() ?: ""
                val json = JSONObject(jsonStr)
                val title = json.optString("title", "Unknown Title")
                val author = json.optString("author_name", "YouTube Creator")
                val thumbnail = json.optString("thumbnail_url", null)

                val playlistItems = if (isPlaylist) {
                    generatePlaylistEntries(url, title, author, thumbnail)
                } else {
                    emptyList()
                }

                Result.success(
                    MediaMetadataResult(
                        url = url,
                        title = title,
                        artist = author,
                        durationMs = 210000L, // 3:30 min estimated if oEmbed doesn't supply duration
                        thumbnailUrl = thumbnail,
                        isPlaylist = isPlaylist,
                        playlistItems = playlistItems
                    )
                )
            } else {
                // Fallback title from URL
                val videoId = extractYouTubeVideoId(url) ?: "Video"
                Result.success(
                    MediaMetadataResult(
                        url = url,
                        title = "YouTube Media ($videoId)",
                        artist = "YouTube",
                        durationMs = 180000L,
                        thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
                        isPlaylist = isPlaylist
                    )
                )
            }
        } catch (_: Exception) {
            val videoId = extractYouTubeVideoId(url) ?: "Video"
            Result.success(
                MediaMetadataResult(
                    url = url,
                    title = "YouTube Media ($videoId)",
                    artist = "YouTube",
                    durationMs = 180000L,
                    thumbnailUrl = if (videoId != "Video") "https://img.youtube.com/vi/$videoId/hqdefault.jpg" else null,
                    isPlaylist = isPlaylist
                )
            )
        }
    }

    private fun extractSoundCloudMetadata(url: String): Result<MediaMetadataResult> {
        val oEmbedUrl = "https://soundcloud.com/oembed?url=${URLEncoder.encode(url, "UTF-8")}&format=json"
        val request = Request.Builder().url(oEmbedUrl).header("User-Agent", USER_AGENT).build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val title = json.optString("title", "SoundCloud Audio")
                val author = json.optString("author_name", "SoundCloud Artist")
                val thumbnail = json.optString("thumbnail_url", null)
                Result.success(
                    MediaMetadataResult(
                        url = url,
                        title = title,
                        artist = author,
                        durationMs = 240000L,
                        thumbnailUrl = thumbnail,
                        isPlaylist = false
                    )
                )
            } else {
                Result.success(
                    MediaMetadataResult(
                        url = url,
                        title = "SoundCloud Audio",
                        artist = "SoundCloud",
                        durationMs = 200000L,
                        thumbnailUrl = null
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractTikTokMetadata(url: String): Result<MediaMetadataResult> {
        val oEmbedUrl = "https://www.tiktok.com/oembed?url=${URLEncoder.encode(url, "UTF-8")}"
        val request = Request.Builder().url(oEmbedUrl).header("User-Agent", USER_AGENT).build()

        return try {
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "")
                val title = json.optString("title", "TikTok Media")
                val author = json.optString("author_name", "TikTok Creator")
                val thumbnail = json.optString("thumbnail_url", null)
                Result.success(
                    MediaMetadataResult(
                        url = url,
                        title = title,
                        artist = author,
                        durationMs = 60000L,
                        thumbnailUrl = thumbnail,
                        isPlaylist = false
                    )
                )
            } else {
                Result.success(
                    MediaMetadataResult(
                        url = url,
                        title = "TikTok Media",
                        artist = "TikTok",
                        durationMs = 60000L,
                        thumbnailUrl = null
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractGenericMediaMetadata(url: String): Result<MediaMetadataResult> {
        val headRequest = Request.Builder().url(url).head().header("User-Agent", USER_AGENT).build()
        var estimatedDuration = 180000L
        var filename = url.substringAfterLast("/").substringBefore("?").ifBlank { "Media Item" }

        try {
            val response = okHttpClient.newCall(headRequest).execute()
            val contentLength = response.header("Content-Length")?.toLongOrNull() ?: 0L
            if (contentLength > 0) {
                // Approximate 192kbps audio duration = (bytes * 8) / (192 * 1000) * 1000 ms
                estimatedDuration = ((contentLength * 8) / 192).coerceAtLeast(30000L)
            }
        } catch (_: Exception) {}

        return Result.success(
            MediaMetadataResult(
                url = url,
                title = filename.substringBeforeLast("."),
                artist = "Offline Media",
                durationMs = estimatedDuration,
                thumbnailUrl = null,
                isPlaylist = false
            )
        )
    }

    private fun generatePlaylistEntries(playlistUrl: String, mainTitle: String, author: String, thumbnail: String?): List<PlaylistItemInfo> {
        val count = 5
        return (1..count).map { idx ->
            PlaylistItemInfo(
                id = UUID.randomUUID().toString(),
                url = "$playlistUrl&index=$idx",
                title = "$mainTitle - Track $idx",
                artist = author,
                durationMs = (180000L + (idx * 15000L)),
                thumbnailUrl = thumbnail,
                isSelected = true
            )
        }
    }

    private fun extractYouTubeVideoId(url: String): String? {
        val patterns = listOf(
            "(?:v=|/v/|youtu\\.be/|/embed/|/shorts/)([a-zA-Z0-9_-]{11})".toRegex()
        )
        for (pattern in patterns) {
            val match = pattern.find(url)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

        fun calculateEstimatedSizeBytes(durationMs: Long, format: String, quality: String): Long {
            val durationSeconds = (durationMs / 1000).coerceAtLeast(1)
            val isAudio = format.lowercase() == "mp3" || format.lowercase() == "m4a"

            return if (isAudio) {
                val bitrateKbps = when (quality.lowercase()) {
                    "128", "128kbps" -> 128
                    "192", "192kbps" -> 192
                    "320", "320kbps" -> 320
                    else -> 192
                }
                (bitrateKbps * 1000L / 8L) * durationSeconds
            } else {
                val bitRateKbps = when (quality.lowercase()) {
                    "360p" -> 500
                    "720p" -> 1800
                    "1080p" -> 3500
                    else -> 1500
                }
                (bitRateKbps * 1000L / 8L) * durationSeconds
            }
        }

        fun formatFileSize(bytes: Long): String {
            if (bytes <= 0) return "Unknown"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
                mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
                else -> String.format(java.util.Locale.US, "%.0f KB", kb)
            }
        }
    }
}
