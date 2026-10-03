package com.example.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.db.entity.PlaylistEntity
import com.example.data.db.entity.PlaylistSongCrossRef
import com.example.data.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

data class PlaylistWithSongCount(
    val id: Long,
    val name: String,
    val coverPath: String?,
    val createdAt: Long,
    val songCount: Int,
    val firstSongThumbnail: String?
)

@Dao
interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Query("""
        SELECT p.id, p.name, p.coverPath, p.createdAt,
               COUNT(ps.songId) AS songCount,
               (SELECT s.thumbnailPath FROM songs s 
                INNER JOIN playlist_songs ps2 ON s.id = ps2.songId 
                WHERE ps2.playlistId = p.id ORDER BY ps2.position ASC LIMIT 1) AS firstSongThumbnail
        FROM playlists p
        LEFT JOIN playlist_songs ps ON p.id = ps.playlistId
        GROUP BY p.id
        ORDER BY p.createdAt DESC
    """)
    fun getPlaylistsWithDetails(): Flow<List<PlaylistWithSongCount>>

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: Long): PlaylistEntity?

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun observePlaylistById(playlistId: Long): Flow<PlaylistEntity?>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId
        ORDER BY ps.position ASC
    """)
    fun getSongsForPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_songs ps ON s.id = ps.songId
        WHERE ps.playlistId = :playlistId AND (s.title LIKE '%' || :query || '%' OR s.artist LIKE '%' || :query || '%')
        ORDER BY ps.position ASC
    """)
    fun searchSongsInPlaylist(playlistId: Long, query: String): Flow<List<SongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongCrossRef(crossRef: PlaylistSongCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongCrossRefs(crossRefs: List<PlaylistSongCrossRef>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun getNextPositionForPlaylist(playlistId: Long): Int

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylistSongs(playlistId: Long)

    @Transaction
    suspend fun updateSongPositions(playlistId: Long, songIdsInOrder: List<String>) {
        clearPlaylistSongs(playlistId)
        val crossRefs = songIdsInOrder.mapIndexed { index, songId ->
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId, position = index)
        }
        insertSongCrossRefs(crossRefs)
    }

    @Transaction
    suspend fun addSongToPlaylist(playlistId: Long, songId: String) {
        val nextPos = getNextPositionForPlaylist(playlistId)
        insertSongCrossRef(PlaylistSongCrossRef(playlistId = playlistId, songId = songId, position = nextPos))
    }
}
