package com.example.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MediaRepository
import com.example.domain.models.Playlist
import com.example.domain.models.Song
import com.example.player.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val filteredSongs: List<Song> = emptyList(),
    val searchQuery: String = "",
    val lastRemovedSongId: String? = null
)

class PlaylistDetailViewModel(
    private val playlistId: Long,
    private val mediaRepository: MediaRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _lastRemovedSongId = MutableStateFlow<String?>(null)

    val uiState: StateFlow<PlaylistDetailUiState> = combine(
        mediaRepository.observePlaylist(playlistId),
        _searchQuery,
        _lastRemovedSongId
    ) { playlist, query, lastRemoved ->
        Triple(playlist, query, lastRemoved)
    }.flatMapLatest { (playlist, query, lastRemoved) ->
        val songsFlow = if (query.isBlank()) {
            mediaRepository.getSongsForPlaylist(playlistId)
        } else {
            mediaRepository.searchSongsInPlaylist(playlistId, query)
        }
        combine(mediaRepository.getSongsForPlaylist(playlistId), songsFlow) { allSongs, filtered ->
            PlaylistDetailUiState(
                playlist = playlist,
                songs = allSongs,
                filteredSongs = filtered,
                searchQuery = query,
                lastRemovedSongId = lastRemoved
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PlaylistDetailUiState()
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun playSong(song: Song) {
        playbackManager.playSong(song, uiState.value.songs)
    }

    fun playAll(shuffle: Boolean = false) {
        val list = uiState.value.songs
        if (list.isNotEmpty()) {
            playbackManager.playQueue(list, startIndex = 0, shuffle = shuffle)
        }
    }

    fun removeSong(songId: String) {
        _lastRemovedSongId.value = songId
        viewModelScope.launch {
            mediaRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    fun undoRemoveSong() {
        val songId = _lastRemovedSongId.value ?: return
        viewModelScope.launch {
            mediaRepository.addSongToPlaylist(playlistId, songId)
            _lastRemovedSongId.value = null
        }
    }

    fun moveSong(fromIndex: Int, toIndex: Int) {
        val currentSongs = uiState.value.songs.toMutableList()
        if (fromIndex in currentSongs.indices && toIndex in currentSongs.indices) {
            val moved = currentSongs.removeAt(fromIndex)
            currentSongs.add(toIndex, moved)
            viewModelScope.launch {
                mediaRepository.reorderPlaylist(playlistId, currentSongs.map { it.id })
            }
        }
    }

    fun renamePlaylist(newName: String) {
        viewModelScope.launch {
            mediaRepository.renamePlaylist(playlistId, newName)
        }
    }

    fun deletePlaylist(onComplete: () -> Unit) {
        viewModelScope.launch {
            mediaRepository.deletePlaylist(playlistId)
            onComplete()
        }
    }
}
