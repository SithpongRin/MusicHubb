package com.example.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.download.DownloadRepository
import com.example.data.download.MediaExtractor
import com.example.data.repository.SettingsRepository
import com.example.data.update.UpdateRepository
import com.example.domain.models.AppLanguage
import com.example.domain.models.AppVersionManifest
import com.example.domain.models.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SettingsUiState(
    val language: AppLanguage = AppLanguage.KHMER,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val defaultFormat: String = "mp3",
    val defaultQuality: String = "320kbps",
    val wifiOnly: Boolean = false,
    val concurrentDownloads: Int = 2,
    val audioFocus: Boolean = true,
    val gapless: Boolean = true,
    val defaultSpeed: Float = 1.0f,
    val autoCheckUpdates: Boolean = true,
    val audioStorageFormatted: String = "0 B",
    val videoStorageFormatted: String = "0 B",
    val cacheStorageFormatted: String = "0 B",
    val isCheckingUpdate: Boolean = false,
    val updateManifest: AppVersionManifest? = null,
    val updateMessage: String? = null,
    val isEngineUpdating: Boolean = false,
    val engineMessage: String? = null,
    val installedVersion: String = BuildConfig.VERSION_NAME
)

class SettingsViewModel(
    private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val downloadRepository: DownloadRepository,
    private val updateRepository: UpdateRepository
) : ViewModel() {

    private val _storageAudio = MutableStateFlow(0L)
    private val _storageVideo = MutableStateFlow(0L)
    private val _storageCache = MutableStateFlow(0L)
    private val _isCheckingUpdate = MutableStateFlow(false)
    private val _updateManifest = MutableStateFlow<AppVersionManifest?>(null)
    private val _updateMessage = MutableStateFlow<String?>(null)
    private val _engineMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.languageFlow,
        settingsRepository.themeModeFlow,
        settingsRepository.defaultFormatFlow,
        settingsRepository.defaultQualityFlow,
        settingsRepository.wifiOnlyFlow,
        settingsRepository.concurrentDownloadsFlow,
        settingsRepository.audioFocusFlow,
        settingsRepository.gaplessPlaybackFlow,
        settingsRepository.defaultSpeedFlow,
        settingsRepository.autoCheckUpdatesFlow,
        _storageAudio,
        _storageVideo,
        _storageCache,
        _isCheckingUpdate,
        _updateManifest,
        _updateMessage,
        downloadRepository.engineUpdating,
        _engineMessage
    ) { params ->
        SettingsUiState(
            language = params[0] as AppLanguage,
            themeMode = params[1] as ThemeMode,
            defaultFormat = params[2] as String,
            defaultQuality = params[3] as String,
            wifiOnly = params[4] as Boolean,
            concurrentDownloads = params[5] as Int,
            audioFocus = params[6] as Boolean,
            gapless = params[7] as Boolean,
            defaultSpeed = params[8] as Float,
            autoCheckUpdates = params[9] as Boolean,
            audioStorageFormatted = MediaExtractor.formatFileSize(params[10] as Long),
            videoStorageFormatted = MediaExtractor.formatFileSize(params[11] as Long),
            cacheStorageFormatted = MediaExtractor.formatFileSize(params[12] as Long),
            isCheckingUpdate = params[13] as Boolean,
            updateManifest = params[14] as AppVersionManifest?,
            updateMessage = params[15] as String?,
            isEngineUpdating = params[16] as Boolean,
            engineMessage = params[17] as String?
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    init {
        refreshStorageUsage()
    }

    fun refreshStorageUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            val musicDir = File(context.filesDir, "music")
            val videoDir = File(context.filesDir, "videos")
            val cacheDir = context.cacheDir

            _storageAudio.value = calculateDirectorySize(musicDir)
            _storageVideo.value = calculateDirectorySize(videoDir)
            _storageCache.value = calculateDirectorySize(cacheDir)
        }
    }

    private fun calculateDirectorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        var total = 0L
        dir.walkTopDown().forEach { file ->
            if (file.isFile) total += file.length()
        }
        return total
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setWifiOnly(wifiOnly: Boolean) {
        viewModelScope.launch {
            settingsRepository.setWifiOnly(wifiOnly)
        }
    }

    fun setDefaultFormat(format: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultFormat(format)
        }
    }

    fun setDefaultQuality(quality: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultQuality(quality)
        }
    }

    fun setConcurrentDownloads(count: Int) {
        viewModelScope.launch {
            settingsRepository.setConcurrentDownloads(count)
        }
    }

    fun setAudioFocus(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAudioFocus(enabled)
        }
    }

    fun setGapless(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setGaplessPlayback(enabled)
        }
    }

    fun setDefaultSpeed(speed: Float) {
        viewModelScope.launch {
            settingsRepository.setDefaultSpeed(speed)
        }
    }

    fun setAutoCheckUpdates(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoCheckUpdates(enabled)
        }
    }

    fun clearCache() {
        viewModelScope.launch(Dispatchers.IO) {
            context.cacheDir.deleteRecursively()
            context.cacheDir.mkdirs()
            refreshStorageUsage()
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _updateMessage.value = null
            _updateManifest.value = null

            val result = updateRepository.checkForUpdates()
            _isCheckingUpdate.value = false

            result.onSuccess { manifest ->
                if (manifest != null) {
                    _updateManifest.value = manifest
                } else {
                    _updateMessage.value = "You have the latest version"
                }
            }.onFailure { e ->
                _updateMessage.value = e.message ?: "Failed to check for updates"
            }
        }
    }

    fun dismissUpdateDialog() {
        _updateManifest.value = null
    }

    fun dismissUpdateMessage() {
        _updateMessage.value = null
    }

    fun updateEngine() {
        viewModelScope.launch {
            _engineMessage.value = null
            val result = downloadRepository.updateExtractionEngine()
            result.onSuccess { msg ->
                _engineMessage.value = msg
            }.onFailure { e ->
                _engineMessage.value = e.message ?: "Engine update failed"
            }
        }
    }

    fun dismissEngineMessage() {
        _engineMessage.value = null
    }
}
