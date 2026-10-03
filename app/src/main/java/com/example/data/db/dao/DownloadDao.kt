package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.DownloadEntity
import com.example.data.db.entity.DownloadStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING') ORDER BY createdAt ASC")
    fun getActiveDownloads(): Flow<List<DownloadEntity>>

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getDownloadById(id: String): DownloadEntity?

    @Query("SELECT * FROM downloads WHERE id = :id")
    fun observeDownloadById(id: String): Flow<DownloadEntity?>

    @Query("SELECT * FROM downloads WHERE url = :url AND status IN ('QUEUED', 'DOWNLOADING', 'COMPLETED') LIMIT 1")
    suspend fun findExisting(url: String): DownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(download: DownloadEntity)

    @Update
    suspend fun update(download: DownloadEntity)

    @Query("UPDATE downloads SET status = :status, progress = :progress, speedBytesPerSec = :speed, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: String, status: DownloadStatus, progress: Float, speed: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE downloads SET status = :status, error = :error, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateStatus(id: String, status: DownloadStatus, error: String? = null, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(download: DownloadEntity)

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM downloads WHERE status IN ('COMPLETED', 'CANCELLED')")
    suspend fun clearCompleted()

    @Query("SELECT COUNT(*) FROM downloads WHERE status IN ('QUEUED', 'DOWNLOADING')")
    fun getActiveDownloadCount(): Flow<Int>
}
