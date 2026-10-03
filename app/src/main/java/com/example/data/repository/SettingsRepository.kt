package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.models.AppLanguage
import com.example.domain.models.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "musichub_settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_FORMAT = stringPreferencesKey("default_format")
        val DEFAULT_QUALITY = stringPreferencesKey("default_quality")
        val WIFI_ONLY = booleanPreferencesKey("wifi_only")
        val CONCURRENT_DOWNLOADS = intPreferencesKey("concurrent_downloads")
        val AUDIO_FOCUS = booleanPreferencesKey("audio_focus")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val DEFAULT_SPEED = floatPreferencesKey("default_speed")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("auto_check_updates")
        val LAST_UPDATE_CHECK = longPreferencesKey("last_update_check")
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { preferences ->
        when (preferences[PreferencesKeys.LANGUAGE]) {
            "en" -> AppLanguage.ENGLISH
            else -> AppLanguage.KHMER // Khmer is default language
        }
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        when (preferences[PreferencesKeys.THEME_MODE]) {
            "LIGHT" -> ThemeMode.LIGHT
            "DARK" -> ThemeMode.DARK
            else -> ThemeMode.SYSTEM
        }
    }

    val wifiOnlyFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.WIFI_ONLY] ?: false
    }

    val defaultFormatFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_FORMAT] ?: "mp3"
    }

    val defaultQualityFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_QUALITY] ?: "320kbps"
    }

    val concurrentDownloadsFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CONCURRENT_DOWNLOADS] ?: 2
    }

    val audioFocusFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUDIO_FOCUS] ?: true
    }

    val gaplessPlaybackFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GAPLESS_PLAYBACK] ?: true
    }

    val defaultSpeedFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.DEFAULT_SPEED] ?: 1.0f
    }

    val autoCheckUpdatesFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.AUTO_CHECK_UPDATES] ?: true
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE] = when (language) {
                AppLanguage.KHMER -> "km"
                AppLanguage.ENGLISH -> "en"
            }
        }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setWifiOnly(wifiOnly: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WIFI_ONLY] = wifiOnly
        }
    }

    suspend fun setDefaultFormat(format: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_FORMAT] = format
        }
    }

    suspend fun setDefaultQuality(quality: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_QUALITY] = quality
        }
    }

    suspend fun setConcurrentDownloads(count: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CONCURRENT_DOWNLOADS] = count.coerceIn(1, 4)
        }
    }

    suspend fun setAudioFocus(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_FOCUS] = enabled
        }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GAPLESS_PLAYBACK] = enabled
        }
    }

    suspend fun setDefaultSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_SPEED] = speed
        }
    }

    suspend fun setAutoCheckUpdates(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_CHECK_UPDATES] = enabled
        }
    }

    suspend fun updateLastUpdateCheckTime() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_UPDATE_CHECK] = System.currentTimeMillis()
        }
    }
}
