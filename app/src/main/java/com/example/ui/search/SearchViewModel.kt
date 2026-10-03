package com.example.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.download.DownloadRepository
import com.example.data.repository.MediaRepository
import com.example.data.repository.OnlineSearchRepository
import com.example.domain.models.OnlineSearchResult
import com.example.domain.models.Song
import com.example.player.PlaybackManager
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SearchMode {
    ON_DEVICE,
    ONLINE
}

data class SearchUiState(
    val query: String = "",
    val mode: SearchMode = SearchMode.ON_DEVICE,
    val localResults: List<Song> = emptyList(),
    val onlineResults: List<OnlineSearchResult> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null
)

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val mediaRepository: MediaRepository,
    private val onlineSearchRepository: OnlineSearchRepository,
    private val downloadRepository: DownloadRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val _query = MutableStateFlow("")
    private val _mode = MutableStateFlow(SearchMode.ON_DEVICE)
    private val _onlineResults = MutableStateFlow<List<OnlineSearchResult>>(emptyList())
    private val _isSearching = MutableStateFlow(false)
    private val _searchError = MutableStateFlow<String?>(null)

    private data class SearchBaseState(
        val query: String,
        val mode: SearchMode,
        val onlineList: List<OnlineSearchResult>,
        val isSearching: Boolean,
        val error: String?
    )

    val uiState: StateFlow<SearchUiState> = combine(
        _query,
        _mode,
        _onlineResults,
        _isSearching,
        _searchError
    ) { query, mode, onlineList, searching, error ->
        SearchBaseState(query, mode, onlineList, searching, error)
    }.flatMapLatest { base ->
        val localFlow = if (base.query.isBlank() || base.mode == SearchMode.ONLINE) {
            flowOf(emptyList())
        } else {
            mediaRepository.searchSongs(base.query)
        }
        combine(localFlow, mediaRepository.getRecentSearches()) { localList, history ->
            SearchUiState(
                query = base.query,
                mode = base.mode,
                localResults = localList,
                onlineResults = base.onlineList,
                recentSearches = history,
                isSearching = base.isSearching,
                searchError = base.error
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    init {
        // Debounce for online search
        viewModelScope.launch {
            _query
                .debounce(300)
                .distinctUntilChanged()
                .collect { q ->
                    if (_mode.value == SearchMode.ONLINE && q.isNotBlank()) {
                        performOnlineSearch(q)
                    }
                }
        }
    }

    fun onQueryChanged(newQuery: String) {
        _query.value = newQuery
        _searchError.value = null
        if (newQuery.isBlank()) {
            _onlineResults.value = emptyList()
            _isSearching.value = false
        }
    }

    fun setMode(mode: SearchMode) {
        _mode.value = mode
        if (mode == SearchMode.ONLINE && _query.value.isNotBlank()) {
            performOnlineSearch(_query.value)
        }
    }

    fun submitSearch(query: String) {
        _query.value = query
        viewModelScope.launch {
            mediaRepository.addSearchQuery(query)
        }
        if (_mode.value == SearchMode.ONLINE) {
            performOnlineSearch(query)
        }
    }

    private fun performOnlineSearch(query: String) {
        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            val result = onlineSearchRepository.searchOnline(query)
            _isSearching.value = false
            result.onSuccess { list ->
                _onlineResults.value = list
            }.onFailure { e ->
                _searchError.value = e.message ?: "Online search failed"
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            mediaRepository.clearSearchHistory()
        }
    }

    fun downloadOnlineResult(item: OnlineSearchResult, format: String = "mp3", quality: String = "320kbps") {
        viewModelScope.launch {
            downloadRepository.enqueueDownload(
                url = item.url,
                title = item.title,
                artist = item.channel,
                format = format,
                quality = quality,
                isVideo = format == "mp4",
                thumbnailUrl = item.thumbnailUrl
            )
        }
    }

    fun playSong(song: Song) {
        playbackManager.playSong(song, uiState.value.localResults)
    }
}
