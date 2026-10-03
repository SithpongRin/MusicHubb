package com.example.ui.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.download.DownloadRepository
import com.example.data.download.MediaExtractor
import com.example.data.download.UrlValidator
import com.example.data.repository.MediaRepository
import com.example.domain.models.DownloadItem
import com.example.domain.models.MediaMetadataResult
import com.example.domain.models.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DownloadUiState(
    val urlInput: String = "",
    val detectedClipboardUrl: String? = null,
    val isFetchingMetadata: Boolean = false,
    val metadata: MediaMetadataResult? = null,
    val metadataError: String? = null,
    val selectedFormat: String = "mp3",
    val selectedQuality: String = "320kbps",
    val targetPlaylistId: Long? = null,
    val estimatedSizeFormatted: String = "",
    val selectedPlaylistItems: Set<String> = emptySet(),
    val playlists: List<Playlist> = emptyList(),
    val downloads: List<DownloadItem> = emptyList(),
    val activeCount: Int = 0,
    val message: String? = null
)

class DownloadViewModel(
    private val mediaExtractor: MediaExtractor,
    private val downloadRepository: DownloadRepository,
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _urlInput = MutableStateFlow("")
    private val _detectedClipboardUrl = MutableStateFlow<String?>(null)
    private val _isFetchingMetadata = MutableStateFlow(false)
    private val _metadata = MutableStateFlow<MediaMetadataResult?>(null)
    private val _metadataError = MutableStateFlow<String?>(null)
    private val _selectedFormat = MutableStateFlow("mp3")
    private val _selectedQuality = MutableStateFlow("320kbps")
    private val _targetPlaylistId = MutableStateFlow<Long?>(null)
    private val _selectedPlaylistItems = MutableStateFlow<Set<String>>(emptySet())
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<DownloadUiState> = combine(
        _urlInput,
        _detectedClipboardUrl,
        _isFetchingMetadata,
        _metadata,
        _metadataError,
        _selectedFormat,
        _selectedQuality,
        _targetPlaylistId,
        _selectedPlaylistItems,
        mediaRepository.getPlaylists(),
        downloadRepository.downloadsFlow,
        _message
    ) { params ->
        val url = params[0] as String
        val clip = params[1] as String?
        val fetching = params[2] as Boolean
        val meta = params[3] as MediaMetadataResult?
        val err = params[4] as String?
        val format = params[5] as String
        val quality = params[6] as String
        val playlistId = params[7] as Long?
        @Suppress("UNCHECKED_CAST")
        val selectedItems = params[8] as Set<String>
        @Suppress("UNCHECKED_CAST")
        val playlists = params[9] as List<Playlist>
        @Suppress("UNCHECKED_CAST")
        val downloads = params[10] as List<DownloadItem>
        val msg = params[11] as String?

        val activeCount = downloads.count { it.status == "QUEUED" || it.status == "DOWNLOADING" }

        val sizeBytes = if (meta != null) {
            if (meta.isPlaylist) {
                val count = selectedItems.size
                MediaExtractor.calculateEstimatedSizeBytes(meta.durationMs, format, quality) * count
            } else {
                MediaExtractor.calculateEstimatedSizeBytes(meta.durationMs, format, quality)
            }
        } else 0L

        DownloadUiState(
            urlInput = url,
            detectedClipboardUrl = clip,
            isFetchingMetadata = fetching,
            metadata = meta,
            metadataError = err,
            selectedFormat = format,
            selectedQuality = quality,
            targetPlaylistId = playlistId,
            estimatedSizeFormatted = MediaExtractor.formatFileSize(sizeBytes),
            selectedPlaylistItems = selectedItems,
            playlists = playlists,
            downloads = downloads,
            activeCount = activeCount,
            message = msg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DownloadUiState()
    )

    fun onUrlInputChanged(newUrl: String) {
        _urlInput.value = newUrl
        _metadataError.value = null
    }

    fun onClipboardDetected(text: String?) {
        val extracted = UrlValidator.extractUrlFromText(text)
        if (extracted != null && extracted != _urlInput.value) {
            _detectedClipboardUrl.value = extracted
        } else {
            _detectedClipboardUrl.value = null
        }
    }

    fun useCopiedUrl() {
        val url = _detectedClipboardUrl.value ?: return
        _urlInput.value = url
        _detectedClipboardUrl.value = null
        fetchMetadata(url)
    }

    fun fetchMetadata(targetUrl: String = _urlInput.value) {
        if (!UrlValidator.isValidUrl(targetUrl)) {
            _metadataError.value = "Invalid URL"
            return
        }
        viewModelScope.launch {
            _isFetchingMetadata.value = true
            _metadataError.value = null
            val result = mediaExtractor.extractMetadata(targetUrl)
            _isFetchingMetadata.value = false
            result.onSuccess { meta ->
                _metadata.value = meta
                if (meta.isPlaylist) {
                    _selectedPlaylistItems.value = meta.playlistItems.map { it.id }.toSet()
                }
            }.onFailure { e ->
                _metadataError.value = e.message ?: "Failed to fetch metadata"
            }
        }
    }

    fun setFormat(format: String) {
        _selectedFormat.value = format
        if (format == "mp4" && !_selectedQuality.value.endsWith("p")) {
            _selectedQuality.value = "720p"
        } else if (format == "mp3" && _selectedQuality.value.endsWith("p")) {
            _selectedQuality.value = "320kbps"
        }
    }

    fun setQuality(quality: String) {
        _selectedQuality.value = quality
    }

    fun setTargetPlaylist(playlistId: Long?) {
        _targetPlaylistId.value = playlistId
    }

    fun updateTitle(newTitle: String) {
        _metadata.value = _metadata.value?.copy(title = newTitle)
    }

    fun togglePlaylistItem(itemId: String) {
        val current = _selectedPlaylistItems.value.toMutableSet()
        if (current.contains(itemId)) {
            current.remove(itemId)
        } else {
            current.add(itemId)
        }
        _selectedPlaylistItems.value = current
    }

    fun selectAllPlaylistItems() {
        _metadata.value?.playlistItems?.let { items ->
            _selectedPlaylistItems.value = items.map { it.id }.toSet()
        }
    }

    fun deselectAllPlaylistItems() {
        _selectedPlaylistItems.value = emptySet()
    }

    fun startDownload() {
        val meta = _metadata.value ?: return
        val format = _selectedFormat.value
        val quality = _selectedQuality.value
        val isVideo = format == "mp4"
        val playlistId = _targetPlaylistId.value

        viewModelScope.launch {
            if (meta.isPlaylist) {
                val selectedList = meta.playlistItems.filter { _selectedPlaylistItems.value.contains(it.id) }
                selectedList.forEach { item ->
                    downloadRepository.enqueueDownload(
                        url = item.url,
                        title = item.title,
                        artist = item.artist,
                        format = format,
                        quality = quality,
                        isVideo = isVideo,
                        thumbnailUrl = item.thumbnailUrl,
                        targetPlaylistId = playlistId
                    )
                }
                _message.value = "${selectedList.size} downloads queued"
            } else {
                val result = downloadRepository.enqueueDownload(
                    url = meta.url,
                    title = meta.title,
                    artist = meta.artist,
                    format = format,
                    quality = quality,
                    isVideo = isVideo,
                    thumbnailUrl = meta.thumbnailUrl,
                    targetPlaylistId = playlistId
                )
                result.onSuccess {
                    _message.value = "Download added to queue"
                }.onFailure { e ->
                    _message.value = e.message ?: "Failed to start download"
                }
            }
            // Reset input after queued
            _urlInput.value = ""
            _metadata.value = null
        }
    }

    fun cancelDownload(id: String) {
        downloadRepository.cancelDownload(id)
    }

    fun pauseDownload(id: String) {
        downloadRepository.pauseDownload(id)
    }

    fun resumeDownload(id: String) {
        downloadRepository.resumeDownload(id)
    }

    fun retryDownload(id: String) {
        downloadRepository.retryDownload(id)
    }

    fun clearCompleted() {
        viewModelScope.launch {
            downloadRepository.clearCompleted()
        }
    }

    fun dismissMessage() {
        _message.value = null
    }
}
