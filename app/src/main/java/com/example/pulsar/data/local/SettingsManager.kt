package com.example.pulsar.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages local application settings using DataStore preferences.
 */
@Singleton
class SettingsManager @Inject constructor(@ApplicationContext context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pulsar_settings", Context.MODE_PRIVATE)

    private val _themeFlow = MutableStateFlow(getTheme())
    val themeFlow: StateFlow<String> = _themeFlow.asStateFlow()

    private val _videoQualityFlow = MutableStateFlow(getVideoQuality())
    val videoQualityFlow: StateFlow<String> = _videoQualityFlow.asStateFlow()

    private val _audioQualityFlow = MutableStateFlow(getAudioQuality())
    val audioQualityFlow: StateFlow<String> = _audioQualityFlow.asStateFlow()

    // Video Quality (Default: 1080p)
    fun getVideoQuality(): String = prefs.getString("VIDEO_QUALITY", "1080p") ?: "1080p"
    fun setVideoQuality(value: String) {
        prefs.edit { putString("VIDEO_QUALITY", value) }
        _videoQualityFlow.value = value
    }

    // Audio Quality (Default: High (320k))
    fun getAudioQuality(): String = prefs.getString("AUDIO_QUALITY", "High (320k)") ?: "High (320k)"
    fun setAudioQuality(value: String) {
        prefs.edit { putString("AUDIO_QUALITY", value) }
        _audioQualityFlow.value = value
    }

    // Theme (Default: Auto)
    fun getTheme(): String = prefs.getString("APP_THEME", "Auto") ?: "Auto"
    fun setTheme(value: String) {
        prefs.edit { putString("APP_THEME", value) }
        _themeFlow.value = value
    }

    // Max Concurrent Downloads (Default: 3)
    fun getMaxConcurrent(): Int = prefs.getInt("MAX_CONCURRENT", 3)
    fun setMaxConcurrent(value: Int) = prefs.edit().putInt("MAX_CONCURRENT", value).apply()

    // Aria2c Toggle (Default: True)
    fun getUseAria2c(): Boolean = prefs.getBoolean("USE_ARIA2C", true)
    fun setUseAria2c(value: Boolean) = prefs.edit().putBoolean("USE_ARIA2C", value).apply()

    // Custom Flags (Default: empty)
    fun getCustomFlags(): String = prefs.getString("CUSTOM_FLAGS", "") ?: ""
    fun setCustomFlags(flags: String) = prefs.edit { putString("CUSTOM_FLAGS", flags) }

    // Deleted Download IDs
    fun getDeletedDownloadIds(): Set<String> = prefs.getStringSet("DELETED_DOWNLOAD_IDS", emptySet()) ?: emptySet()
    fun addDeletedDownloadId(id: String) {
        val current = getDeletedDownloadIds().toMutableSet()
        current.add(id)
        prefs.edit { putStringSet("DELETED_DOWNLOAD_IDS", current) }
    }

    // Clear Cache
    fun clearCache(context: Context): Long {
        var deletedSize = 0L
        val cacheDir = context.cacheDir
        if (cacheDir != null && cacheDir.isDirectory) {
            deletedSize = getDirSize(cacheDir)
            cacheDir.deleteRecursively()
        }
        return deletedSize
    }

    private fun getDirSize(dir: java.io.File): Long {
        var size = 0L
        for (file in dir.listFiles() ?: emptyArray()) {
            size += if (file.isDirectory) getDirSize(file) else file.length()
        }
        return size
    }
}