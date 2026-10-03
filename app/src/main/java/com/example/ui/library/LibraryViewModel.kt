package com.example.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.download.DownloadRepository
import com.example.data.repository.MediaRepository
import com.example.domain.models.DownloadItem
import com.example.domain.models.Playlist
import com.example.domain.models.Song
import com.example.domain.models.SortDirection
import com.example.domain.models.SortOrder
import com.example.player.PlaybackManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LibraryTab {
    ALL_SONGS,
    PLAYLISTS,
    FAVORITES,
    VIDEOS,
    DOWNLOADS
}

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.ALL_SONGS,
    val sortOrder: SortOrder = SortOrder.DATE,
    val sortDirection: SortDirection = SortDirection.DESC,
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val videos: List<Song> = emptyList(),
    val downloads: List<DownloadItem> = emptyList(),
    val selectedSongIds: Set<String> = emptySet(),
    val isMultiSelectMode: Boolean = false,
    val currentPlayingSongId: String? = null
)

class LibraryViewModel(
    private val mediaRepository: MediaRepository,
    private val downloadRepository: DownloadRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(LibraryTab.ALL_SONGS)
    private val _sortOrder = MutableStateFlow(SortOrder.DATE)
    private val _sortDirection = MutableStateFlow(SortDirection.DESC)
    private val _selectedSongIds = MutableStateFlow<Set<String>>(emptySet())

    val uiState: StateFlow<LibraryUiState> = combine(
        _selectedTab,
        _sortOrder,
        _sortDirection,
        _selectedSongIds,
        playbackManager.currentSong
    ) { tab, order, dir, selected, currentSong ->
        FiveParams(tab, order, dir, selected, currentSong?.id)
    }.flatMapLatest { params ->
        combine(
            mediaRepository.getSongs(params.order, params.dir),
            mediaRepository.getPlaylists(),
            mediaRepository.getFavoriteSongs(),
            mediaRepository.getVideoSongs(),
            downloadRepository.downloadsFlow
        ) { songs, playlists, favs, videos, downloads ->
            LibraryUiState(
                selectedTab = params.tab,
                sortOrder = params.order,
                sortDirection = params.dir,
                songs = songs,
                playlists = playlists,
                favorites = favs,
                videos = videos,
                downloads = downloads,
                selectedSongIds = params.selected,
                isMultiSelectMode = params.selected.isNotEmpty(),
                currentPlayingSongId = params.currentSongId
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LibraryUiState()
    )

    private data class FiveParams(
        val tab: LibraryTab,
        val order: SortOrder,
        val dir: SortDirection,
        val selected: Set<String>,
        val currentSongId: String?
    )

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
        _selectedSongIds.value = emptySet()
    }

    fun setSortOrder(order: SortOrder) {
        if (_sortOrder.value == order) {
            _sortDirection.value = if (_sortDirection.value == SortDirection.ASC) SortDirection.DESC else SortDirection.ASC
        } else {
            _sortOrder.value = order
            _sortDirection.value = SortDirection.ASC
        }
    }

    fun toggleSongSelection(songId: String) {
        val set = _selectedSongIds.value.toMutableSet()
        if (set.contains(songId)) {
            set.remove(songId)
        } else {
            set.add(songId)
        }
        _selectedSongIds.value = set
    }

    fun clearSelection() {
        _selectedSongIds.value = emptySet()
    }

    fun deleteSelectedSongs() {
        val ids = _selectedSongIds.value
        val allSongs = uiState.value.songs.filter { ids.contains(it.id) }
        viewModelScope.launch {
            mediaRepository.deleteSongs(allSongs)
            _selectedSongIds.value = emptySet()
        }
    }

    fun deleteSong(song: Song) {
        viewModelScope.launch {
            mediaRepository.deleteSong(song)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            mediaRepository.setFavorite(song.id, !song.isFavorite)
        }
    }

    fun playSong(song: Song, queue: List<Song>) {
        playbackManager.playSong(song, queue)
    }

    fun createPlaylist(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            mediaRepository.createPlaylist(name.trim())
        }
    }

    fun renamePlaylist(id: Long, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            mediaRepository.renamePlaylist(id, name.trim())
        }
    }

    fun deletePlaylist(id: Long) {
        viewModelScope.launch {
            mediaRepository.deletePlaylist(id)
        }
    }
}
