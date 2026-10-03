package com.example.data.repository

import com.example.data.db.AppDatabase
import com.example.data.db.entity.PlaylistEntity
import com.example.data.db.entity.SearchHistoryEntity
import com.example.data.db.entity.SongEntity
import com.example.domain.models.Playlist
import com.example.domain.models.Song
import com.example.domain.models.SortDirection
import com.example.domain.models.SortOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class MediaRepository(private val database: AppDatabase) {

    private val songDao = database.songDao()
    private val playlistDao = database.playlistDao()
    private val searchHistoryDao = database.searchHistoryDao()

    fun getSongs(sortOrder: SortOrder, sortDirection: SortDirection): Flow<List<Song>> {
        val flow = when (sortOrder) {
            SortOrder.NAME -> if (sortDirection == SortDirection.ASC) {
                songDao.getAllSongsSortedByNameAsc()
            } else {
                songDao.getAllSongsSortedByNameDesc()
            }
            SortOrder.DATE -> if (sortDirection == SortDirection.ASC) {
                songDao.getAllSongsSortedByDateAsc()
            } else {
                songDao.getAllSongsSortedByDateDesc()
            }
            SortOrder.DURATION -> if (sortDirection == SortDirection.ASC) {
                songDao.getAllSongsSortedByDurationAsc()
            } else {
                songDao.getAllSongsSortedByDurationDesc()
            }
        }
        return flow.map { list -> list.map { it.toDomain() } }
    }

    fun getFavoriteSongs(): Flow<List<Song>> {
        return songDao.getFavoriteSongs().map { list -> list.map { it.toDomain() } }
    }

    fun getVideoSongs(): Flow<List<Song>> {
        return songDao.getVideoSongs().map { list -> list.map { it.toDomain() } }
    }

    fun getRecentlyDownloaded(): Flow<List<Song>> {
        return songDao.getRecentlyDownloaded().map { list -> list.map { it.toDomain() } }
    }

    suspend fun getSongById(id: String): Song? {
        return songDao.getSongById(id)?.toDomain()
    }

    fun searchSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongs(query).map { list -> list.map { it.toDomain() } }
    }

    suspend fun setFavorite(songId: String, isFavorite: Boolean) {
        songDao.setFavorite(songId, isFavorite)
    }

    suspend fun deleteSong(song: Song) = withContext(Dispatchers.IO) {
        try {
            val file = File(song.filePath)
            if (file.exists()) {
                file.delete()
            }
            song.thumbnailPath?.let {
                val thumbFile = File(it)
                if (thumbFile.exists()) {
                    thumbFile.delete()
                }
            }
        } catch (_: Exception) {}
        songDao.deleteById(song.id)
    }

    suspend fun deleteSongs(songs: List<Song>) = withContext(Dispatchers.IO) {
        songs.forEach { song ->
            try {
                val file = File(song.filePath)
                if (file.exists()) file.delete()
                song.thumbnailPath?.let {
                    val thumbFile = File(it)
                    if (thumbFile.exists()) thumbFile.delete()
                }
            } catch (_: Exception) {}
        }
        songDao.deleteByIds(songs.map { it.id })
    }

    // Playlists
    fun getPlaylists(): Flow<List<Playlist>> {
        return playlistDao.getPlaylistsWithDetails().map { list ->
            list.map {
                Playlist(
                    id = it.id,
                    name = it.name,
                    coverPath = it.coverPath,
                    createdAt = it.createdAt,
                    songCount = it.songCount,
                    previewThumbnail = it.firstSongThumbnail
                )
            }
        }
    }

    fun observePlaylist(playlistId: Long): Flow<Playlist?> {
        return playlistDao.observePlaylistById(playlistId).map { it?.let { p ->
            Playlist(id = p.id, name = p.name, coverPath = p.coverPath, createdAt = p.createdAt)
        }}
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return playlistDao.getSongsForPlaylist(playlistId).map { list -> list.map { it.toDomain() } }
    }

    fun searchSongsInPlaylist(playlistId: Long, query: String): Flow<List<Song>> {
        return playlistDao.searchSongsInPlaylist(playlistId, query).map { list -> list.map { it.toDomain() } }
    }

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name.trim()))
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        val existing = playlistDao.getPlaylistById(playlistId) ?: return
        playlistDao.updatePlaylist(existing.copy(name = newName.trim()))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylistById(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: String) {
        playlistDao.addSongToPlaylist(playlistId, songId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun reorderPlaylist(playlistId: Long, songIdsInOrder: List<String>) {
        playlistDao.updateSongPositions(playlistId, songIdsInOrder)
    }

    // Search History
    fun getRecentSearches(): Flow<List<String>> {
        return searchHistoryDao.getRecentSearches().map { list -> list.map { it.query } }
    }

    suspend fun addSearchQuery(query: String) {
        if (query.isBlank()) return
        searchHistoryDao.insertSearch(SearchHistoryEntity(query = query.trim()))
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearHistory()
    }

    private fun SongEntity.toDomain() = Song(
        id = id,
        title = title,
        artist = artist,
        thumbnailPath = thumbnailPath,
        filePath = filePath,
        format = format,
        quality = quality,
        durationMs = durationMs,
        sourceUrl = sourceUrl,
        isFavorite = isFavorite,
        createdAt = createdAt,
        fileSizeBytes = fileSizeBytes,
        mimeType = mimeType,
        updatedAt = updatedAt,
        isVideo = isVideo
    )
}
