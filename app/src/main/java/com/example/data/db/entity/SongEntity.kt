package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["isFavorite"]),
        Index(value = ["createdAt"]),
        Index(value = ["sourceUrl"])
    ]
)
data class SongEntity(
    @PrimaryKey
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
