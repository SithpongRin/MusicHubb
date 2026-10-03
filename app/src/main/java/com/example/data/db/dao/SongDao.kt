package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun getAllSongsSortedByNameAsc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE DESC")
    fun getAllSongsSortedByNameDesc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY createdAt DESC")
    fun getAllSongsSortedByDateDesc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY createdAt ASC")
    fun getAllSongsSortedByDateAsc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY durationMs DESC")
    fun getAllSongsSortedByDurationDesc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY durationMs ASC")
    fun getAllSongsSortedByDurationAsc(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY createdAt DESC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isVideo = 1 ORDER BY createdAt DESC")
    fun getVideoSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs ORDER BY createdAt DESC LIMIT 10")
    fun getRecentlyDownloaded(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: String): SongEntity?

    @Query("SELECT * FROM songs WHERE sourceUrl = :sourceUrl AND format = :format AND quality = :quality LIMIT 1")
    suspend fun findExisting(sourceUrl: String, format: String, quality: String): SongEntity?

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' ORDER BY title ASC")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    @Update
    suspend fun update(song: SongEntity)

    @Query("UPDATE songs SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE id = :songId")
    suspend fun setFavorite(songId: String, isFavorite: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(song: SongEntity)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    @Query("SELECT COUNT(*) FROM songs")
    fun getSongCount(): Flow<Int>
}
