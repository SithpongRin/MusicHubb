package com.example.di

import android.content.Context
import com.example.data.db.AppDatabase
import com.example.data.download.DownloadRepository
import com.example.data.download.MediaExtractor
import com.example.data.repository.MediaRepository
import com.example.data.repository.OnlineSearchRepository
import com.example.data.repository.SettingsRepository
import com.example.data.update.UpdateRepository
import com.example.player.PlaybackManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class AppContainer(private val context: Context) {

    val database: AppDatabase by lazy {
        AppDatabase.getInstance(context)
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(context)
    }

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(database)
    }

    val mediaExtractor: MediaExtractor by lazy {
        MediaExtractor(okHttpClient)
    }

    val downloadRepository: DownloadRepository by lazy {
        DownloadRepository(context, database, okHttpClient)
    }

    val onlineSearchRepository: OnlineSearchRepository by lazy {
        OnlineSearchRepository(okHttpClient)
    }

    val updateRepository: UpdateRepository by lazy {
        UpdateRepository(context, okHttpClient)
    }

    val playbackManager: PlaybackManager by lazy {
        PlaybackManager(context)
    }
}
