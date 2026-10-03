package com.example.player

import android.content.Context
import android.net.Uri
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.example.domain.models.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PlaybackManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    val exoPlayer: ExoPlayer by lazy {
        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build().apply {
                addListener(playerListener)
            }
    }

    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _queue = MutableStateFlow<List<Song>>(emptyList())
    val queue: StateFlow<List<Song>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _shuffleMode = MutableStateFlow(false)
    val shuffleMode: StateFlow<Boolean> = _shuffleMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _sleepTimerSeconds = MutableStateFlow<Long?>(null)
    val sleepTimerSeconds: StateFlow<Long?> = _sleepTimerSeconds.asStateFlow()

    private val _isVideoPresentation = MutableStateFlow(false)
    val isVideoPresentation: StateFlow<Boolean> = _isVideoPresentation.asStateFlow()

    private val waveformCache = mutableMapOf<String, List<Float>>()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            if (playing) {
                startProgressTracker()
            } else {
                stopProgressTracker()
            }
        }

        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_READY -> {
                    _durationMs.value = exoPlayer.duration.coerceAtLeast(0L)
                }
                Player.STATE_ENDED -> {
                    if (sleepTimerSeconds.value == -1L) {
                        // End of track sleep timer
                        pause()
                        _sleepTimerSeconds.value = null
                    } else if (_currentIndex.value < _queue.value.size - 1) {
                        next()
                    } else if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
                        playAtIndex(0)
                    } else {
                        _isPlaying.value = false
                        stopProgressTracker()
                    }
                }
                else -> {}
            }
        }

        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            _currentPositionMs.value = exoPlayer.currentPosition.coerceAtLeast(0L)
        }
    }

    fun playSong(song: Song, newQueue: List<Song> = emptyList()) {
        val targetQueue = if (newQueue.isNotEmpty()) newQueue else listOf(song)
        _queue.value = targetQueue
        val index = targetQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        playAtIndex(index)
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        val list = if (shuffle) songs.shuffled() else songs
        _queue.value = list
        _shuffleMode.value = shuffle
        exoPlayer.shuffleModeEnabled = shuffle
        playAtIndex(startIndex.coerceIn(0, list.size - 1))
    }

    fun playAtIndex(index: Int) {
        val list = _queue.value
        if (index !in list.indices) return
        val song = list[index]
        _currentIndex.value = index
        _currentSong.value = song
        _isVideoPresentation.value = song.isVideo

        val file = File(song.filePath)
        val mediaUri = if (file.exists()) Uri.fromFile(file) else Uri.parse(song.sourceUrl)

        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(mediaUri)
            .setMediaMetadata(metadata)
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0)
            }
            exoPlayer.play()
        }
    }

    fun pause() {
        exoPlayer.pause()
    }

    fun play() {
        exoPlayer.play()
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _currentPositionMs.value = positionMs
    }

    fun next() {
        val list = _queue.value
        if (list.isEmpty()) return
        val nextIndex = if (_currentIndex.value < list.size - 1) {
            _currentIndex.value + 1
        } else if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
            0
        } else {
            return
        }
        playAtIndex(nextIndex)
    }

    fun previous() {
        if (exoPlayer.currentPosition > 3000L) {
            seekTo(0L)
            return
        }
        val list = _queue.value
        if (list.isEmpty()) return
        val prevIndex = if (_currentIndex.value > 0) {
            _currentIndex.value - 1
        } else if (_repeatMode.value == Player.REPEAT_MODE_ALL) {
            list.size - 1
        } else {
            0
        }
        playAtIndex(prevIndex)
    }

    fun toggleShuffle() {
        val newShuffle = !_shuffleMode.value
        _shuffleMode.value = newShuffle
        exoPlayer.shuffleModeEnabled = newShuffle
    }

    fun toggleRepeat() {
        val nextMode = when (_repeatMode.value) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        _repeatMode.value = nextMode
        exoPlayer.repeatMode = nextMode
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        _playbackSpeed.value = clamped
        exoPlayer.playbackParameters = PlaybackParameters(clamped)
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        if (minutes <= 0) {
            _sleepTimerSeconds.value = null
            return
        }
        if (minutes == -1) {
            // End of track indicator
            _sleepTimerSeconds.value = -1L
            return
        }

        val totalSeconds = minutes * 60L
        _sleepTimerSeconds.value = totalSeconds

        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                _sleepTimerSeconds.value = remaining
            }
            if (remaining <= 0) {
                pause()
                _sleepTimerSeconds.value = null
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        _sleepTimerSeconds.value = null
    }

    fun setVideoPresentation(enabled: Boolean) {
        _isVideoPresentation.value = enabled
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val list = _queue.value.toMutableList()
        if (fromIndex in list.indices && toIndex in list.indices) {
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            _queue.value = list
            _currentSong.value?.let { current ->
                _currentIndex.value = list.indexOfFirst { it.id == current.id }.coerceAtLeast(0)
            }
        }
    }

    fun removeFromQueue(index: Int) {
        val list = _queue.value.toMutableList()
        if (index in list.indices) {
            val wasCurrent = index == _currentIndex.value
            list.removeAt(index)
            _queue.value = list
            if (list.isEmpty()) {
                exoPlayer.stop()
                _currentSong.value = null
                _isPlaying.value = false
            } else if (wasCurrent) {
                val nextIdx = index.coerceAtMost(list.size - 1)
                playAtIndex(nextIdx)
            } else if (index < _currentIndex.value) {
                _currentIndex.value = _currentIndex.value - 1
            }
        }
    }

    suspend fun getWaveformData(songId: String): List<Float> = withContext(Dispatchers.Default) {
        waveformCache[songId]?.let { return@withContext it }

        // Generate smooth waveform profile deterministically from songId hash
        val bars = 48
        val hash = songId.hashCode()
        val random = java.util.Random(hash.toLong())
        val data = (0 until bars).map { i ->
            val sinWave = kotlin.math.sin(i.toDouble() / 4.0).toFloat() * 0.25f
            val base = 0.35f + kotlin.math.abs(sinWave)
            val noise = (random.nextFloat() * 0.4f)
            (base + noise).coerceIn(0.15f, 1.0f)
        }
        waveformCache[songId] = data
        data
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _currentPositionMs.value = exoPlayer.currentPosition.coerceAtLeast(0L)
                delay(250)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
        _currentPositionMs.value = exoPlayer.currentPosition.coerceAtLeast(0L)
    }

    fun release() {
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }
}
