package com.example.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "search_history",
    indices = [
        Index(value = ["query"], unique = true),
        Index(value = ["timestamp"])
    ]
)
data class SearchHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
