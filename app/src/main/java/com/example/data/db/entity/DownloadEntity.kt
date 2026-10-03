package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    FAILED,
    CANCELLED
}

@Entity(
    tableName = "downloads",
    indices = [
        Index(value = ["status"]),
        Index(value = ["createdAt"])
    ]
)
data class DownloadEntity(
    @PrimaryKey
    val id: String,
    val url: String,
    val title: String,
    val artist: String = "",
    val status: DownloadStatus = DownloadStatus.QUEUED,
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
