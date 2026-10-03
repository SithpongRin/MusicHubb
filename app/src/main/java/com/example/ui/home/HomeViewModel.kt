package com.example.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.MediaRepository
import com.example.domain.models.Song
import com.example.domain.models.SortDirection
import com.example.domain.models.SortOrder
import com.example.player.PlaybackManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class HomeUiState(
    val greetingRes: Int = com.example.R.string.greeting_morning,
    val continueListening: List<Song> = emptyList(),
    val recentlyDownloaded: List<Song> = emptyList(),
    val favorites: List<Song> = emptyList(),
    val isEmpty: Boolean = true
)

class HomeViewModel(
    private val mediaRepository: MediaRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val greetingRes: Int
        get() {
            val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            return when (hour) {
                in 5..11 -> com.example.R.string.greeting_morning
                in 12..17 -> com.example.R.string.greeting_afternoon
                else -> com.example.R.string.greeting_evening
            }
        }

    val uiState: StateFlow<HomeUiState> = combine(
        mediaRepository.getSongs(SortOrder.DATE, SortDirection.DESC),
        mediaRepository.getRecentlyDownloaded(),
        mediaRepository.getFavoriteSongs()
    ) { allSongs, recent, favs ->
        HomeUiState(
            greetingRes = greetingRes,
            continueListening = allSongs.take(8),
            recentlyDownloaded = recent,
            favorites = favs.take(10),
            isEmpty = allSongs.isEmpty()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeUiState()
    )

    fun playSong(song: Song, queue: List<Song>) {
        playbackManager.playSong(song, queue)
    }

    fun shuffleAll() {
        val list = uiState.value.continueListening
        if (list.isNotEmpty()) {
            playbackManager.playQueue(list, startIndex = 0, shuffle = true)
        }
    }

    fun toggleFavorite(song: Song) {
        viewModelScope.launch {
            mediaRepository.setFavorite(song.id, !song.isFavorite)
        }
    }
}
